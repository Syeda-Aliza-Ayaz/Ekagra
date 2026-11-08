# Draws the Ekagra logo (blue rings + coral centre dot) and saves it as packaging\ekagra.ico
# for the Windows executable. Run once; the .ico is kept in the repo.
param([string]$Out = "$PSScriptRoot\ekagra.ico")
Add-Type -AssemblyName System.Drawing

function New-LogoPng([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)
    $r = $size / 2.0
    $stroke = $r * 0.14
    $blue = [System.Drawing.Color]::FromArgb(255, 0x22, 0x61, 0x92)
    $outer = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(115, 0x22, 0x61, 0x92)), $stroke
    $inner = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(205, 0x22, 0x61, 0x92)), $stroke
    $ro = $r * 0.88 - $stroke / 2
    $ri = $r * 0.56 - $stroke / 2
    $g.DrawEllipse($outer, [float]($r - $ro), [float]($r - $ro), [float](2 * $ro), [float](2 * $ro))
    $g.DrawEllipse($inner, [float]($r - $ri), [float]($r - $ri), [float](2 * $ri), [float](2 * $ri))
    $dot = $r * 0.22
    $coral = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 0xEF, 0x85, 0x57))
    $g.FillEllipse($coral, [float]($r - $dot), [float]($r - $dot), [float](2 * $dot), [float](2 * $dot))
    $g.Dispose()
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    return ,$ms.ToArray()
}

# ICO file = header + one directory entry per size + the PNG images
$sizes = @(16, 24, 32, 48, 64, 128, 256)
$images = @(); foreach ($s in $sizes) { $images += ,(New-LogoPng $s) }
$fs = [System.IO.File]::Create($Out)
$w = New-Object System.IO.BinaryWriter $fs
$w.Write([UInt16]0); $w.Write([UInt16]1); $w.Write([UInt16]$sizes.Count)
$offset = 6 + 16 * $sizes.Count
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $s = $sizes[$i]; $len = $images[$i].Length
    $w.Write([byte]($(if ($s -ge 256) { 0 } else { $s })))
    $w.Write([byte]($(if ($s -ge 256) { 0 } else { $s })))
    $w.Write([byte]0); $w.Write([byte]0)
    $w.Write([UInt16]1); $w.Write([UInt16]32)
    $w.Write([UInt32]$len); $w.Write([UInt32]$offset)
    $offset += $len
}
foreach ($img in $images) { $w.Write($img) }
$w.Close()
Write-Output "Icon written to $Out"
