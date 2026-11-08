package io.lemoncore.opendatadex.data.save

import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.user.GameCopyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class Gen4SaveTest {
    private val trainer = 12345L or (54321L shl 16)

    @Test
    fun readsBadgesTheStartDateAndWhenAPokemonWasMet() {
        val save = GameSave.read(fakeSave(), "POKEMON_HG_IPKE.sav")!!
        assertEquals(Game.HEARTGOLD, save.game)
        assertEquals(12, save.badges.countOneBits())
        assertEquals(0xFF, save.badges and 0xFF)
        assertEquals(LocalDate.of(2017, 9, 21), consoleDate(save.startedAt!!))
        assertEquals(20170921, save.party.single().metDate)
        assertEquals(LocalDate.of(2017, 9, 21), metLocalDate(save.party.single().metDate))
    }

    @Test
    fun readsAnEmulatorsFileWithAFooterAfterTheSave() {
        assertEquals(158, GameSave.read(fakeSave() + ByteArray(122), "POKEMON_HG_IPKE.dsv")!!.party.single().speciesId)
    }

    @Test
    fun anAdventureWithNoDateHasNone() {
        assertNull(GameSave.read(fakeSave(started = 0), "POKEMON_HG_IPKE.sav")!!.startedAt)
    }

    @Test
    fun aMetDateOutsideTheCalendarIsNoDate() {
        assertNull(metLocalDate(0))
        assertNull(metLocalDate(20170231))
    }

    @Test
    fun aSaveOnlyUpdatesTheCopyWithItsGameAndTrainer() {
        val save = GameSave.read(fakeSave(), "POKEMON_HG_IPKE.sav")!!
        fun copy(game: Game, id: String) = GameCopyEntity(game = game.key, label = "", playerName = "", trainerId = id, language = null, platform = "EMULATOR", nationalDex = false, createdAt = 0)
        assertEquals(Game.HEARTGOLD, save.forCopy(copy(Game.HEARTGOLD, "12345"))?.game)
        assertNull(save.forCopy(copy(Game.HEARTGOLD, "54321")))
        assertNull(save.forCopy(copy(Game.SOULSILVER, "12345")))
        assertNull(save.forCopy(copy(Game.HEARTGOLD, "")))
        // A save that can't say which of a pair it is fits either, and takes the copy's game.
        assertEquals(Game.SOULSILVER, save.copy(gameOptions = listOf(Game.HEARTGOLD, Game.SOULSILVER)).forCopy(copy(Game.SOULSILVER, "12345"))?.game)
    }

    @Test
    fun readsDiamondAndPearl() {
        val save = GameSave.read(fakeSinnoh(0xC100, 0x121E0, 0x64, 0x98, 0x12DC, origin = 11))!!
        assertEquals(Game.PEARL, save.game)
        assertEquals("ETHAN", save.trainerName)
        assertEquals(3, save.badges.countOneBits())
        assertEquals(90, save.playMinutes)
        assertEquals(true, save.nationalDex)
        assertEquals("ETHAN", save.boxNames.first())
    }

    @Test
    fun readsPlatinum() {
        val save = GameSave.read(fakeSinnoh(0xCF2C, 0x121E4, 0x68, 0xA0, 0x1328, origin = 12))!!
        assertEquals(Game.PLATINUM, save.game)
        assertEquals(158, save.party.single().speciesId)
    }

    @Test
    fun theNewestCopyOfABlockIsTheOneWithTheHigherFirstCount() {
        val bytes = fakeSinnoh(0xC100, 0x121E0, 0x64, 0x98, 0x12DC, origin = 11)
        fakeSinnoh(0xC100, 0x121E0, 0x64, 0x98, 0x12DC, origin = 11).copyInto(bytes, 0x40000, 0, 0x40000)
        name(bytes, 0x64, "OLD")
        // The first copy has the higher second count, but the second copy has been saved more often.
        sealSinnoh(bytes, 0, 0xC100, major = 1, minor = 9)
        sealSinnoh(bytes, 0x40000, 0xC100, major = 2, minor = 1)
        assertEquals("ETHAN", GameSave.read(bytes)!!.trainerName)
    }

    @Test
    fun japaneseTrainerNamesAreRead() {
        val bytes = fakeSave()
        // ア, イ in Generation IV's own codes.
        putU16(bytes, 0x64, 0x53); putU16(bytes, 0x66, 0x55); putU16(bytes, 0x68, 0xFFFF)
        seal(bytes, 0, 0xF628)
        assertEquals("アイ", GameSave.read(bytes, "POKEMON_HG_IPKE.sav")!!.trainerName)
    }

    /** Diamond's, Pearl's or Platinum's blocks at their sizes, with three badges, 1 h 30 min and the National Pokédex. */
    private fun fakeSinnoh(general: Int, storage: Int, trainerAt: Int, party: Int, dex: Int, origin: Int): ByteArray {
        val b = ByteArray(0x80000)
        name(b, trainerAt, "ETHAN")
        putU16(b, trainerAt + 0x10, (trainer and 0xFFFF).toInt())
        putU16(b, trainerAt + 0x12, (trainer ushr 16).toInt())
        b[trainerAt + 0x1A] = 0b0000_0111
        putU16(b, trainerAt + 0x22, 1)
        b[trainerAt + 0x24] = 30
        putU32(b, party - 4, 1)
        pokemon(b, party, origin)
        putU32(b, dex, 0xBEEFCAFEL)
        b[dex + 0x31B] = 1
        name(b, general + 4 + 18 * 0xFF0, "ETHAN")
        sealSinnoh(b, 0, general, 1, 1)
        sealSinnoh(b, general, storage, 1, 1)
        return b
    }

    /** Writes a Sinnoh block's footer, whose checksum leaves out both its save counts. */
    private fun sealSinnoh(b: ByteArray, at: Int, size: Int, major: Long, minor: Long) {
        putU32(b, at + size - 0x14, major)
        putU32(b, at + size - 0x10, minor)
        putU32(b, at + size - 0x0C, size.toLong())
        putU16(b, at + size - 2, crc16(b, at, size - 0x14))
    }

    /** A HeartGold save with one Totodile in the party, twelve badges, and blocks that pass their checks. */
    private fun fakeSave(started: Long = 559_351_764): ByteArray {
        val b = ByteArray(0x80000)
        putU32(b, 0x34, started)
        name(b, 0x64, "ETHAN")
        putU16(b, 0x74, (trainer and 0xFFFF).toInt())
        putU16(b, 0x76, (trainer ushr 16).toInt())
        b[0x7E] = 0xFF.toByte()
        b[0x83] = 0b0000_1111
        putU32(b, 0x94, 1)
        pokemon(b, 0x98)
        putU32(b, 0x12B8, 0xBEEFCAFEL)
        seal(b, 0, 0xF628)
        seal(b, 0xF700, 0x12310)
        return b
    }

    /** Writes a block's footer: its save count, size and checksum. */
    private fun seal(b: ByteArray, at: Int, size: Int) {
        putU32(b, at + size - 0x10, 1)
        putU32(b, at + size - 0x0C, size.toLong())
        var crc = 0xFFFF
        for (i in at until at + size - 0x10) {
            crc = crc xor ((b[i].toInt() and 0xFF) shl 8)
            repeat(8) { crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1 }
            crc = crc and 0xFFFF
        }
        putU16(b, at + size - 2, crc)
    }

    /** Writes one encrypted Pokémon. A PID of 0 keeps its four parts in A, B, C, D order. */
    private fun pokemon(b: ByteArray, at: Int, origin: Int = 7) {
        val data = ByteArray(128)
        putU16(data, 0, 158)
        putU32(data, 4, trainer)
        data[64 + 0x17] = origin.toByte()
        // Part D starts at 96: the trainer's name, then the Egg's date, then the day it was met.
        name(data, 96, "ETHAN")
        data[96 + 0x13] = 17; data[96 + 0x14] = 9; data[96 + 0x15] = 21
        val sum = (0 until 64).sumOf { u16(data, it * 2) } and 0xFFFF
        putU16(b, at + 6, sum)
        var seed = sum
        for (w in 0 until 64) {
            seed = seed * 0x41C64E6D + 0x6073
            putU16(b, at + 8 + w * 2, u16(data, w * 2) xor (seed ushr 16))
        }
    }

    private fun name(b: ByteArray, at: Int, s: String) {
        s.forEachIndexed { i, c -> putU16(b, at + i * 2, 0x12B + (c - 'A')) }
        putU16(b, at + s.length * 2, 0xFFFF)
    }

    private fun u16(b: ByteArray, at: Int) = (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8)
    private fun putU16(b: ByteArray, at: Int, v: Int) { b[at] = v.toByte(); b[at + 1] = (v shr 8).toByte() }
    private fun putU32(b: ByteArray, at: Int, v: Long) { putU16(b, at, (v and 0xFFFF).toInt()); putU16(b, at + 2, ((v ushr 16) and 0xFFFF).toInt()) }
}
