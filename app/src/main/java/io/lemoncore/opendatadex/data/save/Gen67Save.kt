package io.lemoncore.opendatadex.data.save

import io.lemoncore.opendatadex.data.Game

/**
 * X, Y, Omega Ruby, Alpha Sapphire, Sun, Moon, Ultra Sun and Ultra Moon saves, as a 3DS hands them over (the file called
 * main), checked against real saves from each pair of games. Each pair has its own file size and a fixed run of blocks,
 * and every one of them ends with a block table marked BEEF.
 */
object Gen67Save {
    private const val BOX_SIZE = 30
    private const val BOX_MON = 232
    private const val PARTY_MON = 260
    private const val BOX_NAME = 34
    private const val BLOCK_TABLE_MAGIC = 0x42454546L
    private const val BLOCK_TABLE_FROM_END = 0x1F0

    /**
     * One pair of games. [status] is the trainer's block, with the name at [name] and language at [language] within it;
     * [time] holds the start date at [startedAt]. The Pokédex's caught flags and then its seen flags, four regions of
     * [dexRegion] bytes, start at [dex] + [dexCaught], with [dexRegion] + [dexCaughtSize] between the two.
     */
    private class Layout(
        val size: Int, val games: List<Game>, val party: Int, val box: Int, val boxCount: Int, val boxNames: Int, val status: Int,
        val name: Int, val play: Int, val time: Int, val startedAt: Int, val dex: Int, val dexCaught: Int, val dexCaughtSize: Int,
        val dexRegion: Int, val dexCount: Int, val badges: Int?, val dexForeign: Int? = null,
    )

    /** X and Y mark Pokémon that came from other games apart from their own catches, up to Generation V. */
    private const val FOREIGN_COUNT = 649

    private val XY = Layout(0x65600, listOf(Game.X, Game.Y), 0x14200, 0x22600, 31, 0x4400, 0x14000, 0x48, 0x1800, 0x1200, 0x18, 0x15000, 8, 0x60, 0x60, 721, 0x420C, dexForeign = 0x64C)
    private val ORAS = Layout(0x76000, listOf(Game.OMEGA_RUBY, Game.ALPHA_SAPPHIRE), 0x14200, 0x33000, 31, 0x4400, 0x14000, 0x48, 0x1800, 0x1200, 0x18, 0x15000, 8, 0x60, 0x60, 721, 0x420C)
    private val SM = Layout(0x6BE00, listOf(Game.SUN, Game.MOON), 0x1400, 0x4E00, 32, 0x4800, 0x1200, 0x38, 0x40C00, 0x4600, 0x28, 0x2A00, 0x88, 0x68, 0x8C, 802, null)
    private val USUM = Layout(0x6CC00, listOf(Game.ULTRA_SUN, Game.ULTRA_MOON), 0x1600, 0x5200, 32, 0x4C00, 0x1400, 0x38, 0x41000, 0x4A00, 0x28, 0x2C00, 0x88, 0x68, 0x8C, 807, null)

    fun read(bytes: ByteArray, fileName: String? = null): GameSave? {
        val l = listOf(XY, ORAS, SM, USUM).firstOrNull { it.size == bytes.size } ?: return null
        if (bytes.u32(bytes.size - BLOCK_TABLE_FROM_END) != BLOCK_TABLE_MAGIC) return null
        val seventh = l.games.first().dataGen == 7

        val name = gfText(bytes, l.status + l.name, 13)
        val fullId = bytes.u32(l.status)
        val partyCount = bytes.u8(l.party + 6 * PARTY_MON).coerceIn(0, 6)
        val party = (0 until partyCount).mapNotNull { readPk67(bytes, l.party + it * PARTY_MON, 0, it, fullId, name) }
        val boxes = (0 until l.boxCount * BOX_SIZE).mapNotNull { i ->
            readPk67(bytes, l.box + i * BOX_MON, i / BOX_SIZE + 1, i % BOX_SIZE, fullId, name)
        }
        val known = saveOrigin(bytes.u8(l.status + 4))?.takeIf { it in l.games }
            ?: gameInFileName(fileName, l.games.map { it to listOf(it.key.replace("-", " ").uppercase(), it.key.replace("-", "").uppercase()) })
        val seenAt = l.dex + l.dexCaught + l.dexCaughtSize
        return GameSave(
            game = known ?: l.games.first(),
            gameOptions = if (known == null) l.games else emptyList(),
            trainerName = name,
            // Sun onwards shows the last six digits of the whole ID, and the rest is the secret part.
            trainerId = if (seventh) (fullId % 1_000_000).toInt() else (fullId and 0xFFFF).toInt(),
            secretId = if (seventh) (fullId / 1_000_000).toInt() else (fullId ushr 16).toInt(),
            playMinutes = bytes.u16(l.play) * 60 + bytes.u8(l.play + 2),
            owned = dexFlags(bytes, l.dex + l.dexCaught, l.dexCount) + l.dexForeign?.let { dexFlags(bytes, l.dex + it, FOREIGN_COUNT) }.orEmpty(),
            seen = (0 until 4).flatMapTo(LinkedHashSet()) { dexFlags(bytes, seenAt + it * l.dexRegion, l.dexCount) },
            nationalDex = !seventh && bytes.u8(l.dex + 4) and 1 == 1,
            party = party,
            boxes = boxes,
            boxNames = List(l.boxCount) { gfText(bytes, l.boxNames + it * BOX_NAME, BOX_NAME / 2) },
            badges = l.badges?.let { bytes.u8(it) } ?: 0,
            startedAt = bytes.u32(l.time + l.startedAt).takeIf { it != 0L },
        )
    }

