package io.lemoncore.opendatadex.data

import io.lemoncore.opendatadex.data.seed.DataLanguage
import io.lemoncore.opendatadex.data.seed.Seed
import io.lemoncore.opendatadex.data.seed.SeedLanguage
import io.lemoncore.opendatadex.data.seed.translated
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the bundled seed against well-known facts from every generation, so a regenerated seed can't silently regress. */
class DexTest {
    private val dex: Dex by lazy { shared }

    private val ghost = 8
    private val psychic = 14
    private val bug = 7
    private val poison = 4
    private val electric = 13
    private val steel = 9
    private val fairy = 18
    private val normal = 1

    private fun species(id: Int) = dex.speciesOf(id)!!
    private fun default(id: Int) = dex.defaultForm(species(id))

    @Test fun `regional dexes have the right size`() {
        assertEquals(151, dex.pokemonFor(Game.RED).size)
        assertEquals(251, dex.pokemonFor(Game.CRYSTAL).size)
        assertEquals(1025, dex.pokemonFor(Game.NATIONAL).size)
        assertEquals("Chikorita", dex.pokemonFor(Game.GOLD).first().name)
        assertEquals(202, dex.pokemonFor(Game.RUBY).size)
        assertEquals(151, dex.pokemonFor(Game.FIRERED).size)
        assertEquals(151, dex.pokemonFor(Game.DIAMOND).size)
        assertEquals(210, dex.pokemonFor(Game.PLATINUM).size)
        assertEquals(256, dex.pokemonFor(Game.HEARTGOLD).size)
        assertEquals(156, dex.pokemonFor(Game.BLACK).size)
        assertEquals(301, dex.pokemonFor(Game.BLACK_2).size)
        assertEquals(403, dex.pokemonFor(Game.ULTRA_SUN).size)
        assertEquals(153, dex.pokemonFor(Game.LETS_GO_PIKACHU).size)
        assertEquals(151, dex.pokemonFor(Game.BRILLIANT_DIAMOND).size)
        assertEquals(242, dex.pokemonFor(Game.LEGENDS_ARCEUS).size)
        assertEquals("Victini", dex.pokemonFor(Game.BLACK).first().name)
    }

    @Test fun `games with several dexes list each one in order`() {
        val sword = dex.pokemonFor(Game.SWORD)
        assertEquals(listOf(0, 1, 2), sword.map { it.dexIndex }.distinct())
        assertEquals("Grookey", sword.first().name)
        val scarlet = dex.pokemonFor(Game.SCARLET)
        assertEquals(400, scarlet.count { it.dexIndex == 0 })
        assertEquals(listOf(0, 1, 2), scarlet.map { it.dexIndex }.distinct())
        assertEquals(3, dex.pokemonFor(Game.X).map { it.dexIndex }.distinct().size)
    }

    @Test fun `type charts follow the generation`() {
        assertEquals(0, dex.factor(ghost, psychic, 1))
        assertEquals(200, dex.factor(ghost, psychic, 2))
        assertEquals(200, dex.factor(bug, poison, 1))
        assertEquals(50, dex.factor(bug, poison, 2))
        assertEquals(50, dex.factor(ghost, steel, 5))
        assertEquals(100, dex.factor(ghost, steel, 6))
        assertFalse(dex.typesIn(Game.BLACK).any { it.id == fairy })
        assertTrue(dex.typesIn(Game.X).any { it.id == fairy })
    }

    @Test fun `types changed over time`() {
        assertEquals(listOf(electric), dex.typesOf(default(81), Game.RED))
        assertEquals(listOf(electric, steel), dex.typesOf(default(81), Game.GOLD))
        assertEquals(listOf(normal), dex.typesOf(default(35), Game.BLACK)) // Clefairy before Fairy existed
        assertEquals(listOf(fairy), dex.typesOf(default(35), Game.X))
    }

    @Test fun `generation one uses the single Special stat`() {
        val red = dex.baseStats(default(6), Game.RED)
        assertEquals(5, red.size)
        assertEquals(85, red.first { it.statId == 9 }.base)
        assertEquals(6, dex.baseStats(default(6), Game.CRYSTAL).size)
    }

