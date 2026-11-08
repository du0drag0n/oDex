package io.lemoncore.opendatadex.data.save

import io.lemoncore.opendatadex.data.Game

/**
 * Red, Blue, Yellow, Gold, Silver and Crystal saves, in the layout of the games sold outside Japan and Korea, plus
 * Japanese Crystal. The layouts follow the pret decompilations and Bulbapedia's notes on them; Japanese Crystal's comes
 * from reading a real save, since its shorter names and 30-Pokémon boxes shift everything after them.
 *
 * These games keep the party and each PC box as lists: a count, the species, then the Pokémon, their trainers' names
 * and their nicknames. The box in use lives in the main save data, and its copy among the other boxes goes stale.
 */
object Gen12Save {
    private const val SIZE = 0x8000
    /** Japanese Crystal's extra memory holds what the Mobile System GB needs. */
    private const val JAPANESE_CRYSTAL_SIZE = 0x10000
    /** Some emulators add their clock data after the save. */
    private const val MAX_FOOTER = 0x100
    private const val NAME = 11
    private const val JAPANESE_NAME = 6
    private const val BOX_SIZE = 20
    private const val JAPANESE_BOX_SIZE = 30
    private const val LIST_END = 0xFF
    /** What [SavedPokemon.language] calls Japanese. */
    private const val JAPANESE = 1

    /** True for a file the size of a Game Boy cartridge's save memory, which may still hold no save. */
    fun isCartridgeSized(bytes: ByteArray) = bytes.size in SIZE..SIZE + MAX_FOOTER || bytes.size in JAPANESE_CRYSTAL_SIZE..JAPANESE_CRYSTAL_SIZE + MAX_FOOTER

    fun read(bytes: ByteArray, fileName: String? = null): GameSave? {
        if (bytes.size in JAPANESE_CRYSTAL_SIZE..JAPANESE_CRYSTAL_SIZE + MAX_FOOTER) return gen2(bytes, listOf(JAPANESE_CRYSTAL), fileName)
        if (!isCartridgeSized(bytes)) return null
        return gen2(bytes, listOf(CRYSTAL, GOLD_SILVER), fileName) ?: gen1(bytes, fileName)
    }

    // ---- Generation I ------------------------------------------------------------------------------------------------------

    private const val G1_MON = 33
    private const val G1_PARTY_MON = 44
    private const val G1_BOX = 0x462
    private const val G1_BOXES = 12
    private const val G1_PIKACHU_INDEX = 0x54

    private fun gen1(b: ByteArray, fileName: String?): GameSave? {
        val sum = (0x2598 until 0x3523).sumOf { b.u8(it) }
        if (sum.inv() and 0xFF != b.u8(0x3523)) return null
        val name = text(b, 0x2598, NAME)
        val id = b.be16(0x2605)
        if (name.isBlank()) return null
        val mon = { at: Int, listed: Int, box: Int, slot: Int, ot: String, nick: String ->
            gen1Pokemon(b, at, listed, box, slot, ot, nick, id, name)
        }
        val party = list(b, 0x2F2C, 6, G1_PARTY_MON, 0, mon)
        val current = b.u8(0x284C) and 0x7F
        // The top bit says the player has changed box, which sets up the storage; until then it holds whatever was in memory.
        val boxesReady = b.u8(0x284C) and 0x80 != 0
        val boxes = (0 until G1_BOXES).flatMap { box ->
            when {
                box == current -> list(b, 0x30C0, BOX_SIZE, G1_MON, box + 1, mon)
                !boxesReady -> emptyList()
                else -> list(b, (if (box < 6) 0x4000 + box * G1_BOX else 0x6000 + (box - 6) * G1_BOX), BOX_SIZE, G1_MON, box + 1, mon)
            }
        }
        val named = gameInFileName(fileName, listOf(Game.YELLOW to listOf("YELLOW", "APS"), Game.BLUE to listOf("BLUE", "APE"), Game.RED to listOf("RED", "APA")))
        val game = named ?: if (b.u8(0x29C3) == G1_PIKACHU_INDEX) Game.YELLOW else Game.RED
        return GameSave(
            game = game,
            gameOptions = if (named == null) listOf(game) + (listOf(Game.RED, Game.BLUE, Game.YELLOW) - game) else emptyList(),
            trainerName = name,
            trainerId = id,
            secretId = 0,
            playMinutes = b.u8(0x2CED) * 60 + b.u8(0x2CEF),
            owned = dexFlags(b, 0x25A3, 151),
            seen = dexFlags(b, 0x25B6, 151),
            nationalDex = false,
            party = party,
            boxes = boxes,
            boxNames = List(G1_BOXES) { "" },
            badges = b.u8(0x2602),
        )
    }

