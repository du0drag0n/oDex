package io.lemoncore.opendatadex.data.seed

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shape of assets/seed/dex.json, written by tools/seedgen/SeedGen.java from openDex's bulk game data.
 * Compact rows are positional arrays; the comment on each says what the positions are.
 *
 * Version groups are compared by [SeedVersionGroup.order] (release order) wherever the seed stores a "vg" that is not an id.
 */
@Serializable
data class Seed(
    val schema: Int,
    val versionGroups: List<SeedVersionGroup>,
    val versions: List<SeedVersion>,
    val pokedexes: List<SeedPokedex>,
    val regions: Map<String, String>,
    val stats: Map<String, String>,
    val types: List<SeedType>,
    /** [attackingType, defendingType, factor%] for every non-neutral pairing today. */
    val efficacy: List<List<Int>>,
    /** [attackingType, defendingType, factor%, lastGeneration]: factor that applied up to and including lastGeneration. */
    val efficacyPast: List<List<Int>>,
    val eggGroups: Map<String, String>,
    val growthRates: List<SeedGrowthRate>,
    val encounterMethods: Map<String, String>,
    val encounterConditions: Map<String, String>,
    val pokemon: List<SeedPokemon>,
    val species: List<SeedSpecies>,
    val evolutions: List<SeedEvolution>,
    /** [pokemonId, versionId, areaId, methodId, minLevel, maxLevel, rarity%, conditionIds...]. */
    val encounters: List<List<Int>>,
    /** [pokemonId, versionId, itemId, rarity%]: items wild Pokémon may be holding. */
    val heldItems: List<List<Int>>,
    val moves: List<SeedMove>,
    val abilities: List<SeedAbility>,
    val items: List<SeedItem>,
    val natures: List<SeedNature>,
    val areas: List<SeedArea>,
    /** [generation id, game index, itemId]: what the item numbers in a save from that generation mean. */
    val saveItems: List<List<Int>> = emptyList(),
    /** [generation id, game index, locationId]: likewise for the places where Pokémon were met. */
    val savePlaces: List<List<Int>> = emptyList(),
    /** The places in [savePlaces], which include many that have no encounters and so no [areas]. */
    val placeNames: List<SeedPlace> = emptyList(),
)

@Serializable
data class SeedPlace(val id: Int, val name: String)

@Serializable
data class SeedVersionGroup(val id: Int, val key: String, val gen: Int, val order: Int)

@Serializable
data class SeedVersion(val id: Int, val vg: Int, val key: String, val name: String)

@Serializable
data class SeedPokedex(val id: Int, val key: String, val region: Int, val name: String)

@Serializable
data class SeedType(val id: Int, val key: String, val name: String, val gen: Int, val dmgClass: Int)

@Serializable
data class SeedGrowthRate(val id: Int, val name: String, val exp100: Int)

/** One Pokémon variety: a species' default form, or an alternate form with its own types, stats or abilities. */
@Serializable
data class SeedPokemon(
    val id: Int,
    val species: Int,
    val key: String,
    /** "Alolan Rattata", "Mega Charizard X"; the species name for default forms. */
    val name: String,
    /** "Alolan Form", "Normal Forme"; empty when the species has no named forms. */
    val form: String,
    /** Release order of the version group that introduced this form. */
    val introduced: Int,
    /** Region of a regional variant (7 Alola, 8 Galar, 9 Hisui, 10 Paldea), else 0. */
    val region: Int,
    val mega: Boolean,
    val gmax: Boolean,
    val battleOnly: Boolean,
    /** Decimetres. */
    val height: Int,
    /** Hectograms. */
    val weight: Int,
    val baseExp: Int,
    val types: List<Int>,
    /** [lastGeneration, slot, typeId]. */
    val typesPast: List<List<Int>>,
    /** HP, Atk, Def, SpA, SpD, Spe. */
    val stats: List<Int>,
    val ev: List<Int>,
    /** [lastGeneration, statId, base, effort]; statId 9 is the Generation I Special stat. */
    val statsPast: List<List<Int>>,
    /** [slot, abilityId, isHidden]. */
    val abilities: List<List<Int>>,
    /** [lastGeneration, slot, abilityId (0 = empty), isHidden]. */
    val abilitiesPast: List<List<Int>>,
) {
    val isDefault get() = id < 10000
}

