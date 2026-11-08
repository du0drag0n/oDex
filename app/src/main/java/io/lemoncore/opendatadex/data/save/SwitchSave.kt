package io.lemoncore.opendatadex.data.save

import io.lemoncore.opendatadex.data.Game

/**
 * Let's Go Pikachu and Eevee saves. Their layout was worked out by reading a real save. The Pokémon are Sun and Moon's,
 * and the Pokédex is too, but they all live in one long storage of 1,000 slots: the party is six slot numbers into it,
 * and nothing divides it into boxes, so oDex shows it in pages of 30.
 */
object LetsGoSave {
    private const val SIZE = 0x100000
    private const val BLOCK_TABLE = 0xB8610
    private const val BLOCK_TABLE_MAGIC = 0x42454546L
    private const val STATUS = 0x1000
    private const val NAME = 0x38
    private const val DEX = 0x2A00
    private const val DEX_CAUGHT = 0x88
    private const val DEX_SEEN = 0xF0
    private const val DEX_REGION = 0x8C
    private const val SPECIES_COUNT = 809
    private const val PARTY = 0x5A00
    private const val STORAGE = 0x5C00
    private const val STORAGE_SLOTS = 1000
    private const val SLOT_BYTES = 260
    private const val BOX_SIZE = 30
    private const val PLAY = 0x45400
    private val GAMES = listOf(Game.LETS_GO_PIKACHU, Game.LETS_GO_EEVEE)

    fun read(bytes: ByteArray, fileName: String? = null): GameSave? {
        if (bytes.size != SIZE || bytes.u32(BLOCK_TABLE) != BLOCK_TABLE_MAGIC) return null
        val name = Gen67Save.gfText(bytes, STATUS + NAME, 13)
        val fullId = bytes.u32(STATUS)
        val partySlots = (0 until 6).map { bytes.u16(PARTY + it * 2) }.filter { it < STORAGE_SLOTS }
        val party = partySlots.mapIndexedNotNull { i, slot -> Gen67Save.readPk67(bytes, STORAGE + slot * SLOT_BYTES, 0, i, fullId, name) }
        val boxes = (0 until STORAGE_SLOTS).filter { it !in partySlots }.mapNotNull { i ->
            Gen67Save.readPk67(bytes, STORAGE + i * SLOT_BYTES, i / BOX_SIZE + 1, i % BOX_SIZE, fullId, name)
        }
        val known = saveOrigin(bytes.u8(STATUS + 4))?.takeIf { it in GAMES }
            ?: gameInFileName(fileName, listOf(Game.LETS_GO_PIKACHU to listOf("PIKACHU"), Game.LETS_GO_EEVEE to listOf("EEVEE")))
        return GameSave(
            game = known ?: GAMES.first(),
            gameOptions = if (known == null) GAMES else emptyList(),
            trainerName = name,
            trainerId = (fullId % 1_000_000).toInt(),
            secretId = (fullId / 1_000_000).toInt(),
            playMinutes = bytes.u16(PLAY) * 60 + bytes.u8(PLAY + 2),
            owned = dexFlags(bytes, DEX + DEX_CAUGHT, SPECIES_COUNT),
            seen = (0 until 4).flatMapTo(LinkedHashSet()) { dexFlags(bytes, DEX + DEX_SEEN + it * DEX_REGION, SPECIES_COUNT) },
            nationalDex = false,
            party = party,
            boxes = boxes,
            boxNames = List(boxes.maxOfOrNull { it.box } ?: 0) { "" },
            badges = GameSave.UNKNOWN_BADGES,
        )
    }
}

/**
 * Brilliant Diamond and Shining Pearl saves. Their layout was worked out by reading a real save, so sizes other than
 * that one's are accepted as long as what's read makes sense. The party and every box slot are 344 bytes, of which the
 * first 328 hold the Pokémon, and the Pokédex is a run of 32-bit states rather than flags.
 */
object BdspSave {
    private val SIZES = 0xE0000..0x100000
    private const val PARTY = 0x14098
    private const val BOX_NAMES = 0x148AA
    private const val BOX_NAME = 0x22
    private const val BOXES = 0x14EF4
    private const val BOX_COUNT = 40
    private const val BOX_SIZE = 30
    private const val SLOT_BYTES = 344
    private const val MON_BYTES = 328
    private const val BLOCK_BYTES = 80
    private const val TRAINER_NAME = 0x79BB4
    private const val TRAINER_ID = 0x79BD0
    private const val BADGE_COUNT = 0x79BDD
    private const val PLAY = 0x79C04
    private const val DEX = 0x7A324
    private const val SPECIES_COUNT = 493
    private const val DEX_SEEN = 2L
    private const val DEX_CAUGHT = 3L
    /** The Sinnoh Pokédex's size: catching more than this takes the National Pokédex. */
    private const val SINNOH_COUNT = 151
    private val GAMES = listOf(Game.BRILLIANT_DIAMOND, Game.SHINING_PEARL)
    private val FILE_MARKS = mapOf(Game.BRILLIANT_DIAMOND to listOf("DIAMOND"), Game.SHINING_PEARL to listOf("PEARL"))