    private fun gen1Pokemon(b: ByteArray, at: Int, listed: Int, box: Int, slot: Int, ot: String, nick: String, trainerId: Int, trainerName: String): SavedPokemon? {
        val species = Gen1Species.national(listed) ?: return null
        if (b.u8(at) != listed) return null
        val dvs = dvs(b.be16(at + 27))
        val otId = b.be16(at + 12)
        val statExp = List(5) { b.be16(at + 17 + it * 2) }
        return SavedPokemon(
            box = box, slot = slot, speciesId = species, nickname = nick, otName = ot, otId = otId.toLong(),
            ownCatch = otId == trainerId && ot == trainerName, pid = 0, exp = b.be24(at + 14), heldItem = 0, friendship = 0,
            moves = (8..11).map { b.u8(at + it) }.filter { it != 0 },
            ivs = listOf(dvs.hp, dvs.attack, dvs.defense, dvs.special, dvs.special, dvs.speed),
            evs = listOf(statExp[0], statExp[1], statExp[2], statExp[4], statExp[4], statExp[3]),
            metLevel = 0, metLocation = 0, originGame = 0, ball = 0, egg = false, abilitySlot = 0, shiny = false, language = 0,
        )
    }

    // ---- Generation II -----------------------------------------------------------------------------------------------------

    private const val G2_MON = 32
    private const val G2_PARTY_MON = 48
    private const val G2_BOX = 0x450
    private const val G2_BOXES = 14
    private const val G2_BOX_NAME = 9
    private const val G2_EGG = 0xFD

    /** Where a game keeps things. The checksum covers from 0x2009 to one of [checkedEnds]. */
    private class Layout(
        val crystal: Boolean, val time: Int, val currentBox: Int, val boxNames: Int, val party: Int, val owned: Int, val seen: Int,
        val currentBoxData: Int, val checkedEnds: List<Int>, val checksum: Int, val badges: Int, val japanese: Boolean = false,
    ) {
        val nameLength get() = if (japanese) JAPANESE_NAME else NAME
        val boxSize get() = if (japanese) JAPANESE_BOX_SIZE else BOX_SIZE
        val boxCount get() = if (japanese) G2_JAPANESE_BOXES else G2_BOXES
        val boxBytes get() = if (japanese) G2_JAPANESE_BOX else G2_BOX
        /** How many boxes fit in the first of the two memory banks that hold them. */
        val firstBankBoxes get() = if (japanese) 6 else 7
    }

    private const val G2_JAPANESE_BOX = 0x54A
    private const val G2_JAPANESE_BOXES = 9

    private val GOLD_SILVER = Layout(false, 0x2053, 0x2724, 0x2727, 0x288A, 0x2A4C, 0x2A6C, 0x2D6C, listOf(0x2D68), 0x2D69, 0x23E4)
    private val CRYSTAL = Layout(true, 0x2052, 0x2700, 0x2703, 0x2865, 0x2A27, 0x2A47, 0x2D10, listOf(0x2B82), 0x2D0D, 0x23E5)
    // The one save this was read from has a 0 in the byte that would tell its two possible checksum ends apart.
    private val JAPANESE_CRYSTAL = Layout(true, 0x2034, 0x26E2, 0x26E5, 0x281A, 0x29AA, 0x29CA, 0x2D10, listOf(0x2AE1, 0x2AE2), 0x2D0D, 0x23C7, japanese = true)

