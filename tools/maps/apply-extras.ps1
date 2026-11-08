param([string]$MapDir = "$PSScriptRoot\..\..\app\src\main\assets\maps", [string]$Extras = "$PSScriptRoot\extras.json")
# Copies the maps of their own in pages\ (areas split off a region, such as the Battle Zone) next to the region maps, then
# merges extras.json into the region maps in place: removes spots that moved to a page (with their lines and, by a point
# inside it, their inset's outline), moves spots, changes or adds to a spot's locations, drops lines that a new spot now
# sits on, and adds the new spots and lines. Safe to run twice (spots are matched by id, lines by their ends), so it can
# follow a fresh run of linkmaps.ps1 whenever the design's maps change.
$utf8 = New-Object System.Text.UTF8Encoding $false
Get-ChildItem "$PSScriptRoot\pages\*.json" | ForEach-Object { Copy-Item $_.FullName (Join-Path $MapDir $_.Name) -Force }
$all = [IO.File]::ReadAllText($Extras, $utf8) | ConvertFrom-Json
function Js($o) { ($o | ConvertTo-Json -Compress -Depth 4) }
function NodeJson($n) {
    $o = [ordered]@{ id = $n.id; name = $n.name; kind = $n.kind; x = $n.x; y = $n.y; locations = @($n.locations) }
    if ($n.sea) { $o.sea = $true }
    if ($n.labelSide) { $o.labelSide = $n.labelSide }
    if ($n.games) { $o.games = @($n.games) }
    if ($n.names) { $o.names = $n.names }
    Js $o
}
function EdgeJson($e) {
    $o = [ordered]@{ from = $e.from; to = $e.to; style = $e.style }
    if ($e.elbow) { $o.elbow = $e.elbow }
    Js $o
}
foreach ($region in ($all.PSObject.Properties | Where-Object { $_.Name -notlike '_*' })) {
    $x = $region.Value
    $file = Join-Path $MapDir "$($region.Name).json"
    $m = [IO.File]::ReadAllText($file, $utf8) | ConvertFrom-Json
    $nodes = [Collections.ArrayList]@($m.nodes)
    $edges = [Collections.ArrayList]@($m.edges)
    $gone = @($x.removeNodes | Where-Object { $_ })
    foreach ($n in @($nodes | Where-Object { $gone -contains $_.id })) { $nodes.Remove($n) }
    foreach ($e in @($edges | Where-Object { $gone -contains $_.from -or $gone -contains $_.to })) { $edges.Remove($e) }
    foreach ($pt in @($x.dropLandAt)) {
        if (-not $pt) { continue }
        # Keep every outline that doesn't contain the point (even-odd ray cast).
        $kept = [regex]::Matches($m.land, 'M[^Z]*Z') | Where-Object {
            $p = @([regex]::Matches($_.Value, '(-?[\d.]+),(-?[\d.]+)') | ForEach-Object { , @([double]$_.Groups[1].Value, [double]$_.Groups[2].Value) })
            $in = $false
            for ($i = 0; $i -lt $p.Count; $i++) {
                $a = $p[$i]; $b = $p[($i + 1) % $p.Count]
                if ((($a[1] -gt $pt[1]) -ne ($b[1] -gt $pt[1])) -and ($pt[0] -lt ($b[0] - $a[0]) * ($pt[1] - $a[1]) / ($b[1] - $a[1]) + $a[0])) { $in = -not $in }
            }
            -not $in
        } | ForEach-Object { $_.Value }
        if (@($kept).Count -eq [regex]::Matches($m.land, 'M[^Z]*Z').Count) { Write-Warning "$($region.Name) has no land at $($pt -join ',')" }
        $m.land = $kept -join ''
    }
    $byId = @{}; foreach ($n in $nodes) { $byId[$n.id] = $n }
    if ($x.move) {
        foreach ($p in $x.move.PSObject.Properties) {
            if (-not $byId[$p.Name]) { throw "$($region.Name) has no spot $($p.Name)" }
            $byId[$p.Name].x = $p.Value[0]; $byId[$p.Name].y = $p.Value[1]
        }
    }
    if ($x.set) {
        foreach ($p in $x.set.PSObject.Properties) {
            if (-not $byId[$p.Name]) { throw "$($region.Name) has no spot $($p.Name)" }
            foreach ($f in $p.Value.PSObject.Properties) { $byId[$p.Name] | Add-Member -NotePropertyName $f.Name -NotePropertyValue $f.Value -Force }
        }
    }
    if ($x.addLocations) {
        foreach ($p in $x.addLocations.PSObject.Properties) {
            $n = $byId[$p.Name]; if (-not $n) { throw "$($region.Name) has no spot $($p.Name)" }
            $n.locations = @(@($n.locations) + @($p.Value) | Select-Object -Unique)
        }
    }
    foreach ($k in @($x.dropEdges)) {
        if (-not $k) { continue }
        $hit = @($edges | Where-Object { "$($_.from)-$($_.to)" -eq $k })
        if (-not $hit) { Write-Warning "$($region.Name) has no line $k to drop" }
        foreach ($e in $hit) { $edges.Remove($e) }
    }
    foreach ($n in @($x.nodes)) {
        if (-not $n) { continue }
        if ($byId[$n.id]) { $nodes[$nodes.IndexOf($byId[$n.id])] = $n } else { [void]$nodes.Add($n) }
        $byId[$n.id] = $n
    }
    foreach ($e in @($x.edges)) {
        if (-not $e) { continue }
        foreach ($end in $e.from, $e.to) { if (-not $byId[$end]) { throw "$($region.Name) line $($e.from)-$($e.to) has no spot $end" } }
        $same = @($edges | Where-Object { ($_.from -eq $e.from -and $_.to -eq $e.to) -or ($_.from -eq $e.to -and $_.to -eq $e.from) })
        foreach ($s in $same) { $edges.Remove($s) }
        [void]$edges.Add($e)
    }
    $text = "{`n  `"region`": $(Js $m.region),`n  `"name`": $(Js $m.name),`n  `"games`": $(Js $m.games),`n  `"viewBox`": $(Js @($m.viewBox)),`n  `"land`": $(Js $m.land),`n  `"nodes`": [`n" +
        (($nodes | ForEach-Object { '    ' + (NodeJson $_) }) -join ",`n") + "`n  ],`n  `"edges`": [`n" +
        (($edges | ForEach-Object { '    ' + (EdgeJson $_) }) -join ",`n") + "`n  ]`n}`n"
    [IO.File]::WriteAllText($file, $text, $utf8)
    "$($region.Name): $($nodes.Count) spots, $($edges.Count) lines"
}
