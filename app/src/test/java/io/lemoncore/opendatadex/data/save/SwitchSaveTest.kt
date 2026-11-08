package io.lemoncore.opendatadex.data.save

import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.user.BackupFile
import io.lemoncore.opendatadex.data.user.BackupGameCopy
import io.lemoncore.opendatadex.data.user.SaveImportEntity
import io.lemoncore.opendatadex.data.user.SavePokemonEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwitchSaveTest {
    private val fullId = 1_234_567_890L

    @Test
    fun readsLetsGo() {
        val b = fakeLetsGo()
        val save = GameSave.read(b)!!
        assertEquals(Game.LETS_GO_PIKACHU, save.game)
        assertEquals("Josh", save.trainerName)
        assertEquals(567_890, save.trainerId)
        assertEquals(117 * 60 + 55, save.playMinutes)
        assertEquals(setOf(1, 809), save.owned)
        // The party is slots in the storage, so its Pokémon aren't in the boxes as well.
        assertEquals(listOf(809), save.party.map { it.speciesId })
        assertEquals(listOf(1), save.boxes.map { it.speciesId })
        assertEquals(1, save.boxes.single().box)
        assertEquals(GameSave.UNKNOWN_BADGES, save.badges)
    }

    @Test
    fun aMegabyteFileWithNoBlockTableIsNoSave() {
        assertNull(GameSave.read(ByteArray(0x100000)))
    }

    @Test
    fun readsBrilliantDiamond() {
        val save = GameSave.read(fakeBdsp(), "SaveData.bin")!!
        assertEquals(Game.BRILLIANT_DIAMOND, save.game)
        assertEquals("Ikura", save.trainerName)
        assertEquals(567_890, save.trainerId)
        assertEquals(45 * 60 + 35, save.playMinutes)
        assertEquals(255, save.badges)
        assertEquals("Box 1", save.boxNames.first())
        assertEquals(40, save.boxNames.size)
        assertEquals(setOf(445), save.owned)
        assertEquals(setOf(445, 446), save.seen)
        val mon = save.party.single()
        assertEquals(445, mon.speciesId)
        assertTrue(mon.ownCatch)
        assertEquals(1, mon.gender)
        assertEquals(2, mon.form)
        assertEquals(12, mon.nature)
        assertEquals(HIDDEN_ABILITY_SLOT, mon.abilitySlot)
        assertEquals(20211229, mon.metDate)
        assertEquals(Game.BRILLIANT_DIAMOND, saveOrigin(mon.originGame))
        assertEquals(listOf(47, 83, 87, 80, 66, 147), mon.evs)
        assertEquals(3, save.boxes.single().box)
    }

    @Test
    fun shiningPearlIsToldApartByWhereTheTrainerCaughtThings() {
        assertEquals(Game.SHINING_PEARL, GameSave.read(fakeBdsp(version = 49))!!.game)
    }

    @Test
    fun aFileThatSizeWithNoTrainerIsNoSave() {
        assertNull(GameSave.read(ByteArray(0xEDC20)))
    }

    @Test
    fun unownLettersComeFromTheirDvsOrPid() {
        assertEquals(0, gen2UnownForm(0, 0, 0, 0))
        assertEquals(25, gen2UnownForm(15, 15, 15, 15))
        assertEquals(0, gen3UnownForm(0))
        assertEquals(255 % 28, gen3UnownForm(0x03030303))
        assertFalse(gen3UnownForm(0xFFFFFFFFL) > 27)
    }

    @Test
    fun aBackupCarriesACopysImportAndOldBackupsStillRead() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val mon = SavePokemonEntity(copyId = 3, box = 1, slot = 0, speciesId = 445, nickname = "Gabite", otName = "Ikura", otId = 1, ownCatch = true, pid = 2,
            exp = 3, heldItem = 0, friendship = 70, moves = "89", ivs = "1,2,3,4,5,6", evs = "0,0,0,0,0,0", metLevel = 5, metLocation = 9, originGame = 48,
            ball = 4, egg = false, abilitySlot = 2, shiny = false, language = 2, form = 2, gender = 1, nature = 12)
        val file = BackupFile(exportedAt = 1, gameCopies = listOf(BackupGameCopy("brilliant-diamond", saveImport = SaveImportEntity(3, 5, "SaveData.bin", 60, "Box 1"), savedPokemon = listOf(mon))))
        val back = json.decodeFromString(BackupFile.serializer(), json.encodeToString(BackupFile.serializer(), file))
        assertEquals(mon, back.gameCopies.single().savedPokemon.single())
        assertEquals("SaveData.bin", back.gameCopies.single().saveImport?.fileName)

        val old = """{"app":"oDex","version":1,"exportedAt":1,"gameCopies":[{"game":"red","caught":[1]}]}"""
        val read = json.decodeFromString(BackupFile.serializer(), old).gameCopies.single()
        assertNull(read.saveImport)
        assertTrue(read.savedPokemon.isEmpty())
    }

    /** A Let's Go save with two Pokémon in the storage, the second of them in the party. */
    private fun fakeLetsGo(): ByteArray {
        val b = ByteArray(0x100000)
        putU32(b, 0xB8610, 0x42454546)
        putU32(b, 0x1000, fullId)
        b[0x1004] = 42
        utf16(b, 0x1038, "Josh")
        flag(b, 0x2A88, 1); flag(b, 0x2A88, 809)
        for (i in 0 until 6) putU16(b, 0x5A00 + i * 2, if (i == 0) 1 else 1001)
        pk7(b, 0x5C00, 1)
        pk7(b, 0x5C00 + 260, 809)
        putU16(b, 0x45400, 117)
        b[0x45402] = 55
        return b
    }

    private fun pk7(b: ByteArray, at: Int, species: Int) {
        val data = ByteArray(224)
        putU16(data, 0, species)
        putU32(data, 0x0C - 8, fullId)
        utf16(data, 0xB0 - 8, "Josh")
        data[0xDF - 8] = 42
        val sum = (0 until 112).sumOf { u16(data, it * 2) } and 0xFFFF
        putU16(b, at + 6, sum)
        encrypt(data, b, at + 8)
    }

    /** A Brilliant Diamond save with a Garchomp in the party, another in the third box and one species seen but not caught. */
    private fun fakeBdsp(version: Int = 48): ByteArray {
        val b = ByteArray(0xEDC20)
        utf16(b, 0x79BB4, "Ikura")
        putU32(b, 0x79BD0, fullId)
        b[0x79BDD] = 8
        putU16(b, 0x79C04, 45)
        b[0x79C06] = 35
        utf16(b, 0x148AA, "Box 1")
        putU32(b, 0x7A324 + 444 * 4, 3)
        putU32(b, 0x7A324 + 445 * 4, 2)
        pb8(b, 0x14098, version)
        pb8(b, 0x14EF4 + 60 * 344, version)
        return b
    }

    private fun pb8(b: ByteArray, at: Int, version: Int) {
        val data = ByteArray(320)
        fun put(offset: Int, value: Int) { data[offset - 8] = value.toByte() }
        putU16(data, 0, 445)
        putU32(data, 0x0C - 8, fullId)
        put(0x16, 4)
        put(0x20, 12)
        put(0x22, 1 shl 2)
        put(0x24, 2)
        listOf(47, 83, 87, 147, 80, 66).forEachIndexed { i, ev -> put(0x26 + i, ev) }
        put(0xDE, version)
        utf16(data, 0xF8 - 8, "Ikura")
        put(0x11C, 21); put(0x11D, 12); put(0x11E, 29)
        val sum = (0 until 160).sumOf { u16(data, it * 2) } and 0xFFFF
        putU16(b, at + 6, sum)
        encrypt(data, b, at + 8)
    }

    /** With an encryption key of 0, a Pokémon's parts stay in A, B, C, D order. */
    private fun encrypt(data: ByteArray, b: ByteArray, at: Int) {
        var state = 0
        for (w in 0 until data.size / 2) {
            state = state * 0x41C64E6D + 0x6073
            putU16(b, at + w * 2, u16(data, w * 2) xor (state ushr 16))
        }
    }

    private fun flag(b: ByteArray, at: Int, species: Int) {
        val i = at + (species - 1) / 8
        b[i] = (b[i].toInt() or (1 shl ((species - 1) % 8))).toByte()
    }

    private fun utf16(b: ByteArray, at: Int, s: String) = s.forEachIndexed { i, c -> putU16(b, at + i * 2, c.code) }
    private fun u16(b: ByteArray, at: Int) = (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8)
    private fun putU16(b: ByteArray, at: Int, v: Int) { b[at] = v.toByte(); b[at + 1] = (v shr 8).toByte() }
    private fun putU32(b: ByteArray, at: Int, v: Long) { putU16(b, at, (v and 0xFFFF).toInt()); putU16(b, at + 2, ((v ushr 16) and 0xFFFF).toInt()) }
}
