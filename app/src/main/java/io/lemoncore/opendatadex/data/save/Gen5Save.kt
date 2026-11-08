package io.lemoncore.opendatadex.data.save

import io.lemoncore.opendatadex.data.Game

/**
 * Black, White, Black 2 and White 2 saves, following Project Pokémon's notes on the format and checked against real
 * saves. The save is a run of blocks at fixed places, each 0x100-aligned, and a table of their checksums that tells the
 * two pairs of games apart.
 */
object Gen5Save {
    private const val BOX_NAMES = 0
    private const val FIRST_BOX = 0x400
    private const val BOX_BYTES = 0x1000
    private const val BOX_COUNT = 24
    private const val BOX_SIZE = 30
    private const val BOX_MON = 136
    private const val PARTY = 0x18E00
    private const val PARTY_MON = 220
    private const val TRAINER = 0x19400
    private const val ADVENTURE = 0x1D900
    private const val NATIONAL_COUNT = 649
    private const val DEX_REGION = 0x54

    /** Where a pair of games keeps its checksum table, with the table's size, then its Pokédex and badges. */
    private class Layout(val games: List<Game>, val checksums: Int, val checksumBytes: Int, val dex: Int, val misc: Int)

    private val BLACK_WHITE = Layout(listOf(Game.BLACK, Game.WHITE), 0x23F00, 0x8C, 0x21600, 0x21200)
    private val BLACK_2_WHITE_2 = Layout(listOf(Game.BLACK_2, Game.WHITE_2), 0x25F00, 0x94, 0x21400, 0x21100)

    fun read(bytes: ByteArray, fileName: String? = null): GameSave? {
        if (bytes.size !in Gen4Save.SIZE..Gen4Save.SIZE + Gen4Save.MAX_DSV_FOOTER) return null
        val l = listOf(BLACK_WHITE, BLACK_2_WHITE_2).firstOrNull {
            crc16(bytes, it.checksums, it.checksumBytes) == bytes.u16(it.checksums + it.checksumBytes + 0xE)
        } ?: return null

        val name = gen5Text(bytes, TRAINER + 4, 8)
        val id = bytes.u16(TRAINER + 0x14)
        val secret = bytes.u16(TRAINER + 0x16)
        val fullId = id.toLong() or (secret.toLong() shl 16)
        val count = bytes.u8(PARTY + 4).coerceIn(0, 6)
        val party = (0 until count).mapNotNull { readPk45(bytes, PARTY + 8 + it * PARTY_MON, 0, it, fullId, name, gen5 = true) }
        val boxes = (0 until BOX_COUNT * BOX_SIZE).mapNotNull { i ->
            val box = i / BOX_SIZE
            val slot = i % BOX_SIZE
            readPk45(bytes, FIRST_BOX + box * BOX_BYTES + slot * BOX_MON, box + 1, slot, fullId, name, gen5 = true)
        }
        val known = saveOrigin(bytes.u8(TRAINER + 0x1F))?.takeIf { it in l.games }
            ?: gameInFileName(fileName, l.games.map { it to listOf(it.key.replace("-", " ").uppercase(), it.key.replace("-", "").uppercase()) })
        return GameSave(
            game = known ?: l.games.first(),
            gameOptions = if (known == null) l.games else emptyList(),
            trainerName = name,
            trainerId = id,
            secretId = secret,
            playMinutes = bytes.u16(TRAINER + 0x24) * 60 + bytes.u8(TRAINER + 0x26),
            owned = dexFlags(bytes, l.dex + 8, NATIONAL_COUNT),
            seen = (0 until 4).flatMapTo(LinkedHashSet()) { dexFlags(bytes, l.dex + 8 + DEX_REGION * (it + 1), NATIONAL_COUNT) },
            nationalDex = bytes.u8(l.dex + 4) and 1 == 1,
            party = party,
            boxes = boxes,
            boxNames = List(BOX_COUNT) { gen5Text(bytes, BOX_NAMES + 4 + it * 0x28, 10) },
            badges = bytes.u8(l.misc + 4),
            startedAt = bytes.u32(ADVENTURE + 0x34).takeIf { it != 0L },
        )
    }
}

/** Generation V text: Unicode up to 0xFFFF, with its own code points for the gender symbols. */
internal fun gen5Text(b: ByteArray, at: Int, length: Int): String = buildString {
    for (i in 0 until length) {
        val c = b.u16(at + i * 2)
        if (c == 0xFFFF || c == 0) break
        append(when (c) { 0x246D -> '♂'; 0x246E -> '♀'; else -> c.toChar() })
    }
}.trim()
