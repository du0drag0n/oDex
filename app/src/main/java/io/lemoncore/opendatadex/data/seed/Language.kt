package io.lemoncore.opendatadex.data.seed

import kotlinx.serialization.Serializable

/** Pokémon data languages; [code] names the pack in assets/seed/lang (English is built into dex.json). */
enum class DataLanguage(val code: String, val label: String, val locale: String) {
    ENGLISH("en", "English", "en"),
    FRENCH("fr", "Français", "fr"),
    GERMAN("de", "Deutsch", "de"),
    SPANISH("es", "Español", "es"),
    ITALIAN("it", "Italiano", "it"),
    JAPANESE("ja", "日本語", "ja"),
    KOREAN("ko", "한국어", "ko"),
    CHINESE_TRADITIONAL("zh-Hant", "繁體中文", "zh-TW"),
    CHINESE_SIMPLIFIED("zh-Hans", "简体中文", "zh-CN");

    companion object {
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

/** assets/seed/lang/<code>.json: names and in-game text that replace the English ones; missing entries stay English. */
@Serializable
data class SeedLanguage(
    val code: String,
    /** speciesId -> [name, genus]. */
    val species: Map<String, List<String>> = emptyMap(),
    /** pokemonId -> [form name, full name of an alternate form]. */
    val forms: Map<String, List<String>> = emptyMap(),
    /** "<speciesId>-<formKey>" -> cosmetic form name. */
    val cosmetic: Map<String, String> = emptyMap(),
    val moves: Map<String, String> = emptyMap(),
    val abilities: Map<String, String> = emptyMap(),
    val items: Map<String, String> = emptyMap(),
    val types: Map<String, String> = emptyMap(),
    val natures: Map<String, String> = emptyMap(),
    val stats: Map<String, String> = emptyMap(),
    val eggGroups: Map<String, String> = emptyMap(),
    val locations: Map<String, String> = emptyMap(),
    val regions: Map<String, String> = emptyMap(),
    val pokedexes: Map<String, String> = emptyMap(),
    val versions: Map<String, String> = emptyMap(),
    val moveFlavor: Map<String, List<Timeline>> = emptyMap(),
    val abilityFlavor: Map<String, List<Timeline>> = emptyMap(),
    val itemFlavor: Map<String, List<Timeline>> = emptyMap(),
    /** speciesId -> versionId (or "latest") -> text. */
    val flavor: Map<String, Map<String, String>> = emptyMap(),
)

/**
 * This seed with [lang]'s names and text in place of English wherever the language has them.
 *
 * Pokédex text falls back per game: a game with no text in the language keeps its English entry, so older games
 * (which PokéAPI only has in English) still show something. Area details ("south, towards Viridian City") exist only
 * in English, so translated places use the location name alone.
 */
fun Seed.translated(lang: SeedLanguage): Seed {
    fun Map<String, String>.or(id: Int, english: String) = this[id.toString()]?.takeIf { it.isNotBlank() } ?: english
    fun <T> Map<String, List<T>>.orList(id: Int, english: List<T>) = this[id.toString()]?.takeIf { it.isNotEmpty() } ?: english
    return copy(
        versions = versions.map { it.copy(name = lang.versions.or(it.id, it.name)) },
        pokedexes = pokedexes.map { it.copy(name = lang.pokedexes.or(it.id, it.name)) },
        regions = regions.mapValues { (k, v) -> lang.regions[k] ?: v },
        stats = stats.mapValues { (k, v) -> lang.stats[k] ?: v },
        types = types.map { it.copy(name = lang.types.or(it.id, it.name)) },
        eggGroups = eggGroups.mapValues { (k, v) -> lang.eggGroups[k] ?: v },
        pokemon = pokemon.map { p ->
            val names = lang.forms[p.id.toString()]
            val speciesName = lang.species[p.species.toString()]?.firstOrNull()
            p.copy(
                name = when {
                    p.isDefault -> speciesName ?: p.name
                    names != null && names.getOrNull(1).orEmpty().isNotBlank() -> names[1]
                    else -> p.name
                },
                form = names?.getOrNull(0)?.takeIf { it.isNotBlank() } ?: p.form,
            )
        },
        species = species.map { s ->
            val names = lang.species[s.id.toString()]
            val text = lang.flavor[s.id.toString()].orEmpty()
            s.copy(
                name = names?.getOrNull(0)?.takeIf { it.isNotBlank() } ?: s.name,
                genus = names?.getOrNull(1)?.takeIf { it.isNotBlank() } ?: s.genus,
                flavor = s.flavor + text,
                cosmetic = s.cosmetic.map { f -> f.copy(name = lang.cosmetic["${s.id}-${f.key}"] ?: f.name) },
            )
        },
        moves = moves.map { it.copy(name = lang.moves.or(it.id, it.name), flavor = lang.moveFlavor.orList(it.id, it.flavor)) },
        abilities = abilities.map { it.copy(name = lang.abilities.or(it.id, it.name), flavor = lang.abilityFlavor.orList(it.id, it.flavor)) },
        items = items.map { it.copy(name = lang.items.or(it.id, it.name), flavor = lang.itemFlavor.orList(it.id, it.flavor)) },
        natures = natures.map { it.copy(name = lang.natures.or(it.id, it.name)) },
        areas = areas.map { a -> lang.locations[a.location.toString()]?.let { a.copy(name = it, area = it) } ?: a },
        placeNames = placeNames.map { p -> lang.locations[p.id.toString()]?.let { p.copy(name = it) } ?: p },
    )
}
