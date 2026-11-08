package io.lemoncore.opendatadex.data.save

import io.lemoncore.opendatadex.data.Game

/**
 * Diamond, Pearl, Platinum, HeartGold and SoulSilver saves, following Project Pokémon's notes on the format and the
 * pret decompilations, and checked against real saves.
 *
 * A save is two 256 KB copies, each with a general block and a storage block for the PC. The game writes the storage
 * block only when the PC changed, so the newest general block and the newest storage block can sit in different copies.
 */
object Gen4Save {
    internal const val SIZE = 0x80000
    /** Emulators' .dsv files add a short footer after the save. */
    internal const val MAX_DSV_FOOTER = 0x100
    private const val COPY = 0x40000
    private const val BOX_COUNT = 18
    private const val BOX_SIZE = 30
    private const val BOX_MON = 136
    private const val PARTY_MON = 236
    private const val BOX_NAME = 40
    private const val DEX_MAGIC = 0xBEEFCAFEL
    private const val NATIONAL_COUNT = 493
    private const val SINNOH_FIRST_BOX = 4
    private const val SINNOH_BOX_BYTES = BOX_SIZE * BOX_MON
    private const val SINNOH_BOX_NAMES = SINNOH_FIRST_BOX + BOX_COUNT * SINNOH_BOX_BYTES
    private const val SINNOH_DEX_NATIONAL = 0x31B

    /**
     * Where one family of games keeps things: its [generalSize] and [storageSize] blocks, what the checksum leaves out
     * of a block ([footer]), the boxes, the trainer, the party and the Pokédex. [national] is within the Pokédex.
     */
    private class Layout(
        val games: List<Game>, val generalSize: Int, val storageStart: Int, val storageSize: Int, val footer: Int,
        val firstBox: Int, val boxBytes: Int, val boxNames: Int, val trainer: Int, val party: Int, val dex: Int, val national: Int,
    )

    private val DIAMOND_PEARL = Layout(listOf(Game.DIAMOND, Game.PEARL), 0xC100, 0xC100, 0x121E0, 0x14, SINNOH_FIRST_BOX, SINNOH_BOX_BYTES, SINNOH_BOX_NAMES, 0x64, 0x98, 0x12DC, SINNOH_DEX_NATIONAL)
    private val PLATINUM = Layout(listOf(Game.PLATINUM), 0xCF2C, 0xCF2C, 0x121E4, 0x14, SINNOH_FIRST_BOX, SINNOH_BOX_BYTES, SINNOH_BOX_NAMES, 0x68, 0xA0, 0x1328, SINNOH_DEX_NATIONAL)
    private val HEARTGOLD_SOULSILVER = Layout(listOf(Game.HEARTGOLD, Game.SOULSILVER), 0xF628, 0xF700, 0x12310, 0x10, 0, 0x1000, 0x12008, 0x64, 0x98, 0x12B8, 0x337)

    fun read(bytes: ByteArray, fileName: String? = null): GameSave? {
        if (bytes.size !in SIZE..SIZE + MAX_DSV_FOOTER) return null
        return listOf(HEARTGOLD_SOULSILVER, PLATINUM, DIAMOND_PEARL).firstNotNullOfOrNull { read(bytes, it, fileName) }
    }

