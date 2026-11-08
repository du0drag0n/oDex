param([string]$MapDir, [string]$Csv, [string]$OutDir)
# Copies the design's region maps (v2) into the app:
#  - each node gets its PokéAPI location ids ("locations"), from pokeapiLocation or the table below;
#  - route nodes whose every line is a sea route get "sea": true (for the SEA ROUTE kind line);
#  - edges get "elbow": "h" / "v" where the design bends the line the other way from its default (dx >= dy -> h);
#  - sampleWild, hasWild and pokeapiLocation are dropped.
# MapDir is the design handoff's maps folder, Csv a folder holding PokéAPI's locations.csv, and OutDir
# app/src/main/assets/maps. Run apply-extras.ps1 afterwards to add the spots the design leaves out.
$fixes = @{
    'alola:wela' = 'wela-volcano-park wela-volcano-park--totems-den'
    'alola:hokulani' = 'mount-hokulani'
    'alola:potown' = 'po-town'
    'alola:seafolk' = 'seafolk-village'
    'alola:lanakila' = 'mount-lanakila'
    'hoenn:hsafari' = 'hoenn-safari-zone'
    'johto:j40' = 'johto-sea-route-40'
    'johto:j41' = 'johto-sea-route-41'
    'johto:j26' = 'kanto-route-26'
    'johto:j27' = 'kanto-route-27'
    'johto:j28' = 'kanto-route-28'
    'johto:jsafari' = 'johto-safari-zone'
    'kalos:kvictory' = 'kalos-victory-road'
    'kalos:kiloude' = 'kiloude-city'
    'kanto:victory' = 'kanto-victory-road-1 kanto-victory-road-2'
    'kanto:safari' = 'kanto-safari-zone'
    'kanto:r19' = 'kanto-sea-route-19'
    'kanto:r20' = 'kanto-sea-route-20'
    'kanto:k21' = 'kanto-sea-route-21'
    'kanto:cinnabar' = 'cinnabar-island'
    'sevii:altering' = 'kanto-altering-cave'
    'sinnoh:s220' = 'sinnoh-sea-route-220'
    'sinnoh:s223' = 'sinnoh-sea-route-223'
    'sinnoh:s226' = 'sinnoh-sea-route-226'
    'sinnoh:s230' = 'sinnoh-sea-route-230'
    'unova:village' = 'village-bridge'
    'unova:black' = 'black-city'
    # Galar has no slugs in the design (MAPS.md §5.2).
    'galar:postwick' = 'postwick'; 'galar:wedgehurst' = 'wedgehurst'; 'galar:motostoke' = 'motostoke motostoke-outskirts'
    'galar:g1' = 'galar-route-1'; 'galar:g2' = 'galar-route-2'; 'galar:g3' = 'galar-route-3'; 'galar:g4' = 'galar-route-4'
    'galar:g5' = 'galar-route-5'; 'galar:g6' = 'galar-route-6'; 'galar:g7' = 'galar-route-7'; 'galar:g8' = 'galar-route-8'
    'galar:g9' = 'galar-route-9 galar-mine-no-2'; 'galar:g10' = 'galar-route-10'
    # The Wild Area hub holds the zones the map doesn't draw yet (6 of them have their own spots).
    'galar:wild' = 'motostoke-riverbank dappled-grove watchtower-ruins giants-seat giants-mirror hammerlocke-hills stony-wilderness dusty-bowl'
    'galar:rolling' = 'rolling-fields'; 'galar:axewell' = 'east-lake-axewell west-lake-axewell axews-eye'
    'galar:miloch' = 'north-lake-miloch south-lake-miloch'; 'galar:bridge' = 'bridge-field'; 'galar:cap' = 'giants-cap'
    'galar:outrage' = 'lake-of-outrage'; 'galar:mine' = 'galar-mine'; 'galar:turffield' = 'turffield'; 'galar:hulbury' = 'hulbury'
    'galar:hammerlocke' = 'hammerlocke'; 'galar:stow' = 'stow-on-side'; 'galar:glimwood' = 'glimwood-tangle'; 'galar:ballonlea' = 'ballonlea'
    'galar:circhester' = 'circhester'; 'galar:spikemuth' = 'spikemuth'; 'galar:wyndon' = 'wyndon'; 'galar:weald' = 'slumbering-weald'
    # Route 2's lakeside isn't its own PokéAPI location.
    'galar:lakeside' = ''
    'galar:honor' = 'fields-of-honor'; 'galar:dojo' = 'master-dojo'; 'galar:wetlands' = 'soothing-wetlands'; 'galar:focus' = 'forest-of-focus'
    'galar:freezington' = 'freezington'; 'galar:slippery' = 'slippery-slope'; 'galar:frostpoint' = 'frostpoint-field'; 'galar:giantbed' = 'giants-bed'
    'paldea:cabopoco' = 'cabo-poco'; 'paldea:mesagoza' = 'mesagoza'; 'paldea:cortondo' = 'cortondo'; 'paldea:artazon' = 'artazon'
    'paldea:cascarrafa' = 'cascarrafa'; 'paldea:medali' = 'medali'; 'paldea:montenevera' = 'montenevera'; 'paldea:zapapico' = 'zapapico'
    'paldea:levincia' = 'levincia'; 'paldea:losplatos' = 'los-platos'; 'paldea:alfornada' = 'alfornada alfornada-cavern'; 'paldea:porto' = 'porto-marinada'
    'paldea:asado' = 'asado-desert'; 'paldea:glaseado' = 'glaseado-mountain'; 'paldea:tagtree' = 'tagtree-thicket'; 'paldea:zero' = 'area-zero'
    'paldea:poco' = 'poco-path'; 'paldea:inlet' = 'inlet-grotto'; 'paldea:casseroya' = 'casseroya-lake'; 'paldea:dalizapa' = 'dalizapa-passage'
    'paldea:sp1' = 'paldea-south-province-area-one'; 'paldea:sp2' = 'paldea-south-province-area-two'; 'paldea:sp3' = 'paldea-south-province-area-three'
    'paldea:sp4' = 'paldea-south-province-area-four'; 'paldea:sp5' = 'paldea-south-province-area-five'; 'paldea:sp6' = 'paldea-south-province-area-six'
    'paldea:ep1' = 'paldea-east-province-area-one'; 'paldea:ep2' = 'paldea-east-province-area-two'; 'paldea:ep3' = 'paldea-east-province-area-three'
    'paldea:wp1' = 'paldea-west-province-area-one'; 'paldea:wp2' = 'paldea-west-province-area-two'; 'paldea:wp3' = 'paldea-west-province-area-three'
    'paldea:np1' = 'paldea-north-province-area-one'; 'paldea:np2' = 'paldea-north-province-area-two'; 'paldea:np3' = 'paldea-north-province-area-three'
}
# Line bends from the design HTML (hf = horizontal first, vf = vertical first).
$hf = @{
    kanto = 'pewter-k3 k9-tunnel k12-k13 k17-k18 fuchsia-r19 viridian-k22'; sevii = 'bond-three'
    hoenn = 'r113-r111 fortree-r120 r121-pyre dewford-r105 r124-mossdeep'; unova = 'pinwheel-nacrene u12-lacunosa'
    kalos = 'lumiose-x4 x5-lumiose cyllage-x10 reflection-shalour x17-anistar'; alola = 'a1-a2 a2-a3 hauoli-heahea heahea-malie'
}
$vf = @{
    kanto = 'k3-moon k24-k25 k11-k12'; johto = 'j45-j46'; hoenn = 'r109-r108 r120-r121 r128-r131'; sinnoh = 's208-hearthome s205-eforest'
    kalos = 'lumiose-x14 x7-camphrier shalour-x12'; alola = 'a8-konikoni b17-tapu b12-tapu malie-seafolk malie-b12'
}
# Sea routes, from the design's sea lists (Hoenn's is HSEA).
$seaRoutes = @{
    kanto = 'r19 r20 k21'; johto = 'j40 j41'; sinnoh = 's218 s219 s220 s223 s230 s225'; unova = 'u17 u18 u21'; galar = 'g9'
    hoenn = 'r105 r106 r125 r126 r127 r129 r130 r133 r134 r107 r108 r109 r124 r128 r131 r132'
}
$ids = @{}; Import-Csv "$Csv\locations.csv" | ForEach-Object { $ids[$_.identifier] = [int]$_.id }
New-Item -ItemType Directory -Force $OutDir | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding $false
function Js($o) { ($o | ConvertTo-Json -Compress -Depth 4) }
foreach ($f in Get-ChildItem "$MapDir\*.json") {
    $region = $f.BaseName
    $m = [IO.File]::ReadAllText($f.FullName, $utf8) | ConvertFrom-Json
    $bends = @{}
    if ($hf[$region]) { $hf[$region] -split ' ' | ForEach-Object { $bends[$_] = 'h' } }
    if ($vf[$region]) { $vf[$region] -split ' ' | ForEach-Object { $bends[$_] = 'v' } }
    $keys = $m.edges | ForEach-Object { "$($_.from)-$($_.to)" }
    # The design matches bends by exact "from-to", so one written the other way round never applied there either.
    foreach ($k in @($bends.Keys)) { if ($keys -notcontains $k) { Write-Warning "$region has no edge $k (ignored, as in the design)"; $bends.Remove($k) } }
    $nodeLines = foreach ($n in $m.nodes) {
        $key = "${region}:$($n.id)"
        $slugs = if ($fixes.ContainsKey($key)) { @($fixes[$key] -split ' ' | Where-Object { $_ }) } elseif ($n.pokeapiLocation) { @($n.pokeapiLocation) } else { throw "no location for $key" }
        $locs = @($slugs | ForEach-Object { if (-not $ids[$_]) { throw "unknown location $_ ($key)" }; $ids[$_] })
        $o = [ordered]@{ id = $n.id; name = $n.name; kind = $n.kind; x = $n.x; y = $n.y; locations = $locs }
        if ($n.kind -eq 'route' -and ($seaRoutes[$region] -split ' ') -contains $n.id) { $o.sea = $true }
        if ($n.labelSide) { $o.labelSide = $n.labelSide }
        '    ' + (Js $o)
    }
    $edgeLines = foreach ($e in $m.edges) {
        $o = [ordered]@{ from = $e.from; to = $e.to; style = $e.style }
        $b = $bends["$($e.from)-$($e.to)"]; if ($b) { $o.elbow = $b }
        '    ' + (Js $o)
    }
    $text = "{`n  `"region`": $(Js $m.region),`n  `"name`": $(Js $m.name),`n  `"games`": $(Js $m.games),`n  `"viewBox`": $(Js @($m.viewBox)),`n  `"land`": $(Js $m.land),`n  `"nodes`": [`n" + ($nodeLines -join ",`n") + "`n  ],`n  `"edges`": [`n" + ($edgeLines -join ",`n") + "`n  ]`n}`n"
    [IO.File]::WriteAllText((Join-Path $OutDir $f.Name), $text, $utf8)
    "$region ok: $($m.nodes.Count) nodes, $($m.edges.Count) edges, $($bends.Count) bends"
}