    @Test fun `moves follow the selected game's rules`() {
        val bite = dex.move(44, Game.RED)!!
        assertEquals(1, bite.type) // Normal until Generation II
        assertEquals(17, dex.move(44, Game.GOLD)!!.type)
        assertEquals(2, bite.category) // physical because Normal is a physical type
        assertEquals(3, dex.move(52, Game.RED)!!.category)
        // Fire Punch: special while category came from type, physical from Generation IV.
        assertEquals(3, dex.move(7, Game.RUBY)!!.category)
        assertEquals(2, dex.move(7, Game.DIAMOND)!!.category)
    }

    @Test fun `level-up moves and machines resolve per game`() {
        val bulbasaur = default(1)
        val red = dex.learnsets(Game.RED)
        assertTrue(dex.movesLearned(bulbasaur, Game.RED, LearnMethod.LEVEL_UP, red).any { it.move.name == "Vine Whip" && it.level == 13 })
        assertTrue(dex.movesLearned(bulbasaur, Game.RED, LearnMethod.MACHINE, red).any { it.machine == "HM01" && it.move.name == "Cut" })
        val diamond = dex.learnsets(Game.DIAMOND)
        assertTrue(dex.movesLearned(bulbasaur, Game.DIAMOND, LearnMethod.MACHINE, diamond).any { it.machine == "HM01" })
        val sword = dex.learnsets(Game.SWORD)
        assertTrue(dex.movesLearned(default(25), Game.SWORD, LearnMethod.MACHINE, sword).any { it.machine!!.startsWith("TR") })
        val scarlet = dex.learnsets(Game.SCARLET)
        val pikachu = dex.movesLearned(default(25), Game.SCARLET, LearnMethod.MACHINE, scarlet)
        assertTrue(pikachu.isNotEmpty() && pikachu.all { it.machine!!.matches(Regex("TM\\d{3}")) })
        assertTrue(dex.movesLearned(default(25), Game.SCARLET, LearnMethod.LEVEL_UP, scarlet).any { it.move.name == "Thunderbolt" })
    }

    @Test fun `every game with move data has its pack`() {
        Game.playable.filter { it != Game.LEGENDS_ZA }.forEach { game ->
            assertTrue("no move pools for ${game.key}", dex.learnsets(game).pokemonIds.isNotEmpty())
        }
    }

    @Test fun `abilities follow the generation`() {
        assertEquals(listOf("Levitate"), dex.abilitiesOf(default(94), Game.X).map { it.ability.name })
        assertEquals(listOf("Cursed Body"), dex.abilitiesOf(default(94), Game.SUN).map { it.ability.name })
        assertEquals(listOf("Overgrow"), dex.abilitiesOf(default(1), Game.RUBY).map { it.ability.name })
        assertTrue(dex.abilitiesOf(default(1), Game.BLACK).any { it.hidden && it.ability.name == "Chlorophyll" })
    }

    @Test fun `regional forms show in their region`() {
        val raichu = dex.pokemonFor(Game.SUN).first { it.id == 26 }
        assertEquals("Alolan Raichu", raichu.name)
        assertEquals(listOf(electric, psychic), raichu.types)
        assertEquals(26, dex.pokemonFor(Game.RED).first { it.id == 26 }.formId)
        assertTrue(dex.pokemonFor(Game.LEGENDS_ARCEUS).first { it.id == 157 }.name.startsWith("Hisuian"))
    }

    @Test fun `forms exist only where the game has them`() {
        val charizard = species(6)
        assertTrue(dex.formsIn(charizard, Game.X).any { it.key == "charizard-mega-x" })
        assertFalse(dex.formsIn(charizard, Game.X).any { it.gmax })
        assertTrue(dex.formsIn(charizard, Game.SWORD).any { it.gmax })
        assertFalse(dex.formsIn(charizard, Game.SWORD).any { it.mega })
        assertEquals(1, dex.formsIn(charizard, Game.RED).size)
        assertEquals(1, dex.formsIn(charizard, Game.SCARLET).size)
    }

    @Test fun `evolution methods follow the game`() {
        assertEquals(5, dex.evolutionTree(species(133), Game.CRYSTAL)!!.next.size)
        assertEquals(3, dex.evolutionTree(species(133), Game.RED)!!.next.size)
        assertEquals(8, dex.evolutionTree(species(133), Game.SWORD)!!.next.size)
        assertEquals("Level up at Mt. Coronet", dex.evolutionMethod(species(462), Game.DIAMOND))
        assertEquals("Thunder Stone", dex.evolutionMethod(species(462), Game.SWORD))
        assertTrue(dex.evolutionMethod(species(26), Game.SUN)!!.contains("Thunder Stone"))
        assertEquals("Level 36", dex.evolutionMethod(species(6), Game.RED))
    }

