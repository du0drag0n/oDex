package io.lemoncore.opendatadex.data.save

import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.seed.Seed
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Gen3SaveTest {
    @Test
    fun readsTrainerPokedexAndPokemonFromTheNewerSlot() {
        val save = FakeSave()
        // The older slot holds a different trainer, which must be ignored.
        save.slot(0, counter = 4, name = "OLD")
        save.slot(1, counter = 5, name = "RED")
        val read = Gen3Save.read(save.bytes, "POKEMON_FIRE_BPRE00.sav")!!
        assertEquals(Game.FIRERED, read.game)
        assertEquals("RED", read.trainerName)
        assertEquals(TID, read.trainerId)
        assertEquals(SID, read.secretId)
        assertEquals(setOf(1, 4, 252), read.owned)
        assertTrue(read.nationalDex)
        assertEquals(0b0101_0111, read.badges)

        val treecko = read.boxes.single()
        assertEquals(252, treecko.speciesId)
        assertEquals(2, treecko.box)
        assertEquals(3, treecko.slot)
        assertEquals("TREE", treecko.nickname)
        assertEquals(listOf(1, 43), treecko.moves)
        // Stored HP, Atk, Def, Spe, SpA, SpD; read back HP, Atk, Def, SpA, SpD, Spe.
        assertEquals(listOf(31, 1, 2, 4, 5, 3), treecko.ivs)
        assertEquals(4, treecko.ball)
        assertTrue(treecko.ownCatch)
        assertFalse(treecko.shiny)
        assertEquals("Box 2", read.boxNames[1])

        assertEquals(1, read.party.single().speciesId)
    }

    @Test
    fun fireRedAndLeafGreenTrustTheEventFlagOverAByteLeftByAnOlderGame() {
        val save = FakeSave()
        save.slot(0, counter = 1, name = "RED", nationalFlag = false)
        assertFalse(Gen3Save.read(save.bytes, "POKEMON_FIRE_BPRE00.sav")!!.nationalDex)
    }

    @Test
    fun eachFamilyKeepsItsBadgesInItsOwnFlags() {
        // Ruby and Sapphire's save has 0 where Emerald's keeps a random security key.
        for (code in listOf(0L, 0x1234ABCDL)) {
            val save = FakeSave()
            save.slot(0, counter = 1, name = "MAY", code = code)
            assertEquals(0b0001_1111, Gen3Save.read(save.bytes, "save.sav")!!.badges)
        }
    }

    @Test
    fun rejectsFilesThatAreNotSaves() {
        assertNull(Gen3Save.read(ByteArray(131072)))
        assertNull(Gen3Save.read(ByteArray(100)))
    }

    @Test
    fun mapsHoennIndexesToTheNationalPokedex() {
        assertEquals(252, Gen3Species.national(277))
        assertEquals(386, Gen3Species.national(410))
        assertEquals(358, Gen3Species.national(411))
        assertNull(Gen3Species.national(260))
        assertNull(Gen3Species.national(0))
    }

    @Test
    fun levelsFollowEachGrowthCurve() {
        assertEquals(100, levelAt(1_250_000, 1_250_000))
        assertEquals(1, levelAt(0, 1_059_860))
        // Medium slow needs 344,960 for level 70 and 360,898 for 71.
        assertEquals(70, levelAt(350_716, 1_059_860))
        assertEquals(70, levelAt(343_000, 1_000_000))
        assertEquals(69, levelAt(342_999, 1_000_000))
        assertEquals(100, levelAt(600_000, 600_000))
        assertEquals(100, levelAt(1_640_000, 1_640_000))
    }

    @Test
    fun genderComesFromThePidsLowByte() {
        assertNull(genderOf(0x12, -1))
        assertEquals(true, genderOf(0x00, 4))
        assertEquals(false, genderOf(0x7F, 4))
        // One in eight female (starters): female only below 31.
        assertEquals(true, genderOf(0x1E, 1))
        assertEquals(false, genderOf(0x1F, 1))
        assertEquals(false, genderOf(0xFF, 0))
    }

    @Test
    fun mapsItemsAndPlacesToOdexData() {
        assertEquals("poke-ball", gen3ItemKey(4))
        assertEquals("leftovers", gen3ItemKey(200))
        assertEquals("x-defense", gen3ItemKey(76))
        assertNull(gen3ItemKey(0))
        assertNull(gen3ItemKey(52))
        assertEquals("Littleroot Town", gen3Place(0)?.name)
        assertEquals("Pallet Town", gen3Place(88)?.name)
        assertNull(gen3Place(MET_TRADE))
        assertNull(gen3Place(MET_FATEFUL))
    }

    /** Prints a summary of every real save in ODEX_SAVES (a folder), to ODEX_REPORT when set; skipped otherwise. */
    @Test
    fun readsRealSaves() {
        val folder = System.getenv("ODEX_SAVES") ?: return
        val dex = Dex(Json { ignoreUnknownKeys = true }.decodeFromString(Seed.serializer(), File("src/main/assets/seed/dex.json").readText()))
        val report = StringBuilder()
        File(folder).listFiles().orEmpty().filter { it.isFile }.sortedBy { it.name }.forEach { file ->
            report.appendLine("=== ${file.name} (${file.length()} bytes)")
            val save = GameSave.read(file.readBytes(), file.name)
            if (save == null) {
                report.appendLine("unreadable")
                return@forEach
            }
            val gen = save.game.dataGen
            report.appendLine("game=${save.game} options=${save.gameOptions} trainer=${save.trainerName} id=${save.trainerId} play=${save.playMinutes / 60}h national=${save.nationalDex} " +
                "badges=${save.badges.countOneBits()}/${save.game.badgeTotal} started=${save.startedAt?.let(::consoleDate)}")
            report.appendLine("owned=${save.owned.size} seen=${save.seen.size} party=${save.party.size} boxed=${save.boxes.size} own=${save.all.count { it.ownCatch }} shiny=${save.all.count { it.shiny }} eggs=${save.all.count { it.egg }}")
            report.appendLine("boxes=${save.boxNames} perBox=${save.boxes.groupingBy { it.box }.eachCount().toSortedMap()}")
            fun placeOf(p: SavedPokemon) = dex.savePlaceName(saveOrigin(p.originGame)?.dataGen?.takeIf { it in 5..gen } ?: gen, p.metLocation)
            if (gen >= 2) {
                val unknownItems = save.all.filter { it.heldItem != 0 && dex.saveItemKey(gen, it.heldItem) == null }.map { it.heldItem }.distinct().sorted()
                val unknownPlaces = save.all.filter { !it.egg && placeOf(it) == null }.map { it.metLocation }.distinct().sorted()
                report.appendLine("unknownItems=$unknownItems unknownPlaces=$unknownPlaces")
            }
            save.all.take(if (gen <= 2) save.all.size else 12).forEach { p ->
                report.appendLine("  box ${p.box} slot ${p.slot}: #${p.speciesId} ${p.nickname} ot=${p.otName} exp=${p.exp} moves=${p.moves} ivs=${p.ivs} evs=${p.evs} ball=${p.ball} " +
                    "origin=${p.originGame} own=${p.ownCatch} shiny=${p.shiny} egg=${p.egg} item=${dex.saveItemKey(gen, p.heldItem) ?: p.heldItem} lang=${p.language} " +
                    "met=${placeOf(p) ?: p.metLocation}@${p.metLevel} on ${metLocalDate(p.metDate)} time=${p.metTime} " +
                    "form=${p.form} gender=${p.gender} nature=${p.nature} ability=${p.abilitySlot}")
            }
        }
        System.getenv("ODEX_REPORT")?.let { File(it).writeText(report.toString()) } ?: println(report)
    }

    private companion object {
        const val TID = 12345
        const val SID = 54321
        val FULL_ID = (SID.toLong() shl 16) or TID.toLong()
    }

    /** Builds a FireRed-shaped save in memory. */
    private class FakeSave {
        val bytes = ByteArray(131072)

        /** A FireRed slot, or Ruby's or Emerald's when [code] says so; badges are set in each family's own event flags. */
        fun slot(index: Int, counter: Int, name: String, code: Long = 1, nationalFlag: Boolean = true) {
            val base = index * 0xE000
            val sections = Array(14) { ByteArray(0xF80) }
            val trainer = sections[0]
            encodeText(name, trainer, 0, 7)
            putU32(trainer, 0x0A, FULL_ID)
            putU32(trainer, 0xAC, code)
            trainer[0x1B] = 0xB9.toByte()
            val player = sections.slice(1..4).map { it }
            fun setFlag(flags: Int, flag: Int) {
                val at = flags + flag / 8
                val section = player[at / 0xF80]
                section[at % 0xF80] = (section[at % 0xF80].toInt() or (1 shl (flag % 8))).toByte()
            }
            when (code) {
                1L -> {
                    listOf(0, 1, 2, 4, 6).forEach { setFlag(0xEE0, 0x820 + it) }
                    if (nationalFlag) setFlag(0xEE0, 0x840)
                }
                0L -> (0 until 5).forEach { setFlag(0x1220, 0x807 + it) }
                else -> (0 until 5).forEach { setFlag(0x1270, 0x867 + it) }
            }
            for (n in listOf(1, 4, 252)) trainer[0x28 + (n - 1) / 8] = (trainer[0x28 + (n - 1) / 8].toInt() or (1 shl ((n - 1) % 8))).toByte()

            putU32(sections[1], 0x34, 1)
            pokemon(sections[1], 0x38, species = 1, nick = "BULBA", moves = listOf(33), ivs = IntArray(6))

            val pc = ByteArray(0xF80 * 9)
            pokemon(pc, 4 + (30 + 3) * 80, species = 277, nick = "TREE", moves = listOf(1, 43), ivs = intArrayOf(31, 1, 2, 3, 4, 5))
            val namesAt = 4 + 420 * 80
            for (b in 0 until 14) encodeText("Box ${b + 1}", pc, namesAt + b * 9, 9)
            for (i in 5..13) pc.copyInto(sections[i], 0, (i - 5) * 0xF80, (i - 4) * 0xF80)

            // Sections are stored rotated, as the game does, to check they are put back in id order.
            for (i in 0 until 14) {
                val id = (i + 3) % 14
                val at = base + i * 0x1000
                sections[id].copyInto(bytes, at)
                putU16(bytes, at + 0xFF4, id)
                putU32(bytes, at + 0xFF8, 0x08012025)
                putU32(bytes, at + 0xFFC, counter.toLong())
            }
        }

        /** Writes one encrypted Pokémon caught in FireRed in a Poké Ball, its parts in the order PID % 24 picks. */
        fun pokemon(b: ByteArray, at: Int, species: Int, nick: String, moves: List<Int>, ivs: IntArray) {
            val pid = 0x1234567L
            putU32(b, at, pid)
            putU32(b, at + 4, FULL_ID)
            encodeText(nick, b, at + 8, 10)
            b[at + 0x12] = 2
            encodeText("RED", b, at + 0x14, 7)
            val growth = ByteArray(12).also { putU16(it, 0, species); putU32(it, 4, 1000) }
            val attacks = ByteArray(12).also { g -> moves.forEachIndexed { i, m -> putU16(g, i * 2, m) } }
            val evs = ByteArray(12)
            val misc = ByteArray(12).also {
                putU16(it, 2, 5 or (4 shl 7) or (4 shl 11))
                putU32(it, 4, ivs.foldIndexed(0L) { i, acc, v -> acc or (v.toLong() shl (5 * i)) })
            }
            val parts = mapOf('G' to growth, 'A' to attacks, 'E' to evs, 'M' to misc)
            val order = listOf(
                "GAEM", "GAME", "GEAM", "GEMA", "GMAE", "GMEA", "AGEM", "AGME", "AEGM", "AEMG", "AMGE", "AMEG",
                "EGAM", "EGMA", "EAGM", "EAMG", "EMGA", "EMAG", "MGAE", "MGEA", "MAGE", "MAEG", "MEGA", "MEAG",
            )[(pid % 24).toInt()]
            val data = ByteArray(48)
            order.forEachIndexed { i, c -> parts.getValue(c).copyInto(data, i * 12) }
            val sum = (0 until 24).sumOf { (data[it * 2].toInt() and 0xFF) or ((data[it * 2 + 1].toInt() and 0xFF) shl 8) } and 0xFFFF
            putU16(b, at + 0x1C, sum)
            val key = pid xor FULL_ID
            for (w in 0 until 12) {
                var word = 0L
                for (k in 0 until 4) word = word or ((data[w * 4 + k].toLong() and 0xFF) shl (8 * k))
                putU32(b, at + 32 + w * 4, word xor key)
            }
        }

        fun encodeText(s: String, b: ByteArray, at: Int, length: Int) {
            for (i in 0 until length) b[at + i] = 0xFF.toByte()
            s.forEachIndexed { i, c ->
                b[at + i] = when (c) {
                    in 'A'..'Z' -> 0xBB + (c - 'A')
                    in 'a'..'z' -> 0xD5 + (c - 'a')
                    in '0'..'9' -> 0xA1 + (c - '0')
                    else -> 0x00
                }.toByte()
            }
        }

        fun putU16(b: ByteArray, at: Int, v: Int) { b[at] = v.toByte(); b[at + 1] = (v shr 8).toByte() }
        fun putU32(b: ByteArray, at: Int, v: Long) { putU16(b, at, (v and 0xFFFF).toInt()); putU16(b, at + 2, ((v ushr 16) and 0xFFFF).toInt()) }
    }
}
