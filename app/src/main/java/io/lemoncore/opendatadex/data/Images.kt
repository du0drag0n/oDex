package io.lemoncore.opendatadex.data

/**
 * Image and sound locations. openDex species artwork is preferred when it has been synced; everything else comes
 * from openDex's game media, which keeps the paths of PokéAPI's sprite and cry repositories. A file it doesn't hold
 * is a 404, so a sprite set with nothing for a Pokémon still drops out when its image fails. Go by what a file is
 * rather than its extension: official artwork and HOME renders are WebP behind .png paths.
 *
 * Ids here are Pokémon (form) ids: the species id for a default form, 10000+ for alternate forms.
 */
object Images {
    private const val SPRITES = "https://api.opendex.lemoncore.io/v1/game-media/sprites"
    private const val CRIES = "https://api.opendex.lemoncore.io/v1/game-media/cries/pokemon"

    /** Highest id with a pre-Generation VI ("legacy") cry. */
    private const val LAST_LEGACY_CRY = 649

    /** The trainer's choice of Pokémon pictures (Settings); set by AppContainer as settings load. */
    @Volatile var style = ImageStyle.ARTWORK

    /** A Pokémon's picture in the chosen [style]. */
    fun artwork(id: Int, openDex: Map<Int, String>) = when (style) {
        ImageStyle.ARTWORK -> officialArtwork(id, openDex)
        ImageStyle.HOME -> homeSprite(id)
        ImageStyle.PIXEL -> pixelSprite(id)
    }

    fun officialArtwork(id: Int, openDex: Map<Int, String>) = openDex[id] ?: "$SPRITES/pokemon/other/official-artwork/$id.png"

    /** The classic front sprite, drawn without smoothing. */
    fun pixelSprite(id: Int) = "$SPRITES/pokemon/$id.png"

    /** Pixel art is scaled up with hard edges rather than blurred. */
    fun isPixelArt(url: String) = url.startsWith("$SPRITES/pokemon/") && !url.contains("/other/")

    fun shinyArtwork(id: Int) = "$SPRITES/pokemon/other/official-artwork/shiny/$id.png"

    /** Pokémon HOME render: every species and form has one, so it backs up the per-game sprites. */
    fun homeSprite(id: Int, shiny: Boolean = false, female: Boolean = false) =
        "$SPRITES/pokemon/other/home/" + (if (shiny) "shiny/" else "") + (if (female) "female/" else "") + "$id.png"

    /** HOME render of a cosmetic form ("201-b" is Unown B), or its classic sprite. */
    fun cosmeticForm(speciesId: Int, key: String, shiny: Boolean = false) =
        "$SPRITES/pokemon/other/home/" + (if (shiny) "shiny/" else "") + "$speciesId-$key.png"

    fun cosmeticFormSprite(speciesId: Int, key: String) = "$SPRITES/pokemon/$speciesId-$key.png"

    /**
     * The in-game sprite for [game], or null when the game has none (Red/Blue/Yellow had no shinies).
     * [female] picks the female sprite of a species whose genders look different (Generation IV onwards).
     */
    fun gameSprite(id: Int, game: Game, shiny: Boolean = false, female: Boolean = false): String? {
        if (game.isNational) return homeSprite(id, shiny, female)
        // Red, Blue and Yellow had no shinies.
        if (shiny && game.dataGen == 1) return null
        val (folder, ext) = spriteSet(game) ?: return null
        // Generation I–II folders keep shinies under the transparent folder; later ones beside the normal sprites.
        val path = (if (shiny) "$folder/shiny" else folder) + if (female) "/female" else ""
        return "$SPRITES/pokemon/versions/$path/$id.$ext"
    }

    /** Games grouped by the sprite set they share (Ruby and Sapphire, Black to White 2…), in release order. */
    val spriteSets: List<List<Game>> by lazy { Game.playable.groupBy { spriteSet(it)?.first }.filterKeys { it != null }.values.toList() }

    /** A game's sprite folder and file type. */
    private fun spriteSet(game: Game): Pair<String, String>? = when (game) {
        Game.RED, Game.BLUE -> "generation-i/red-blue/transparent" to "png"
        Game.YELLOW -> "generation-i/yellow/transparent" to "png"
        Game.GOLD -> "generation-ii/gold/transparent" to "png"
        Game.SILVER -> "generation-ii/silver/transparent" to "png"
        Game.CRYSTAL -> "generation-ii/crystal/transparent" to "png"
        Game.RUBY, Game.SAPPHIRE -> "generation-iii/ruby-sapphire" to "png"
        Game.EMERALD -> "generation-iii/emerald" to "png"
        Game.FIRERED, Game.LEAFGREEN -> "generation-iii/firered-leafgreen" to "png"
        Game.DIAMOND, Game.PEARL -> "generation-iv/diamond-pearl" to "png"
        Game.PLATINUM -> "generation-iv/platinum" to "png"
        Game.HEARTGOLD, Game.SOULSILVER -> "generation-iv/heartgold-soulsilver" to "png"
        Game.BLACK, Game.WHITE, Game.BLACK_2, Game.WHITE_2 -> "generation-v/black-white" to "png"
        Game.X, Game.Y -> "generation-vi/x-y" to "png"
        Game.OMEGA_RUBY, Game.ALPHA_SAPPHIRE -> "generation-vi/omega-ruby-alpha-sapphire" to "png"
        Game.SUN, Game.MOON, Game.ULTRA_SUN, Game.ULTRA_MOON -> "generation-vii/ultra-sun-ultra-moon" to "gif"
        Game.LETS_GO_PIKACHU, Game.LETS_GO_EEVEE -> "generation-vii/lets-go-pikachu-lets-go-eevee" to "gif"
        Game.SWORD, Game.SHIELD -> "generation-viii/sword-shield" to "webp"
        Game.BRILLIANT_DIAMOND, Game.SHINING_PEARL -> "generation-viii/brilliant-diamond-shining-pearl" to "webp"
        Game.LEGENDS_ARCEUS -> "generation-viii/legends-arceus" to "webp"
        Game.SCARLET, Game.VIOLET -> "generation-ix/scarlet-violet" to "webp"
        Game.LEGENDS_ZA -> "generation-ix/legends-za" to "webp"
        Game.NATIONAL -> null
    }

    fun item(key: String) = "$SPRITES/items/$key.png"

    /** Games up to Generation V use the original cries; later games and alternate forms use today's. */
    fun cry(id: Int, game: Game) =
        if (!game.isNational && game.dataGen <= 5 && id <= LAST_LEGACY_CRY) "$CRIES/legacy/$id.ogg" else "$CRIES/latest/$id.ogg"

    fun latestCry(id: Int) = "$CRIES/latest/$id.ogg"
}