    @Test fun `wild encounters exist across generations`() {
        assertTrue(dex.encountersOf(species(16), Game.RED).isNotEmpty())
        assertTrue(dex.encountersOf(species(6), Game.RED).isEmpty())
        assertTrue(dex.encountersOf(species(263), Game.RUBY).isNotEmpty()) // Zigzagoon
        assertTrue(dex.encountersOf(species(819), Game.SWORD).isNotEmpty()) // Skwovet
    }

    @Test fun `the Sinnoh remakes show the original's wild Pokemon`() {
        assertEquals(Game.DIAMOND, dex.encountersBorrowedFrom(Game.BRILLIANT_DIAMOND))
        assertEquals(null, dex.encountersBorrowedFrom(Game.DIAMOND))
        assertTrue(dex.encountersOf(species(403), Game.BRILLIANT_DIAMOND).isNotEmpty()) // Shinx
        // Starly's Route 201 row applies with or without a GBA game; Growlithe's only with FireRed inserted.
        val route201 = dex.encountersByLocation(Game.BRILLIANT_DIAMOND).getValue(31).map { it.speciesId }
        assertTrue(396 in route201 && 58 !in route201)
        assertTrue(dex.encountersOf(species(434), Game.BRILLIANT_DIAMOND).isNotEmpty()) // Stunky, Diamond's
        assertTrue(dex.encountersOf(species(434), Game.SHINING_PEARL).isEmpty())
        val bd = dex.areasIn(Game.BRILLIANT_DIAMOND).flatMap { (a, _) -> dex.encountersAt(a.id, Game.BRILLIANT_DIAMOND) }
        assertTrue(bd.none { e -> e.conditions.any { "slot 2" in it } })
        assertTrue(dex.encountersOf(species(490), Game.BRILLIANT_DIAMOND).isEmpty()) // Manaphy came from Pokémon Ranger
        assertTrue(dex.encountersOf(species(490), Game.DIAMOND).isNotEmpty())
    }

    @Test fun `pokedex text falls back sensibly`() {
        assertEquals("Diamond", dex.flavorText(species(1), Game.BRILLIANT_DIAMOND)!!.second)
        assertEquals("Ruby", dex.flavorText(species(252), Game.RUBY)!!.second)
        assertNotNull(dex.flavorText(species(1), Game.LEGENDS_ZA))
    }

    @Test fun `game enum matches the seed`() {
        val text = File("src/main/assets/seed/dex.json").readText()
        val seed = Json { ignoreUnknownKeys = true }.decodeFromString(Seed.serializer(), text)
        Game.playable.forEach { g ->
            val vg = seed.versionGroups.first { it.id == g.versionGroup }
            assertEquals("order of ${g.key}", vg.order, g.order)
            assertEquals("generation of ${g.key}", vg.gen, g.dataGen)
            assertEquals("version group of ${g.key}", g.versionGroup, seed.versions.first { it.id == g.versionId }.vg)
        }
    }

    @Test fun `evolution dex and team builder queries`() {
        val red = dex.evolutionFamilies(Game.RED)
        assertEquals(1, red.first().pokemon.id) // Bulbasaur's family first
        assertTrue(red.none { f -> f.pokemon.id == 133 && f.next.size != 3 })
        val moves = dex.learnableMoves(default(25), Game.SCARLET, dex.learnsets(Game.SCARLET))
        assertTrue(moves.any { it.name == "Thunderbolt" } && moves.map { it.id }.distinct().size == moves.size)
        assertEquals(400, dex.factorAgainst(5, listOf(13, 9), Game.SCARLET)) // Ground vs Electric/Steel
    }

    @Test fun `polish pass behaviour`() {
        assertEquals("Level 18", dex.evolutionMethod(species(916), Game.SCARLET)) // Oinkologne, both genders
        assertEquals(25, dex.cosmeticFormsIn(species(201), Game.GOLD).size) // Unown B–Z
        assertEquals(27, dex.cosmeticFormsIn(species(201), Game.RUBY).size) // plus ! and ?
        assertTrue(dex.cosmeticFormsIn(species(493), Game.NATIONAL).any { it.types == listOf(fairy) })
        assertTrue(species(3).genderDiff)
        try {
            dex.formOptions = FormOptions(regionalForms = false, battleForms = false)
            assertEquals(26, dex.pokemonFor(Game.SUN).first { it.id == 26 }.formId)
            assertEquals(1, dex.formsIn(species(6), Game.X).size)
        } finally {
            dex.formOptions = FormOptions()
        }
    }