    /** One Pokémon's 232 bytes (the first 232 of a party Pokémon): decrypted, unshuffled and checked. An empty slot reads as species 0. */
    internal fun readPk67(b: ByteArray, at: Int, box: Int, slot: Int, trainerFullId: Long, trainerName: String): SavedPokemon? {
        val key = b.u32(at)
        val checksum = b.u16(at + 6)
        val data = decryptPokemon(b, at + 8, 224, key.toInt())
        if (((0 until 112).sumOf { data.u16(it * 2) } and 0xFFFF) != checksum) return null
        val order = BLOCK_ORDERS[(((key and 0x3E000) ushr 13) % 24).toInt()]
        // These offsets are the usual ones, counted from the start of a Pokémon with its 8-byte header.
        fun at(offset: Int) = order.indexOf("ABCD"[(offset - 8) / 56]) * 56 + (offset - 8) % 56
        fun u8(offset: Int) = data.u8(at(offset))
        fun u16(offset: Int) = data.u16(at(offset))
        fun u32(offset: Int) = data.u32(at(offset))
        fun text(offset: Int) = gfText(data, at(offset), 13)

        val speciesId = u16(0x08)
        if (speciesId !in 1..809) return null
        val otId = u32(0x0C)
        val pid = u32(0x18)
        val ivWord = u32(0x74)
        // Stored as HP, Attack, Defense, Speed, Sp. Atk, Sp. Def; kept in the usual HP to Speed order.
        val iv = IntArray(6) { ((ivWord ushr (5 * it)) and 31).toInt() }
        val ev = IntArray(6) { u8(0x1E + it) }
        val otName = text(0xB0)
        val metMonth = u8(0xD5)
        val metDay = u8(0xD6)
        val flags = u8(0x1D)
        val tid = otId and 0xFFFF
        val sid = otId ushr 16
        return SavedPokemon(
            box = box,
            slot = slot,
            speciesId = speciesId,
            nickname = text(0x40),
            otName = otName,
            otId = otId,
            ownCatch = otId == trainerFullId && otName == trainerName,
            pid = pid,
            exp = u32(0x10).toInt(),
            heldItem = u16(0x0A),
            friendship = u8(0xCA),
            moves = (0 until 4).map { u16(0x5A + it * 2) }.filter { it != 0 },
            ivs = listOf(iv[0], iv[1], iv[2], iv[4], iv[5], iv[3]),
            evs = listOf(ev[0], ev[1], ev[2], ev[4], ev[5], ev[3]),
            metLevel = u8(0xDD) and 0x7F,
            metLocation = u16(0xDA),
            originGame = u8(0xDF),
            ball = u8(0xDC),
            egg = (ivWord ushr 30) and 1 == 1L,
            abilitySlot = when (u8(0x15) and 7) { 2 -> 1; 4 -> HIDDEN_ABILITY_SLOT; else -> 0 },
            shiny = (tid xor sid xor (pid and 0xFFFF) xor (pid ushr 16)) < 16,
            language = u8(0xE3),
            metDate = if (metMonth != 0 && metDay != 0) (2000 + u8(0xD4)) * 10_000 + metMonth * 100 + metDay else 0,
            form = flags shr 3,
            gender = (flags shr 1) and 3,
            nature = u8(0x1C),
        )
    }

    /**
     * Text from Generation VI onwards: Unicode up to a zero, with the gender symbols at private code points. Chinese
     * species names sit at other private code points; they're dropped, which leaves oDex's own name for the species.
     */
    internal fun gfText(b: ByteArray, at: Int, length: Int): String = buildString {
        for (i in 0 until length) {
            val c = b.u16(at + i * 2)
            if (c == 0) break
            when (c) {
                0xE08E -> append('♂')
                0xE08F -> append('♀')
                in 0xE000..0xF8FF -> {}
                else -> append(c.toChar())
            }
        }
    }.trim()
}
