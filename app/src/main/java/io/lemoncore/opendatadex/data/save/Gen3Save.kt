package io.lemoncore.opendatadex.data.save

import io.lemoncore.opendatadex.data.Game

/**
 * Ruby, Sapphire, Emerald, FireRed and LeafGreen saves.
 *
 * The layout follows the community's documentation of the format and the pret decompilations: two 57,344-byte save
 * slots of fourteen 4 KB sections; the newer slot is the one the game loads.
 */
object Gen3Save {
    private const val SECTION = 0x1000
    private const val SECTION_DATA = 0xF80
    private const val SLOT = SECTION * 14
    private const val SIGNATURE = 0x08012025
    private const val BOX_COUNT = 14
    private const val BOX_SIZE = 30
    private const val BOX_MON = 80
    private const val PARTY_MON = 100
    private const val LANGUAGE_JAPANESE = 1
    private const val FRLG_NATIONAL_FLAG = 0x840

    /** Reads [bytes] as a Generation III save; null when it isn't one. [fileName] helps tell FireRed from LeafGreen. */
    fun read(bytes: ByteArray, fileName: String? = null): GameSave? {
        if (bytes.size < SLOT * 2 || bytes.size > SLOT * 3) return null
        val slot = listOf(0, SLOT).mapNotNull { base -> sectionsAt(bytes, base) }.maxByOrNull { it.first }?.second ?: return null
        val trainer = slot[0]
        val code = trainer.u32(0xAC)
        val frlg = code == 1L
        val family = when {
            frlg -> Family.FRLG
            code == 0L -> Family.RS
            else -> Family.EMERALD
        }
        val fullId = trainer.u32(0x0A)
        val trainerId = (fullId and 0xFFFF).toInt()
        val secretId = (fullId ushr 16).toInt()

        val teamBlock = slot[1]
        val partyAt = if (frlg) 0x34 else 0x234
        val partyCount = teamBlock.u32(partyAt).toInt().coerceIn(0, 6)
        val party = (0 until partyCount).mapNotNull { i ->
            readPokemon(teamBlock, partyAt + 4 + i * PARTY_MON, box = 0, slot = i, fullId)
        }

        // The PC spans sections 5 to 13, stored back to back.
        val pc = ByteArray(SECTION_DATA * 9)
        for (i in 5..13) slot[i].copyInto(pc, (i - 5) * SECTION_DATA, 0, SECTION_DATA)
        val boxes = (0 until BOX_COUNT * BOX_SIZE).mapNotNull { i ->
            readPokemon(pc, 4 + i * BOX_MON, box = i / BOX_SIZE + 1, slot = i % BOX_SIZE, fullId)
        }
        val own = (party + boxes).filter { it.ownCatch && !it.egg }
        val japanese = own.count { it.language == LANGUAGE_JAPANESE } * 2 > own.size
        val namesAt = 4 + BOX_COUNT * BOX_SIZE * BOX_MON
        val boxNames = (0 until BOX_COUNT).map { decodeText(pc, namesAt + it * 9, 9, japanese) }

        /** An event flag; they live in the player's data, which runs on across sections 1 to 4. */
        fun flag(index: Int): Boolean {
            val at = family.eventFlags + index / 8
            return (slot[1 + at / SECTION_DATA].u8(at % SECTION_DATA) shr (index % 8)) and 1 == 1
        }
        // FireRed and LeafGreen's byte can outlast the game that set it, as after a new game on a used cartridge.
        val nationalDex = if (frlg) flag(FRLG_NATIONAL_FLAG) else trainer[0x1A].toInt() and 0xFF == 0xDA
        val known = family.pick(own, fileName)
        return GameSave(
            game = known ?: family.first,
            gameOptions = if (known == null) listOf(family.first, family.second) else emptyList(),
            trainerName = decodeText(trainer, 0, 7, japanese),
            trainerId = trainerId,
            secretId = secretId,
            playMinutes = trainer.u16(0x0E) * 60 + (trainer[0x10].toInt() and 0xFF),
            owned = dexFlags(trainer, 0x28, Gen3Species.NATIONAL_COUNT),
            seen = dexFlags(trainer, 0x5C, Gen3Species.NATIONAL_COUNT),
            nationalDex = nationalDex,
            party = party,
            boxes = boxes,
            boxNames = boxNames,
            badges = (0 until 8).sumOf { i -> if (flag(family.firstBadgeFlag + i)) 1 shl i else 0 },
        )
    }