    @Test fun `catch plan explains how to get each Pokemon`() {
        val red = dex.catchPlan(Game.RED)
        assertEquals(151, red.size)
        assertEquals(ObtainKind.GIFT, red.getValue(1).kind) // Bulbasaur, the starter
        assertEquals(ObtainKind.WILD, red.getValue(16).kind) // Pidgey
        assertEquals(ObtainKind.EVOLVE, red.getValue(2).kind)
        assertEquals("Bulbasaur", red.getValue(2).from)
        assertEquals("Level 16", red.getValue(2).how)
        assertEquals(ObtainKind.EVENT, red.getValue(151).kind) // Mew: only ever given out at events
        val blue = dex.catchPlan(Game.BLUE)
        assertEquals(ObtainKind.OTHER_VERSION, blue.getValue(23).kind) // Ekans is a Red exclusive
        assertEquals(Game.RED, blue.getValue(23).otherGame)
        assertEquals(ObtainKind.OTHER_VERSION, blue.getValue(24).kind) // Arbok is wild in Red too
        assertEquals(ObtainKind.BREED, dex.catchPlan(Game.GOLD).getValue(172).kind) // Pichu
        assertTrue(dex.catchPlan(Game.SCARLET).values.none { it.kind == ObtainKind.WILD })
    }

    @Test fun `games that unlock a National Pokedex list it in national order`() {
        assertEquals(386, dex.pokemonFor(Game.EMERALD, national = true).size)
        assertEquals(493, dex.pokemonFor(Game.HEARTGOLD, national = true).size)
        assertEquals(649, dex.pokemonFor(Game.WHITE_2, national = true).size)
        assertEquals(721, dex.pokemonFor(Game.ALPHA_SAPPHIRE, national = true).size)
        assertEquals(493, dex.pokemonFor(Game.SHINING_PEARL, national = true).size)
        val emerald = dex.pokemonFor(Game.EMERALD, national = true)
        assertEquals(1, emerald.first().id)
        assertEquals(4, emerald.first { it.id == 4 }.dexNumber)
        // Games without one keep their regional dex.
        assertEquals(dex.pokemonFor(Game.SUN).size, dex.pokemonFor(Game.SUN, national = true).size)
        assertEquals(202, dex.pokemonFor(Game.RUBY).size)
    }

    @Test fun `a National Pokedex plan brings in Pokemon from other games`() {
        val emerald = dex.catchPlan(Game.EMERALD, national = true)
        assertEquals(386, emerald.size)
        assertEquals(ObtainKind.OTHER_GAME, emerald.getValue(4).kind) // Charmander: FireRed/LeafGreen starter
        assertTrue(emerald.getValue(4).otherGame in setOf(Game.FIRERED, Game.LEAFGREEN))
        assertEquals(ObtainKind.EVOLVE, emerald.getValue(5).kind)
        assertEquals(ObtainKind.WILD, emerald.getValue(263).kind) // Zigzagoon is still caught in Hoenn
    }

    private fun copy(id: Long, game: Game, language: String? = "en", platform: GamePlatform = GamePlatform.CARTRIDGE, national: Boolean = false) =
        io.lemoncore.opendatadex.data.user.GameCopyEntity(id, game.key, "", "", "", language, platform.name, national, 0)

    @Test fun `games link the way the real ones do`() {
        val emerald = copy(1, Game.EMERALD)
        assertEquals(LinkKind.TRADE, linkBetween(copy(2, Game.FIRERED, "ja"), emerald)) // Generation III trades across languages
        assertEquals(LinkKind.PAL_PARK, linkBetween(emerald, copy(3, Game.PLATINUM)))
        assertEquals(null, linkBetween(copy(3, Game.PLATINUM), emerald)) // Pal Park only goes forward
        assertEquals(LinkKind.POKE_TRANSFER, linkBetween(copy(4, Game.HEARTGOLD), copy(5, Game.BLACK_2)))
        assertEquals(LinkKind.POKE_BANK, linkBetween(copy(6, Game.X), copy(7, Game.ULTRA_MOON)))
        assertEquals(null, linkBetween(copy(8, Game.RED, "ja"), copy(9, Game.BLUE, "en"))) // Japanese Gen I carts can't link abroad
        assertEquals(LinkKind.POKE_BANK, linkBetween(copy(10, Game.CRYSTAL, platform = GamePlatform.DIGITAL), copy(11, Game.SUN)))
        assertEquals(null, linkBetween(copy(12, Game.RUBY, platform = GamePlatform.EMULATOR), emerald))
        assertEquals(20, Game.GOLD.boxSize)
        assertEquals(30, Game.EMERALD.boxSize)
    }

