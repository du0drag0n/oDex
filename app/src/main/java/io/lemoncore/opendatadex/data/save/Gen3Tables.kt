package io.lemoncore.opendatadex.data.save

// Generated from the pret decompilations (include/constants/items.h and src/data/region_map/region_map_sections.json in
// pokefirered and pokeemerald), matched to oDex's item keys and locations.

/** Generation III item indexes as PokéAPI item keys; empty for indexes no item uses. */
private val ITEM_KEYS = listOf(
    "", "master-ball", "ultra-ball", "great-ball", "poke-ball", "safari-ball", "net-ball", "dive-ball", "nest-ball",
    "repeat-ball", "timer-ball", "luxury-ball", "premier-ball", "potion", "antidote", "burn-heal", "ice-heal",
    "awakening", "paralyze-heal", "full-restore", "max-potion", "hyper-potion", "super-potion", "full-heal", "revive",
    "max-revive", "fresh-water", "soda-pop", "lemonade", "moomoo-milk", "energy-powder", "energy-root", "heal-powder",
    "revival-herb", "ether", "max-ether", "elixir", "max-elixir", "lava-cookie", "blue-flute", "yellow-flute",
    "red-flute", "black-flute", "white-flute", "berry-juice", "sacred-ash", "shoal-salt", "shoal-shell", "red-shard",
    "blue-shard", "yellow-shard", "green-shard", "", "", "", "", "", "", "", "", "", "", "", "hp-up", "protein",
    "iron", "carbos", "calcium", "rare-candy", "pp-up", "zinc", "pp-max", "", "guard-spec", "dire-hit", "x-attack",
    "x-defense", "x-speed", "x-accuracy", "x-sp-atk", "poke-doll", "fluffy-tail", "", "super-repel", "max-repel",
    "escape-rope", "repel", "", "", "", "", "", "", "sun-stone", "moon-stone", "fire-stone", "thunder-stone",
    "water-stone", "leaf-stone", "", "", "", "", "tiny-mushroom", "big-mushroom", "", "pearl", "big-pearl", "stardust",
    "star-piece", "nugget", "heart-scale", "", "", "", "", "", "", "", "", "", "orange-mail", "harbor-mail",
    "glitter-mail", "mech-mail", "wood-mail", "wave-mail", "bead-mail", "shadow-mail", "tropic-mail", "dream-mail",
    "fab-mail", "retro-mail", "cheri-berry", "chesto-berry", "pecha-berry", "rawst-berry", "aspear-berry",
    "leppa-berry", "oran-berry", "persim-berry", "lum-berry", "sitrus-berry", "figy-berry", "wiki-berry", "mago-berry",
    "aguav-berry", "iapapa-berry", "razz-berry", "bluk-berry", "nanab-berry", "wepear-berry", "pinap-berry",
    "pomeg-berry", "kelpsy-berry", "qualot-berry", "hondew-berry", "grepa-berry", "tamato-berry", "cornn-berry",
    "magost-berry", "rabuta-berry", "nomel-berry", "spelon-berry", "pamtre-berry", "watmel-berry", "durin-berry",
    "belue-berry", "liechi-berry", "ganlon-berry", "salac-berry", "petaya-berry", "apicot-berry", "lansat-berry",
    "starf-berry", "enigma-berry", "", "", "", "bright-powder", "white-herb", "macho-brace", "exp-share", "quick-claw",
    "soothe-bell", "mental-herb", "choice-band", "kings-rock", "silver-powder", "amulet-coin", "cleanse-tag",
    "soul-dew", "deep-sea-tooth", "deep-sea-scale", "smoke-ball", "everstone", "focus-band", "lucky-egg", "scope-lens",
    "metal-coat", "leftovers", "dragon-scale", "light-ball", "soft-sand", "hard-stone", "miracle-seed",
    "black-glasses", "black-belt", "magnet", "mystic-water", "sharp-beak", "poison-barb", "never-melt-ice",
    "spell-tag", "twisted-spoon", "charcoal", "dragon-fang", "silk-scarf", "up-grade", "shell-bell", "sea-incense",
    "lax-incense", "lucky-punch", "metal-powder", "thick-club", "stick", "", "", "", "", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "red-scarf", "blue-scarf", "pink-scarf",
    "green-scarf", "yellow-scarf", "mach-bike", "coin-case", "dowsing-machine", "old-rod", "good-rod", "super-rod",
    "ss-ticket", "contest-pass", "", "wailmer-pail", "devon-goods", "soot-sack", "basement-key", "acro-bike",
    "pokeblock-case", "letter", "eon-ticket", "red-orb", "blue-orb", "scanner", "go-goggles", "meteorite",
    "key-to-room-1", "key-to-room-2", "key-to-room-4", "key-to-room-6", "storage-key", "root-fossil", "claw-fossil",
    "devon-scope", "tm01", "tm02", "tm03", "tm04", "tm05", "tm06", "tm07", "tm08", "tm09", "tm10", "tm11", "tm12",
    "tm13", "tm14", "tm15", "tm16", "tm17", "tm18", "tm19", "tm20", "tm21", "tm22", "tm23", "tm24", "tm25", "tm26",
    "tm27", "tm28", "tm29", "tm30", "tm31", "tm32", "tm33", "tm34", "tm35", "tm36", "tm37", "tm38", "tm39", "tm40",
    "tm41", "tm42", "tm43", "tm44", "tm45", "tm46", "tm47", "tm48", "tm49", "tm50", "hm01", "hm02", "hm03", "hm04",
    "hm05", "hm06", "hm07", "hm08", "", "", "parcel", "poke-flute", "secret-key", "bike-voucher", "gold-teeth",
    "old-amber", "card-key", "lift-key", "helix-fossil", "dome-fossil", "silph-scope", "bicycle", "town-map",
    "vs-seeker", "fame-checker", "tm-case", "berry-pouch", "teachy-tv", "tri-pass", "rainbow-pass", "tea",
    "mysticticket", "auroraticket", "powder-jar", "ruby", "sapphire",
)