    /** A slot's fourteen sections in id order, with its save counter; null when the slot is empty or damaged. */
    private fun sectionsAt(bytes: ByteArray, base: Int): Pair<Long, List<ByteArray>>? {
        val sections = arrayOfNulls<ByteArray>(14)
        var counter = -1L
        for (i in 0 until 14) {
            val at = base + i * SECTION
            if (bytes.u32(at + 0xFF8) != SIGNATURE.toLong()) return null
            val id = bytes.u16(at + 0xFF4)
            if (id !in 0 until 14 || sections[id] != null) return null
            sections[id] = bytes.copyOfRange(at, at + SECTION_DATA)
            counter = bytes.u32(at + 0xFFC)
        }
        return counter to sections.map { it!! }
    }

    /** One Pokémon's 80 bytes (the first 80 of a party Pokémon's 100): decrypted, unshuffled and checked. */
    private fun readPokemon(b: ByteArray, at: Int, box: Int, slot: Int, trainerFullId: Long): SavedPokemon? {
        val pid = b.u32(at)
        val otId = b.u32(at + 4)
        if (pid == 0L && otId == 0L) return null
        val key = pid xor otId
        val data = ByteArray(48)
        for (w in 0 until 12) {
            val word = b.u32(at + 32 + w * 4) xor key
            for (k in 0 until 4) data[w * 4 + k] = (word ushr (8 * k)).toByte()
        }
        val sum = (0 until 24).sumOf { data.u16(it * 2) } and 0xFFFF
        if (sum != b.u16(at + 0x1C)) return null
        val order = BLOCK_ORDERS[(pid % 24).toInt()]
        fun part(c: Char) = order.indexOf(c) * 12
        val g = part('A'); val a = part('B'); val e = part('C'); val m = part('D')

        val speciesId = Gen3Species.national(data.u16(g)) ?: return null
        val origins = data.u16(m + 2)
        val ivWord = data.u32(m + 4)
        // Stored as HP, Attack, Defense, Speed, Sp. Atk, Sp. Def; kept in the usual HP to Speed order.
        val iv = IntArray(6) { ((ivWord ushr (5 * it)) and 31).toInt() }
        val ev = IntArray(6) { data[e + it].toInt() and 0xFF }
        val tid = otId and 0xFFFF
        val sid = otId ushr 16
        val flags = b[at + 0x13].toInt()
        val language = b[at + 0x12].toInt() and 0xFF
        val japanese = language == LANGUAGE_JAPANESE
        return SavedPokemon(
            box = box,
            slot = slot,
            speciesId = speciesId,
            nickname = decodeText(b, at + 8, 10, japanese),
            otName = decodeText(b, at + 0x14, 7, japanese),
            otId = otId,
            ownCatch = otId == trainerFullId,
            pid = pid,
            exp = data.u32(g + 4).toInt(),
            heldItem = data.u16(g + 2),
            friendship = data[g + 9].toInt() and 0xFF,
            moves = (0 until 4).map { data.u16(a + it * 2) }.filter { it != 0 },
            ivs = listOf(iv[0], iv[1], iv[2], iv[4], iv[5], iv[3]),
            evs = listOf(ev[0], ev[1], ev[2], ev[4], ev[5], ev[3]),
            metLevel = origins and 0x7F,
            metLocation = data[m + 1].toInt() and 0xFF,
            originGame = (origins shr 7) and 0xF,
            ball = (origins shr 11) and 0xF,
            egg = (ivWord ushr 30) and 1 == 1L || flags and 0x04 != 0,
            abilitySlot = ((ivWord ushr 31) and 1).toInt(),
            shiny = (tid xor sid xor (pid and 0xFFFF) xor (pid ushr 16)) < 8,
            language = language,
            form = if (speciesId == UNOWN) gen3UnownForm(pid) else 0,
        )
    }

    private enum class Family(
        val first: Game, val second: Game, val firstOrigin: Int, val secondOrigin: Int, val firstMarks: List<String>, val secondMarks: List<String>,
        val eventFlags: Int, val firstBadgeFlag: Int,
    ) {
        RS(Game.RUBY, Game.SAPPHIRE, 2, 1, listOf("RUBY", "AXV"), listOf("SAPP", "AXP"), 0x1220, 0x807),
        EMERALD(Game.EMERALD, Game.EMERALD, 3, 3, listOf("EMER", "BPE"), emptyList(), 0x1270, 0x867),
        FRLG(Game.FIRERED, Game.LEAFGREEN, 4, 5, listOf("FIRE", "BPR"), listOf("LEAF", "BPG"), 0xEE0, 0x820);

        /** Which game of the pair: the one most of the trainer's own catches came from, or failing that the file name. */
        fun pick(own: List<SavedPokemon>, fileName: String?): Game? {
            if (first == second) return first
            val a = own.count { it.originGame == firstOrigin }
            val b = own.count { it.originGame == secondOrigin }
            return when {
                a > b -> first
                b > a -> second
                else -> gameInFileName(fileName, listOf(first to firstMarks, second to secondMarks))
            }
        }
    }
}