    @Test fun `trade planner finds what your other games can send`() {
        val emerald = copy(1, Game.EMERALD, national = true)
        val fireRed = copy(2, Game.FIRERED)
        val plan = dex.tradePlan(emerald, listOf(emerald, fireRed), mapOf(fireRed.key to setOf(1)))
        val fromFireRed = plan.groups.single { it.copy.id == 2L }
        assertEquals(LinkKind.TRADE, fromFireRed.link)
        val bulbasaur = fromFireRed.pokemon.first() // caught ones come first
        assertEquals(1, bulbasaur.first.id)
        assertTrue(bulbasaur.second.caught)
        assertTrue(fromFireRed.pokemon.any { it.first.id == 4 && !it.second.caught }) // Charmander: FireRed's starter
        assertTrue(plan.tradeEvolutions.any { it.pokemon.id == 65 && it.partners == listOf(fireRed) }) // Alakazam
        assertTrue(dex.evolvesByTrade(dex.speciesOf(94)!!, Game.EMERALD)) // Gengar
        assertFalse(dex.evolvesByTrade(dex.speciesOf(2)!!, Game.EMERALD)) // Ivysaur levels up
    }

    @Test fun `event-only Pokemon are told apart from catchable ones`() {
        val emerald = dex.catchPlan(Game.EMERALD, national = true)
        for (id in listOf(151, 249, 385, 386)) assertEquals("species $id", ObtainKind.EVENT, emerald.getValue(id).kind) // Mew, Lugia, Jirachi, Deoxys
        assertEquals("Faraway Island", emerald.getValue(151).place)
        assertEquals(ObtainKind.WILD, emerald.getValue(381).kind) // Latios roams Hoenn in Emerald
        assertEquals(ObtainKind.EVENT, dex.catchPlan(Game.FIRERED, national = true).getValue(386).kind) // not "LeafGreen exclusive"
        assertEquals(ObtainKind.OTHER_VERSION, dex.catchPlan(Game.RUBY, national = true).getValue(380).kind) // Latias roams Sapphire
        assertEquals(ObtainKind.EVENT, dex.catchPlan(Game.RED).getValue(151).kind)
        assertTrue(386 !in dex.eventOnly(Game.OMEGA_RUBY, national = true)) // Deoxys waits at Sky Pillar in the story
        assertTrue(151 in dex.eventOnly(Game.EMERALD, national = true))
    }

    @Test fun `evolution kinds and the extra Pokedex filters`() {
        fun kinds(id: Int) = dex.evolutionKinds(dex.speciesOf(id)!!, Game.EMERALD)
        assertEquals(setOf(EvolutionKind.TRADE), kinds(65)) // Alakazam
        assertEquals(setOf(EvolutionKind.ITEM), kinds(38)) // Ninetales: Fire Stone
        assertEquals(setOf(EvolutionKind.LEVEL), kinds(5)) // Charmeleon
        assertEquals(setOf(EvolutionKind.OTHER), kinds(292)) // Shedinja
        assertTrue(dex.evolutionFamilies(Game.EMERALD).any { EvolutionKind.TRADE in it.kinds })
        val dratini = dex.summary(147, Game.EMERALD)!!
        assertTrue(dex.inEggGroup(dratini, 14)) // Dragon
        assertTrue(dex.hasAbility(dex.summary(94, Game.EMERALD)!!, 26, Game.EMERALD)) // Gengar: Levitate
        assertTrue(dex.learnsMove(dex.summary(131, Game.EMERALD)!!, 57, dex.learnsets(Game.EMERALD))) // Lapras: Surf
        assertFalse(dex.learnsMove(dex.summary(1, Game.EMERALD)!!, 57, dex.learnsets(Game.EMERALD))) // Bulbasaur can't
    }