    private fun read(bytes: ByteArray, l: Layout, fileName: String?): GameSave? {
        val general = newest(bytes, 0, l.generalSize, l.footer) ?: return null
        val storage = newest(bytes, l.storageStart, l.storageSize, l.footer) ?: return null
        val dex = general + l.dex
        if (bytes.u32(dex) != DEX_MAGIC) return null

        val trainer = general + l.trainer
        val name = gen4Text(bytes, trainer, 8)
        val id = bytes.u16(trainer + 0x10)
        val secret = bytes.u16(trainer + 0x12)
        val fullId = id.toLong() or (secret.toLong() shl 16)
        val partyAt = general + l.party
        val count = bytes.u32(partyAt - 4).toInt().coerceIn(0, 6)
        val party = (0 until count).mapNotNull { readPk45(bytes, partyAt + it * PARTY_MON, 0, it, fullId, name, gen5 = false) }
        val boxes = (0 until BOX_COUNT * BOX_SIZE).mapNotNull { i ->
            val box = i / BOX_SIZE
            val slot = i % BOX_SIZE
            readPk45(bytes, storage + l.firstBox + box * l.boxBytes + slot * BOX_MON, box + 1, slot, fullId, name, gen5 = false)
        }
        val known = l.games.singleOrNull() ?: pairedGame(l.games, party + boxes, fileName, FILE_MARKS)
        val johto = l === HEARTGOLD_SOULSILVER
        return GameSave(
            game = known ?: l.games.first(),
            gameOptions = if (known == null) l.games else emptyList(),
            trainerName = name,
            trainerId = id,
            secretId = secret,
            playMinutes = bytes.u16(trainer + 0x22) * 60 + bytes.u8(trainer + 0x24),
            owned = dexFlags(bytes, dex + 4, NATIONAL_COUNT),
            seen = dexFlags(bytes, dex + 0x44, NATIONAL_COUNT),
            nationalDex = bytes.u8(dex + l.national) != 0,
            party = party,
            boxes = boxes,
            boxNames = List(BOX_COUNT) { gen4Text(bytes, storage + l.boxNames + it * BOX_NAME, BOX_NAME / 2) },
            badges = bytes.u8(trainer + 0x1A) or (if (johto) bytes.u8(trainer + 0x1F) shl 8 else 0),
            startedAt = bytes.u32(general + 0x34).takeIf { it != 0L },
        )
    }

    private val FILE_MARKS = mapOf(
        Game.DIAMOND to listOf("DIAMOND", "ADA"), Game.PEARL to listOf("PEARL", "APA"),
        Game.HEARTGOLD to listOf("HEART", "IPK"), Game.SOULSILVER to listOf("SOUL", "IPG"),
    )

    /**
     * Where the newer valid copy of a block starts; null when neither copy of it is whole. A block's last 20 bytes are
     * its two save counts, its size, a magic number, its id and a checksum, though HeartGold and SoulSilver count the
     * first of them as part of the block.
     */
    private fun newest(b: ByteArray, offset: Int, size: Int, footer: Int): Int? = listOf(offset, offset + COPY)
        .filter { b.u32(it + size - 12) == size.toLong() && crc16(b, it, size - footer) == b.u16(it + size - 2) }
        .maxByOrNull { (b.u32(it + size - 20) shl 32) or b.u32(it + size - 16) }

    /** Generation IV text: 16-bit codes up to 0xFFFF. */
    internal fun gen4Text(b: ByteArray, at: Int, length: Int): String = buildString {
        for (i in 0 until length) {
            val c = b.u16(at + i * 2)
            if (c == 0xFFFF || c == 0) break
            append(GEN4_TEXT.getOrNull(c)?.takeIf { it != '\u0000' } ?: continue)
        }
    }.trim()
}

/** CRC-16-CCITT of [length] bytes, as Nintendo DS saves use. */
internal fun crc16(b: ByteArray, at: Int, length: Int): Int {
    var crc = 0xFFFF
    for (i in at until at + length) {
        crc = crc xor (b.u8(i) shl 8)
        repeat(8) { crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1 }
        crc = crc and 0xFFFF
    }
    return crc
}

/**
 * One Pokémon's 136 bytes from Generation IV or V (the first 136 of a party Pokémon): decrypted, unshuffled and checked.
 * [trainerFullId] and [trainerName] say whether it's the trainer's own.
 */