    private fun gen2(b: ByteArray, layouts: List<Layout>, fileName: String?): GameSave? {
        val l = layouts.firstOrNull { l ->
            l.checkedEnds.any { end -> (0x2009..end).sumOf { b.u8(it) } and 0xFFFF == b.u16(l.checksum) }
        } ?: return null
        val id = b.be16(0x2009)
        val name = text(b, 0x200B, l.nameLength, l.japanese)
        if (name.isBlank()) return null
        val mon = { at: Int, listed: Int, box: Int, slot: Int, ot: String, nick: String ->
            gen2Pokemon(b, at, listed, box, slot, ot, nick, id, name, l.crystal, if (l.japanese) JAPANESE else 0)
        }
        val party = list(b, l.party, 6, G2_PARTY_MON, 0, mon, l.nameLength, l.japanese)
        val current = b.u8(l.currentBox) and 0x0F
        val boxes = (0 until l.boxCount).flatMap { box ->
            val at = when {
                box == current -> l.currentBoxData
                box < l.firstBankBoxes -> 0x4000 + box * l.boxBytes
                else -> 0x6000 + (box - l.firstBankBoxes) * l.boxBytes
            }
            list(b, at, l.boxSize, G2_MON, box + 1, mon, l.nameLength, l.japanese)
        }
        val named = gameInFileName(fileName, listOf(Game.GOLD to listOf("GOLD", "GLD", "AAU"), Game.SILVER to listOf("SILV", "SLV", "AAX")))
        val game = if (l.crystal) Game.CRYSTAL else named ?: Game.GOLD
        return GameSave(
            game = game,
            gameOptions = if (!l.crystal && named == null) listOf(Game.GOLD, Game.SILVER) else emptyList(),
            trainerName = name,
            trainerId = id,
            secretId = 0,
            playMinutes = b.be16(l.time) * 60 + b.u8(l.time + 2),
            owned = dexFlags(b, l.owned, 251),
            seen = dexFlags(b, l.seen, 251),
            nationalDex = false,
            party = party,
            boxes = boxes,
            boxNames = List(l.boxCount) { text(b, l.boxNames + it * G2_BOX_NAME, G2_BOX_NAME, l.japanese) },
            badges = b.u16(l.badges),
        )
    }

    private fun gen2Pokemon(
        b: ByteArray, at: Int, listed: Int, box: Int, slot: Int, ot: String, nick: String, trainerId: Int, trainerName: String, crystal: Boolean, language: Int,
    ): SavedPokemon? {
        val species = b.u8(at)
        if (species !in 1..251 || (listed != species && listed != G2_EGG)) return null
        val dvs = dvs(b.be16(at + 21))
        val otId = b.be16(at + 6)
        val statExp = List(5) { b.be16(at + 11 + it * 2) }
        val shiny = dvs.defense == 10 && dvs.speed == 10 && dvs.special == 10 && dvs.attack and 2 != 0
        // Only Crystal writes down where, when and at what level a Pokémon was caught.
        val metLevel = if (crystal) b.u8(at + 29) and 0x3F else 0
        val metTime = if (crystal) b.u8(at + 29) shr 6 else 0
        val metLocation = if (crystal) b.u8(at + 30) and 0x7F else 0
        return SavedPokemon(
            box = box, slot = slot, speciesId = species, nickname = nick, otName = ot, otId = otId.toLong(),
            ownCatch = otId == trainerId && ot == trainerName, pid = dvs.attack * 16L, exp = b.be24(at + 8), heldItem = b.u8(at + 1),
            friendship = b.u8(at + 27), moves = (2..5).map { b.u8(at + it) }.filter { it != 0 },
            ivs = listOf(dvs.hp, dvs.attack, dvs.defense, dvs.special, dvs.special, dvs.speed),
            evs = listOf(statExp[0], statExp[1], statExp[2], statExp[4], statExp[4], statExp[3]),
            metLevel = metLevel, metLocation = metLocation, originGame = 0, ball = 0, egg = listed == G2_EGG, abilitySlot = 0, shiny = shiny, language = language,
            metTime = metTime, form = if (species == UNOWN) gen2UnownForm(dvs.attack, dvs.defense, dvs.speed, dvs.special) else 0,
        )
    }

    // ---- shared ------------------------------------------------------------------------------------------------------------

    private class Dvs(val attack: Int, val defense: Int, val speed: Int, val special: Int) {
        /** The HP DV isn't stored: it's made of the other four's lowest bits. */
        val hp = (attack and 1 shl 3) or (defense and 1 shl 2) or (speed and 1 shl 1) or (special and 1)
    }

    private fun dvs(word: Int) = Dvs(word shr 12, (word shr 8) and 15, (word shr 4) and 15, word and 15)

    /** The Pokémon in a list at [at]; none if the list isn't whole, as with storage the game hasn't set up. */
    private fun list(
        b: ByteArray, at: Int, capacity: Int, monSize: Int, box: Int,
        mon: (at: Int, listed: Int, box: Int, slot: Int, ot: String, nick: String) -> SavedPokemon?,
        nameLength: Int = NAME, japanese: Boolean = false,
    ): List<SavedPokemon> {
        val count = b.u8(at)
        if (count > capacity || b.u8(at + 1 + count) != LIST_END) return emptyList()
        val monsAt = at + 1 + capacity + 1
        val otAt = monsAt + capacity * monSize
        val nickAt = otAt + capacity * nameLength
        return (0 until count).mapNotNull { i ->
            mon(monsAt + i * monSize, b.u8(at + 1 + i), box, i, text(b, otAt + i * nameLength, nameLength, japanese), text(b, nickAt + i * nameLength, nameLength, japanese))
        }
    }