/** Generation III map sections in index order: Hoenn, then Kanto and the Sevii Islands from 88, then Emerald's own. */
private val PLACE_NAMES = listOf(
    "Littleroot Town", "Oldale Town", "Dewford Town", "Lavaridge Town", "Fallarbor Town", "Verdanturf Town",
    "Pacifidlog Town", "Petalburg City", "Slateport City", "Mauville City", "Rustboro City", "Fortree City",
    "Lilycove City", "Mossdeep City", "Sootopolis City", "Ever Grande City", "Route 101", "Route 102", "Route 103",
    "Route 104", "Route 105", "Route 106", "Route 107", "Route 108", "Route 109", "Route 110", "Route 111",
    "Route 112", "Route 113", "Route 114", "Route 115", "Route 116", "Route 117", "Route 118", "Route 119",
    "Route 120", "Route 121", "Route 122", "Route 123", "Route 124", "Route 125", "Route 126", "Route 127",
    "Route 128", "Route 129", "Route 130", "Route 131", "Route 132", "Route 133", "Route 134", "Underwater",
    "Underwater", "Underwater", "Underwater", "Underwater", "Granite Cave", "Mt. Chimney", "Safari Zone",
    "Battle Frontier", "Petalburg Woods", "Rusturf Tunnel", "Abandoned Ship", "New Mauville", "Meteor Falls",
    "Meteor Falls", "Mt. Pyre", "{aqua} Hideout", "Shoal Cave", "Seafloor Cavern", "Underwater", "Victory Road",
    "Mirage Island", "Cave of Origin", "Southern Island", "Fiery Path", "Fiery Path", "Jagged Pass", "Jagged Pass",
    "Sealed Chamber", "Underwater", "Scorched Slab", "Island Cave", "Desert Ruins", "Ancient Tomb", "Inside of Truck",
    "Sky Pillar", "Secret Base", "", "Pallet Town", "Viridian City", "Pewter City", "Cerulean City", "Lavender Town",
    "Vermilion City", "Celadon City", "Fuchsia City", "Cinnabar Island", "Indigo Plateau", "Saffron City", "Route 4",
    "Route 10", "Route 1", "Route 2", "Route 3", "Route 4", "Route 5", "Route 6", "Route 7", "Route 8", "Route 9",
    "Route 10", "Route 11", "Route 12", "Route 13", "Route 14", "Route 15", "Route 16", "Route 17", "Route 18",
    "Route 19", "Route 20", "Route 21", "Route 22", "Route 23", "Route 24", "Route 25", "Viridian Forest", "Mt. Moon",
    "S.S. Anne", "Underground Path", "Underground Path", "Diglett's Cave", "Victory Road", "Rocket Hideout",
    "Silph Co.", "Pokémon Mansion", "Safari Zone", "Pokémon League", "Rock Tunnel", "Seafoam Islands",
    "Pokémon Tower", "Cerulean Cave", "Power Plant", "One Island", "Two Island", "Three Island", "Four Island",
    "Five Island", "Seven Island", "Six Island", "Kindle Road", "Treasure Beach", "Cape Brink", "Bond Bridge",
    "Three Isle Port", "Sevii Isle 6", "Sevii Isle 7", "Sevii Isle 8", "Sevii Isle 9", "Resort Gorgeous",
    "Water Labyrinth", "Five Isle Meadow", "Memorial Pillar", "Outcast Island", "Green Path", "Water Path",
    "Ruin Valley", "Trainer Tower", "Canyon Entrance", "Sevault Canyon", "Tanoby Ruins", "Sevii Isle 22",
    "Sevii Isle 23", "Sevii Isle 24", "Navel Rock", "Mt. Ember", "Berry Forest", "Icefall Cave", "Rocket Warehouse",
    "Trainer Tower", "Dotted Hole", "Lost Cave", "Pattern Bush", "Altering Cave", "Tanoby Chambers", "Three Isle Path",
    "Tanoby Key", "Birth Island", "Monean Chamber", "Liptoo Chamber", "Weepth Chamber", "Dilford Chamber",
    "Scufib Chamber", "Rixy Chamber", "Viapois Chamber", "Ember Spa", "Special Area", "Aqua Hideout", "Magma Hideout",
    "Mirage Tower", "Birth Island", "Faraway Island", "Artisan Cave", "Marine Cave", "Underwater", "Terra Cave",
    "Underwater", "Underwater", "Underwater", "Desert Underpass", "Altering Cave", "Navel Rock", "Trainer Hill",
)