internal fun readPk45(b: ByteArray, at: Int, box: Int, slot: Int, trainerFullId: Long, trainerName: String, gen5: Boolean): SavedPokemon? {
    val pid = b.u32(at)
    val checksum = b.u16(at + 6)
    if (pid == 0L && checksum == 0) return null
    val data = decryptPokemon(b, at + 8, 128, checksum)
    val sum = (0 until 64).sumOf { data.u16(it * 2) } and 0xFFFF
    if (sum != checksum) return null
    val order = BLOCK_ORDERS[(((pid and 0x3E000) ushr 13) % 24).toInt()]
    fun part(c: Char) = order.indexOf(c) * 32
    val a = part('A'); val m = part('B'); val n = part('C'); val d = part('D')

    val speciesId = data.u16(a)
    if (speciesId !in 1..if (gen5) 649 else 493) return null
    val otId = data.u32(a + 4)
    val ivWord = data.u32(m + 0x10)
    // Stored as HP, Attack, Defense, Speed, Sp. Atk, Sp. Def; kept in the usual HP to Speed order.
    val iv = IntArray(6) { ((ivWord ushr (5 * it)) and 31).toInt() }
    val ev = IntArray(6) { data.u8(a + 0x10 + it) }
    val text = { at: Int, length: Int -> if (gen5) gen5Text(data, at, length) else Gen4Save.gen4Text(data, at, length) }
    val otName = text(d, 8)
    // Platinum and HeartGold keep places and balls newer than Diamond and Pearl's fields can hold beside them.
    val place = (if (gen5) 0 else data.u16(m + 0x1E)).takeIf { it != 0 } ?: data.u16(d + 0x18)
    val ball = (if (gen5) 0 else data.u8(d + 0x1E)).takeIf { it != 0 } ?: data.u8(d + 0x1B)
    val tid = otId and 0xFFFF
    val sid = otId ushr 16
    val metMonth = data.u8(d + 0x14)
    val metDay = data.u8(d + 0x15)
    val flags = data.u8(m + 0x18)
    return SavedPokemon(
        box = box,
        slot = slot,
        speciesId = speciesId,
        nickname = text(n, 11),
        otName = otName,
        otId = otId,
        ownCatch = otId == trainerFullId && otName == trainerName,
        pid = pid,
        exp = data.u32(a + 8).toInt(),
        heldItem = data.u16(a + 2),
        friendship = data.u8(a + 0xC),
        moves = (0 until 4).map { data.u16(m + it * 2) }.filter { it != 0 },
        ivs = listOf(iv[0], iv[1], iv[2], iv[4], iv[5], iv[3]),
        evs = listOf(ev[0], ev[1], ev[2], ev[4], ev[5], ev[3]),
        metLevel = data.u8(d + 0x1C) and 0x7F,
        metLocation = place,
        originGame = data.u8(n + 0x17),
        ball = ball,
        egg = (ivWord ushr 30) and 1 == 1L,
        // Generation V moved the normal ability's bit to the upper half of the PID and added hidden abilities.
        abilitySlot = when {
            gen5 && data.u8(m + 0x1A) and 1 == 1 -> HIDDEN_ABILITY_SLOT
            gen5 -> ((pid ushr 16) and 1).toInt()
            else -> (pid and 1).toInt()
        },
        shiny = (tid xor sid xor (pid and 0xFFFF) xor (pid ushr 16)) < 8,
        language = data.u8(a + 0xF),
        metDate = if (metMonth != 0 && metDay != 0) (2000 + data.u8(d + 0x13)) * 10_000 + metMonth * 100 + metDay else 0,
        form = flags shr 3,
        gender = (flags shr 1) and 3,
        nature = if (gen5) data.u8(m + 0x19) else -1,
    )
}

/** The [length] bytes at [at], decrypted with the keystream Generation IV onwards seeds from [seed]. */
internal fun decryptPokemon(b: ByteArray, at: Int, length: Int, seed: Int): ByteArray {
    val data = ByteArray(length)
    var state = seed
    for (w in 0 until length / 2) {
        state = state * 0x41C64E6D + 0x6073
        val word = b.u16(at + w * 2) xor (state ushr 16)
        data[w * 2] = word.toByte()
        data[w * 2 + 1] = (word shr 8).toByte()
    }
    return data
}