    fun read(bytes: ByteArray, fileName: String? = null): GameSave? {
        if (bytes.size !in SIZES) return null
        val name = Gen67Save.gfText(bytes, TRAINER_NAME, 13)
        if (name.isBlank()) return null
        val fullId = bytes.u32(TRAINER_ID)
        val party = (0 until 6).mapNotNull { readPokemon(bytes, PARTY + it * SLOT_BYTES, 0, it, fullId, name) }
        if (party.isEmpty()) return null
        val boxes = (0 until BOX_COUNT * BOX_SIZE).mapNotNull { i ->
            readPokemon(bytes, BOXES + i * SLOT_BYTES, i / BOX_SIZE + 1, i % BOX_SIZE, fullId, name)
        }
        val known = pairedGame(GAMES, party + boxes, fileName, FILE_MARKS)
        val states = (1..SPECIES_COUNT).associateWith { bytes.u32(DEX + (it - 1) * 4) }
        val owned = states.filterValues { it == DEX_CAUGHT }.keys
        return GameSave(
            game = known ?: GAMES.first(),
            gameOptions = if (known == null) GAMES else emptyList(),
            trainerName = name,
            trainerId = (fullId % 1_000_000).toInt(),
            secretId = (fullId / 1_000_000).toInt(),
            playMinutes = bytes.u16(PLAY) * 60 + bytes.u8(PLAY + 2),
            owned = owned,
            seen = states.filterValues { it >= DEX_SEEN }.keys,
            nationalDex = owned.size > SINNOH_COUNT,
            party = party,
            boxes = boxes,
            boxNames = List(BOX_COUNT) { Gen67Save.gfText(bytes, BOX_NAMES + it * BOX_NAME, BOX_NAME / 2) },
            badges = (1 shl bytes.u8(BADGE_COUNT).coerceAtMost(8)) - 1,
        )
    }

    /** One Pokémon: decrypted, unshuffled and checked. An empty slot reads as species 0. */
    private fun readPokemon(b: ByteArray, at: Int, box: Int, slot: Int, trainerFullId: Long, trainerName: String): SavedPokemon? {
        val key = b.u32(at)
        val checksum = b.u16(at + 6)
        val data = decryptPokemon(b, at + 8, MON_BYTES - 8, key.toInt())
        if (((0 until (MON_BYTES - 8) / 2).sumOf { data.u16(it * 2) } and 0xFFFF) != checksum) return null
        val order = BLOCK_ORDERS[(((key and 0x3E000) ushr 13) % 24).toInt()]
        // These offsets are counted from the start of a Pokémon with its 8-byte header.
        fun at(offset: Int) = order.indexOf("ABCD"[(offset - 8) / BLOCK_BYTES]) * BLOCK_BYTES + (offset - 8) % BLOCK_BYTES
        fun u8(offset: Int) = data.u8(at(offset))
        fun u16(offset: Int) = data.u16(at(offset))
        fun u32(offset: Int) = data.u32(at(offset))
        fun text(offset: Int) = Gen67Save.gfText(data, at(offset), 13)

        val speciesId = u16(0x08)
        if (speciesId !in 1..1025) return null
        val otId = u32(0x0C)
        val pid = u32(0x1C)
        val ivWord = u32(0x8C)
        // Stored as HP, Attack, Defense, Speed, Sp. Atk, Sp. Def; kept in the usual HP to Speed order.
        val iv = IntArray(6) { ((ivWord ushr (5 * it)) and 31).toInt() }
        val ev = IntArray(6) { u8(0x26 + it) }
        val otName = text(0xF8)
        val metMonth = u8(0x11D)
        val metDay = u8(0x11E)
        val tid = otId and 0xFFFF
        val sid = otId ushr 16
        return SavedPokemon(
            box = box,
            slot = slot,
            speciesId = speciesId,
            nickname = text(0x58),
            otName = otName,
            otId = otId,
            ownCatch = otId == trainerFullId && otName == trainerName,
            pid = pid,
            exp = u32(0x10).toInt(),
            heldItem = u16(0x0A),
            friendship = u8(0x112),
            moves = (0 until 4).map { u16(0x72 + it * 2) }.filter { it != 0 },
            ivs = listOf(iv[0], iv[1], iv[2], iv[4], iv[5], iv[3]),
            evs = listOf(ev[0], ev[1], ev[2], ev[4], ev[5], ev[3]),
            metLevel = u8(0x125) and 0x7F,
            metLocation = u16(0x122),
            originGame = u8(0xDE),
            ball = u8(0x124),
            egg = (ivWord ushr 30) and 1 == 1L,
            abilitySlot = when (u8(0x16) and 7) { 2 -> 1; 4 -> HIDDEN_ABILITY_SLOT; else -> 0 },
            shiny = (tid xor sid xor (pid and 0xFFFF) xor (pid ushr 16)) < 16,
            language = u8(0xE2),
            metDate = if (metMonth != 0 && metDay != 0) (2000 + u8(0x11C)) * 10_000 + metMonth * 100 + metDay else 0,
            form = u16(0x24),
            gender = (u8(0x22) shr 2) and 3,
            nature = u8(0x20),
        )
    }
}