    @Test fun `shiny odds follow each generation`() {
        assertEquals(8192, shinyOdds(Game.EMERALD, HuntMethod.ENCOUNTERS, charm = false).oneIn)
        assertEquals(8192, shinyOdds(Game.EMERALD, HuntMethod.MASUDA, charm = true).oneIn) // neither existed yet
        assertEquals(1639, shinyOdds(Game.PLATINUM, HuntMethod.MASUDA, charm = false).oneIn) // 5 rolls of 8,192
        assertEquals(1024, shinyOdds(Game.BLACK_2, HuntMethod.MASUDA, charm = true).oneIn) // 8 rolls of 8,192
        assertEquals(8192, shinyOdds(Game.BLACK, HuntMethod.ENCOUNTERS, charm = true).oneIn) // no Shiny Charm in Black
        assertEquals(4096, shinyOdds(Game.X, HuntMethod.SOFT_RESETS, charm = false).oneIn)
        assertEquals(512, shinyOdds(Game.SUN, HuntMethod.MASUDA, charm = true).oneIn) // 8 rolls of 4,096
        assertEquals(0.632, shinyOdds(Game.EMERALD, HuntMethod.ENCOUNTERS, false).foundBy(8192), 0.001)
        assertFalse(Game.YELLOW.hasShinies)
        assertTrue(Game.GOLD.hasShinies)
    }

    @Test fun `sprite sets group games that share sprites`() {
        val sets = Images.spriteSets
        assertTrue(sets.any { it == listOf(Game.RUBY, Game.SAPPHIRE) })
        assertTrue(sets.any { it == listOf(Game.BLACK, Game.WHITE, Game.BLACK_2, Game.WHITE_2) })
        assertTrue(sets.none { Game.NATIONAL in it })
        assertEquals(null, Images.gameSprite(25, Game.RED, shiny = true)) // no shinies in Red
        assertTrue(Images.gameSprite(25, Game.EMERALD, shiny = true)!!.endsWith("generation-iii/emerald/shiny/25.png"))
    }

    @Test fun `data languages replace names and fall back to English`() {
        val root = File("src/main/assets/seed")
        val json = Json { ignoreUnknownKeys = true }
        val seed = json.decodeFromString(Seed.serializer(), root.resolve("dex.json").readText())
        fun inLanguage(code: String) = Dex(seed.translated(json.decodeFromString(SeedLanguage.serializer(), root.resolve("lang/$code.json").readText())))
        DataLanguage.entries.filter { it != DataLanguage.ENGLISH }.forEach { assertTrue("missing pack ${it.code}", root.resolve("lang/${it.code}.json").exists()) }

        val fr = inLanguage("fr")
        assertEquals("Bulbizarre", fr.speciesOf(1)!!.name)
        assertEquals("Charge", fr.move(33, Game.RED)!!.name) // Tackle
        assertEquals("Red", Dex(seed).flavorText(fr.speciesOf(1)!!, Game.RED)!!.second)
        assertTrue(fr.flavorText(fr.speciesOf(1)!!, Game.X)!!.first.contains("graine")) // French from X/Y on
        assertTrue(fr.pokemonFor(Game.SUN).first { it.id == 26 }.name.contains("Alola"))
        assertEquals("ピカチュウ", inLanguage("ja").speciesOf(25)!!.name)
        assertEquals("Bulbasaur", Dex(seed).speciesOf(1)!!.name) // the English seed itself is untouched
    }

    @Test fun `stat range formula`() {
        assertEquals(Pair(266, 359), Dex.statRange(isHp = true, base = 78, gen = 2))
        // Level 100, base 100: (205 × 0.9) with nothing invested, (299 × 1.1) fully trained with a helpful nature.
        assertEquals(Pair(184, 328), Dex.statRange(isHp = false, base = 100, gen = 8))
    }

    @Test fun `machine labels match each game`() {
        assertEquals("HM01", Dex.machineLabel(101, 1))
        assertEquals("TM100", Dex.machineLabel(100, 17))
        assertEquals("TR05", Dex.machineLabel(105, 20))
        assertEquals("TM00", Dex.machineLabel(0, 20))
        assertEquals("TM171", Dex.machineLabel(171, 25))
    }

    companion object {
        private val shared: Dex by lazy {
            val root = File("src/main/assets/seed")
            val seed = Json { ignoreUnknownKeys = true }.decodeFromString(Seed.serializer(), root.resolve("dex.json").readText())
            Dex(seed) { vg -> root.resolve("learnsets/$vg.json").takeIf { it.exists() }?.readText() }
        }
    }
}