/** The 24 ways the four parts of a Pokémon's data are shuffled in Generations III and IV. */
internal val BLOCK_ORDERS = listOf(
    "ABCD", "ABDC", "ACBD", "ACDB", "ADBC", "ADCB", "BACD", "BADC", "BCAD", "BCDA", "BDAC", "BDCA",
    "CABD", "CADB", "CBAD", "CBDA", "CDAB", "CDBA", "DABC", "DACB", "DBAC", "DBCA", "DCAB", "DCBA",
)

/** Generation III text up to the 0xFF terminator, in the Western character set or the Japanese one. */
internal fun decodeText(b: ByteArray, at: Int, length: Int, japanese: Boolean = false): String = buildString {
    val table = if (japanese) GEN3_JAPANESE_CHARS else GEN3_CHARS
    for (i in at until at + length) {
        val c = b[i].toInt() and 0xFF
        if (c == 0xFF) break
        append(table[c] ?: continue)
    }
}.trim()

private val GEN3_SHARED_CHARS: Map<Int, Char> = buildMap {
    put(0x00, ' ')
    "0123456789".forEachIndexed { i, ch -> put(0xA1 + i, ch) }
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ".forEachIndexed { i, ch -> put(0xBB + i, ch) }
    "abcdefghijklmnopqrstuvwxyz".forEachIndexed { i, ch -> put(0xD5 + i, ch) }
    put(0xB5, '♂'); put(0xB6, '♀'); put(0xBA, '/')
}

private val GEN3_CHARS: Map<Int, Char> = GEN3_SHARED_CHARS + mapOf(
    0xAB to '!', 0xAC to '?', 0xAD to '.', 0xAE to '-', 0xB0 to '…', 0xB1 to '“', 0xB2 to '”', 0xB3 to '‘', 0xB4 to '’',
    0xB8 to ',', 0x1B to 'é', 0x2D to '&', 0x5C to '(', 0x5D to ')',
)

/** The Japanese games' kana: hiragana from 0x01, katakana in the same order from 0x51. */
private val GEN3_JAPANESE_CHARS: Map<Int, Char> = GEN3_SHARED_CHARS + buildMap {
    val hiragana = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをんぁぃぅぇぉゃゅょ" +
        "がぎぐげござじずぜぞだぢづでどばびぶべぼぱぴぷぺぽっ"
    hiragana.forEachIndexed { i, ch ->
        put(0x01 + i, ch)
        put(0x51 + i, ch + 0x60)
    }
    put(0xAB, '！'); put(0xAC, '？'); put(0xAD, '。'); put(0xAE, 'ー'); put(0xAF, '・')
}

/** Generation III species indexes: 1 to 251 match the National Pokédex; the Hoenn species follow in their own order. */
object Gen3Species {
    const val NATIONAL_COUNT = 386
    private const val FIRST_HOENN_INDEX = 277

    /** National numbers for internal indexes 277 (Treecko) to 411 (Chimecho), from the pret decompilations. */
    private val HOENN = intArrayOf(
        252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275,
        290, 291, 292, 276, 277, 285, 286, 327, 278, 279, 283, 284, 320, 321, 300, 301, 352, 343, 344, 299, 324, 302, 339, 340,
        370, 341, 342, 349, 350, 318, 319, 328, 329, 330, 296, 297, 309, 310, 322, 323, 363, 364, 365, 331, 332, 361, 362, 337,
        338, 298, 325, 326, 311, 312, 303, 307, 308, 333, 334, 360, 355, 356, 315, 287, 288, 289, 316, 317, 357, 293, 294, 295,
        366, 367, 368, 359, 353, 354, 336, 335, 369, 304, 305, 306, 351, 313, 314, 345, 346, 347, 348, 280, 281, 282, 371, 372,
        373, 374, 375, 376, 377, 378, 379, 382, 383, 384, 380, 381, 385, 386, 358,
    )

    /** The National Pokédex number for an internal index, or null for an empty slot or a placeholder. */
    fun national(index: Int): Int? = when (index) {
        in 1..251 -> index
        in FIRST_HOENN_INDEX until FIRST_HOENN_INDEX + HOENN.size -> HOENN[index - FIRST_HOENN_INDEX]
        else -> null
    }
}