@Serializable
data class SeedSpecies(
    val id: Int,
    val key: String,
    val name: String,
    val genus: String,
    val gen: Int,
    val color: Int,
    val evolvesFrom: Int,
    val chain: Int,
    val genderRate: Int,
    val capture: Int,
    val happiness: Int,
    val hatch: Int,
    val growth: Int,
    val legendary: Boolean,
    val mythical: Boolean,
    val baby: Boolean,
    /** Male and female look different (sprites exist for both). */
    val genderDiff: Boolean = false,
    /** Pokémon ids of this species' forms; the default form first. */
    val forms: List<Int>,
    /** Looks-only forms that share the default form's data (Unown letters, Vivillon patterns, Arceus plates). */
    val cosmetic: List<SeedCosmeticForm> = emptyList(),
    val eggGroups: List<Int>,
    /** pokedexId -> number. */
    val dex: Map<String, Int>,
    /** versionId -> text, plus "latest". */
    val flavor: Map<String, String>,
)

/** A cosmetic form; [types] is set only where the form changes type (Arceus, Silvally). Sprites are "<speciesId>-<key>". */
@Serializable
data class SeedCosmeticForm(val key: String, val name: String, val introduced: Int, val battleOnly: Boolean, val types: List<Int>)

/** How [species] evolves in games from version group order [vg] on (until a later entry replaces it). */
@Serializable
data class SeedEvolution(
    val species: Int,
    val vg: Int,
    /** Only applies in this region (regional evolutions such as Alolan Raichu), or 0. */
    val region: Int,
    /** The alternate form it evolves into, or 0 for the default form. */
    val toForm: Int,
    val text: String,
)

@Serializable
data class SeedMove(
    val id: Int,
    val key: String,
    val name: String,
    val gen: Int,
    val type: Int,
    val power: Int,
    val pp: Int,
    val acc: Int,
    val priority: Int,
    val dmgClass: Int,
    val short: String,
    val effect: String,
    /** [versionGroupOrder, text]: the in-game description from that version group on. */
    val flavor: List<Timeline>,
    /** [changedInVersionGroupOrder, type (0 unchanged), power, pp, accuracy (-1 unchanged)]: values before that version group. */
    val changes: List<List<Int>>,
)

/** In-game text from version group order [order] on; stored in the seed as a [order, text] array. */
@Serializable(with = TimelineSerializer::class)
data class Timeline(val order: Int, val text: String)

object TimelineSerializer : KSerializer<Timeline> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Timeline")
    override fun deserialize(decoder: Decoder): Timeline {
        val row = (decoder as JsonDecoder).decodeJsonElement().jsonArray
        return Timeline(row[0].jsonPrimitive.int, row[1].jsonPrimitive.content)
    }
    override fun serialize(encoder: Encoder, value: Timeline) =
        (encoder as JsonEncoder).encodeJsonElement(buildJsonArray { add(value.order); add(value.text) })
}

/** The text that applied in the game with release order [order]: the latest entry at or before it, else the earliest. */
fun List<Timeline>.textAt(order: Int): String? = lastOrNull { it.order <= order }?.text ?: firstOrNull()?.text

@Serializable
data class SeedAbility(val id: Int, val name: String, val gen: Int, val short: String, val effect: String, val flavor: List<Timeline>)

@Serializable
data class SeedItem(
    val id: Int,
    val key: String,
    val name: String,
    val cost: Int,
    val category: String,
    val pocket: String,
    val gens: List<Int>,
    val flavor: List<Timeline>,
)

@Serializable
data class SeedNature(val id: Int, val name: String, val up: Int, val down: Int, val likes: String, val hates: String)

@Serializable
data class SeedArea(val id: Int, val location: Int, val name: String, val area: String, val region: Int)

/** One game pair's move pools: assets/seed/learnsets/<versionGroup>.json. */
@Serializable
data class SeedLearnsets(
    val vg: Int,
    /** [machineNumber, moveId]. */
    val machines: List<List<Int>>,
    /** pokemonId -> flat [moveId, method, level, ...]; method 1 level-up, 2 egg, 3 tutor, 4 machine. */
    val pokemon: Map<String, List<Int>>,
)