    private fun ByteArray.be16(at: Int) = (u8(at) shl 8) or u8(at + 1)

    private fun ByteArray.be24(at: Int) = (u8(at) shl 16) or (u8(at + 1) shl 8) or u8(at + 2)

    /** Game Boy text up to the 0x50 terminator. */
    private fun text(b: ByteArray, at: Int, length: Int, japanese: Boolean = false): String = if (japanese) japaneseText(b, at, length) else buildString {
        for (i in at until at + length) {
            val c = b.u8(i)
            if (c == 0x50) break
            append(GB_CHARS[c] ?: continue)
        }
    }.trim()

    /** The games draw four katakana with the matching hiragana's tile, so those only read right beside other katakana. */
    private val SHARED_TILES = mapOf('へ' to 'ヘ', 'べ' to 'ベ', 'ぺ' to 'ペ', 'り' to 'リ')

    private fun japaneseText(b: ByteArray, at: Int, length: Int): String {
        val s = buildString {
            for (i in at until at + length) {
                val c = b.u8(i)
                if (c == 0x50) break
                GB_KANA[c].takeIf { it != '\u0000' }?.let(::append)
            }
        }.trim()
        return if (s.any { it in 'ァ'..'ヺ' }) s.map { SHARED_TILES[it] ?: it }.joinToString("") else s
    }

    private val GB_CHARS: Map<Int, String> = buildMap {
        put(0x7F, " ")
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ():;[]abcdefghijklmnopqrstuvwxyzé".forEachIndexed { i, ch -> put(0x80 + i, ch.toString()) }
        "0123456789".forEachIndexed { i, ch -> put(0xF6 + i, ch.toString()) }
        putAll(mapOf(
            0x5D to "TRAINER", // The whole name of the trainer on an in-game trade.
            0xBB to "'d", 0xBC to "'l", 0xBD to "'s", 0xBE to "'t", 0xBF to "'v", 0xE0 to "'", 0xE1 to "PK", 0xE2 to "MN", 0xE3 to "-", 0xE4 to "'r", 0xE5 to "'m",
            0xE6 to "?", 0xE7 to "!", 0xE8 to ".", 0xEF to "♂", 0xF0 to "¥", 0xF1 to "×", 0xF2 to ".", 0xF3 to "/", 0xF4 to ",", 0xF5 to "♀",
        ))
    }
}

/** Generation I species indexes, which follow the order the Pokémon were made in; Generation II's match the National Pokédex. */
object Gen1Species {
    /** National numbers for indexes 1 onwards, 0 where the index has no Pokémon; from the pret decompilation of Red and Blue. */
    private val BY_INDEX = intArrayOf(
        112, 115, 32, 35, 21, 100, 34, 80, 2, 103, 108, 102, 88, 94, 29, 31, 104, 111, 131, 59, 151, 130, 90, 72, 92, 123, 120, 9, 127, 114,
        0, 0, 58, 95, 22, 16, 79, 64, 75, 113, 67, 122, 106, 107, 24, 47, 54, 96, 76, 0, 126, 0, 125, 82, 109, 0, 56, 86, 50, 128, 0, 0, 0,
        83, 48, 149, 0, 0, 0, 84, 60, 124, 146, 144, 145, 132, 52, 98, 0, 0, 0, 37, 38, 25, 26, 0, 0, 147, 148, 140, 141, 116, 117, 0, 0,
        27, 28, 138, 139, 39, 40, 133, 136, 135, 134, 66, 41, 23, 46, 61, 62, 13, 14, 15, 0, 85, 57, 51, 49, 87, 0, 0, 10, 11, 12, 68, 0,
        55, 97, 42, 150, 143, 129, 0, 0, 89, 0, 99, 91, 0, 101, 36, 110, 53, 105, 0, 93, 63, 65, 17, 18, 121, 1, 3, 73, 0, 118, 119, 0, 0,
        0, 0, 77, 78, 19, 20, 33, 30, 74, 137, 142, 0, 81, 0, 0, 4, 7, 5, 8, 6, 0, 0, 0, 0, 43, 44, 45, 69, 70, 71,
    )

    /** The National Pokédex number for an index, or null for an empty slot or a placeholder. */
    fun national(index: Int): Int? = BY_INDEX.getOrNull(index - 1)?.takeIf { it != 0 }
}