/** The PokéAPI location id of each map section, 0 where oDex has no matching location; used for translated names. */
private val PLACE_LOCATIONS = intArrayOf(
    567, 0, 484, 569, 570, 571, 485, 429, 430, 0, 573, 574, 431, 432, 433, 434, 449, 450, 451, 452, 453, 454, 455, 456,
    457, 458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470, 471, 472, 473, 474, 475, 476, 477, 478, 479,
    480, 481, 482, 0, 0, 0, 0, 0, 437, 0, 483, 0, 438, 436, 447, 446, 435, 435, 441, 0, 445, 442, 0, 444, 577, 443,
    578, 440, 440, 439, 439, 0, 0, 580, 581, 582, 583, 0, 448, 0, 0, 86, 154, 231, 68, 232, 151, 67, 76, 71, 233, 234,
    120, 89, 88, 99, 109, 120, 130, 131, 132, 133, 134, 89, 90, 91, 92, 93, 94, 95, 96, 97, 0, 0, 0, 102, 157, 103,
    104, 155, 80, 498, 0, 0, 73, 152, 0, 0, 161, 162, 0, 87, 136, 160, 147, 0, 526, 0, 0, 527, 528, 0, 0, 505, 506,
    507, 508, 509, 0, 0, 0, 0, 510, 511, 512, 513, 514, 515, 516, 517, 518, 519, 520, 521, 0, 0, 0, 811, 500, 501, 502,
    0, 518, 0, 504, 503, 529, 0, 0, 0, 810, 491, 492, 493, 494, 495, 496, 0, 0, 0, 0, 486, 487, 0, 812, 489, 804, 0,
    803, 0, 0, 0, 488, 490, 0, 0,
)

/** The PokéAPI item key for a Generation III item index, or null for none or an unused index. */
fun gen3ItemKey(index: Int): String? = ITEM_KEYS.getOrNull(index)?.takeIf { it.isNotEmpty() }

/** Where a Pokémon was met: its English name, and the PokéAPI location id (0 when none) for a translated one. */
data class SavePlace(val name: String, val locationId: Int)

/** A met location's place, or null for the special codes ([MET_EGG], [MET_TRADE], [MET_FATEFUL]) or an unknown one. */
fun gen3Place(index: Int): SavePlace? = PLACE_NAMES.getOrNull(index)?.takeIf { it.isNotEmpty() }?.let { SavePlace(it, PLACE_LOCATIONS[index]) }

/** Special met locations: an Egg received as a gift, an in-game trade, and a fateful encounter (an event). */
const val MET_EGG = 0xFD
const val MET_TRADE = 0xFE
const val MET_FATEFUL = 0xFF