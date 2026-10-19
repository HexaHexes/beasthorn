# Produces the name-overlay copies of the listing art:
#
#   beasthorn-icon-512-named.png       CurseForge project icon
#   beasthorn-social-preview-named.png GitHub / CurseForge banner
#
# The name is drawn with Minecraft's *actual* font, not a lookalike. That font
# is not a TTF - it is a 128x128 atlas of 8x8 glyph cells (textures/font/ascii.png,
# provider ascent 7) where codepoint c lives at cell (c % 16, c / 16). Advance is
# the rightmost inked column + 1, which is how Minecraft itself measures, so the
# text is metrically identical to what the game draws.
#
# Text is composed at 1x (with the drop shadow Minecraft draws at +1,+1) and
# nearest-neighbour scaled up, so it stays crisp instead of resampling soft.
#
# Both variants start from the SHIPPED PNGs rather than re-rendering the SVGs.
# A fresh browser render shifts antialiasing on every run, which would dirty the
# repo each time generate-named.ps1 is invoked and would make the named banner a
# subtly different piece of art from the banner it is supposed to be a copy of.
# Deriving from the committed PNGs keeps reruns byte-identical.
#
# Placement follows the art decisions: on the square icons the name sits at top
# centre so it never covers the three orbs; on the banner it takes the exact slot
# the serif wordmark occupied, so there is never a second title crossing the horn.
#
# Note: all math is inlined. Passing @() into a function's positional params
# unrolls it across them. Inline arithmetic inside New-Object's argument list
# also splits at operators, so dimensions are computed into variables first.

Add-Type -AssemblyName System.Drawing

$text      = "Beast Horn"
$iconIn    = "$PSScriptRoot\beasthorn-icon-512.png"
$bannerIn  = "$PSScriptRoot\beasthorn-social-preview.png"
$iconOut   = "$PSScriptRoot\beasthorn-icon-512-named.png"
$bannerOut = "$PSScriptRoot\beasthorn-social-preview-named.png"
$work      = Join-Path ([System.IO.Path]::GetTempPath()) "beasthorn-named"
New-Item -ItemType Directory -Force -Path $work | Out-Null

# ---- locate the vanilla font atlas -----------------------------------------
# It ships in ModDevGradle's client-extra jar alongside the other Minecraft
# resources, so this stays reproducible from a clean checkout after one build.
$atlasPng = Join-Path $work "ascii.png"
if (-not (Test-Path $atlasPng)) {
  $extra = Get-ChildItem (Join-Path $PSScriptRoot "..\build\moddev\artifacts") -Filter "*client-extra*.jar" `
           -ErrorAction SilentlyContinue | Select-Object -First 1
  if (-not $extra) {
    Write-Error "No client-extra jar under build\moddev\artifacts - run 'gradlew build' first."
    exit 1
  }
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $zip = [System.IO.Compression.ZipFile]::OpenRead($extra.FullName)
  $e   = $zip.Entries | Where-Object { $_.FullName -eq "assets/minecraft/textures/font/ascii.png" } | Select-Object -First 1
  if (-not $e) { $zip.Dispose(); Write-Error "ascii.png not found in $($extra.Name)"; exit 1 }
  [System.IO.Compression.ZipFileExtensions]::ExtractToFile($e, $atlasPng, $true)
  $zip.Dispose()
}

# ---- measure every advance straight off the atlas ---------------------------
$atlas = New-Object System.Drawing.Bitmap $atlasPng
$adv = @{}
foreach ($ch in ($text.ToCharArray() | Select-Object -Unique)) {
  if ($ch -eq ' ') { $adv[[string]$ch] = 4; continue }
  $cp   = [int][char]$ch
  $x0   = ($cp % 16) * 8
  $y0   = ([math]::Floor($cp / 16)) * 8
  $last = -1
  for ($x = 0; $x -lt 8; $x++) {
    for ($y = 0; $y -lt 8; $y++) {
      if ($atlas.GetPixel($x0 + $x, $y0 + $y).A -gt 0) { if ($x -gt $last) { $last = $x } }
    }
  }
  $adv[[string]$ch] = if ($last -ge 0) { $last + 1 } else { 0 }
}
$total = 0
foreach ($ch in $text.ToCharArray()) { $total += $adv[[string]$ch] }

function New-TextBitmap($txt, $mainCol, $shadowCol) {
  $w = $total + 1
  $h = 9                                   # 8 glyph rows + 1 shadow row
  $bmp = New-Object System.Drawing.Bitmap $w, $h
  $pen = 0
  foreach ($ch in $txt.ToCharArray()) {
    $a = $adv[[string]$ch]
    if ($ch -ne ' ') {
      $cp = [int][char]$ch
      $x0 = ($cp % 16) * 8
      $y0 = ([math]::Floor($cp / 16)) * 8
      for ($sy = 0; $sy -lt 8; $sy++) {
        for ($sx = 0; $sx -lt 8; $sx++) {
          $p = $atlas.GetPixel($x0 + $sx, $y0 + $sy)
          if ($p.A -gt 0) {
            if ($pen + $sx + 1 -lt $w -and $sy + 1 -lt $h) { $bmp.SetPixel($pen + $sx + 1, $sy + 1, $shadowCol) }
            $bmp.SetPixel($pen + $sx, $sy, $mainCol)
          }
        }
      }
    }
    $pen += $a
  }
  return $bmp
}

# $textY is the top row of the glyphs; $xCenter their horizontal midpoint.
# $scrim paints a soft vertical fade behind the name for legibility over art.
function Add-NameOverlay($bmp, $scale, $scrim, $xCenter, $textY) {
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $g.PixelOffsetMode    = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

  $cw = $bmp.Width
  $tw = $total * $scale
  $th = 8 * $scale
  $dw = ($total + 1) * $scale              # includes the drop-shadow row/col
  $dh = 9 * $scale
  $tx = [int]($xCenter - $tw / 2)
  $ty = [int]$textY

  if ($scrim) {
    # A soft vertical fade rather than a solid bar - a hard-edged rectangle
    # reads as a sticker slammed across the art.
    $padY = [int](1.7 * $th)
    $yTop = [int]($ty - $padY)
    $hgt  = [int]($th + 2 * $padY)
    $edge = [System.Drawing.Color]::FromArgb(0, 8, 5, 3)
    $mid  = [System.Drawing.Color]::FromArgb(175, 8, 5, 3)
    $p0   = New-Object System.Drawing.Point 0, $yTop
    $off  = $yTop + $hgt
    $p1   = New-Object System.Drawing.Point 0, $off
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush $p0, $p1, $edge, $edge
    $blend = New-Object System.Drawing.Drawing2D.ColorBlend 3
    $blend.Colors    = @($edge, $mid, $edge)
    $blend.Positions = @([float]0.0, [float]0.44, [float]1.0)
    $brush.InterpolationColors = $blend
    $rw = $cw
    $r  = New-Object System.Drawing.Rectangle 0, $yTop, $rw, $hgt
    $g.FillRectangle($brush, $r)
    $brush.Dispose()
  }

  $tb = New-TextBitmap $text `
          ([System.Drawing.Color]::FromArgb(255, 250, 244, 224)) `
          ([System.Drawing.Color]::FromArgb(255,  38,  25,  13))
  $dr = New-Object System.Drawing.Rectangle $tx, $ty, $dw, $dh
  $g.DrawImage($tb, $dr)
  $tb.Dispose(); $g.Dispose()
  Write-Output ("      name: " + $tw + "px @ (" + $tx + "," + $ty + ")")
}

Write-Output "Naming overlays with Minecraft's own font atlas"
Write-Output ("  font measured: '" + $text + "' = " + $total + "px at scale 1")

# ---- square icon ------------------------------------------------------------
# Ground only, so a fade is enough to keep the name legible over it.
$icon = New-Object System.Drawing.Bitmap $iconIn
Add-NameOverlay $icon 6 $true 256 44
$icon.Save($iconOut, [System.Drawing.Imaging.ImageFormat]::Png)
$icon.Dispose()
Write-Output ("  wrote " + $iconOut + "   512x512")

# ---- banner -----------------------------------------------------------------
# The serif wordmark has to go before the pixel one can take its slot. It lies
# on a plain stretch of the background gradient - nothing bright above y231 or
# below y293 in this x range, the horn stops at x511, the divider starts at
# y329 - so lerping between the clean rows above and below it reproduces the
# ground exactly. The box starts at x530, clear of the horn and short of the
# wordmark's left stem at x563. (Bounds are measured from
# beasthorn-social-preview.png; if the wordmark in beasthorn-banner.svg is ever
# moved or resized, re-measure them.)
$banner = New-Object System.Drawing.Bitmap $bannerIn
$PX0 = 530; $PX1 = 1190; $PY0 = 215; $PY1 = 310
for ($x = $PX0; $x -le $PX1 -and $x -lt $banner.Width; $x++) {
  $cTop = $banner.GetPixel($x, $PY0)
  $cBot = $banner.GetPixel($x, $PY1)
  for ($y = $PY0 + 1; $y -lt $PY1; $y++) {
    $t = ($y - $PY0) / ($PY1 - $PY0)
    $r = [int]($cTop.R + ($cBot.R - $cTop.R) * $t)
    $gg = [int]($cTop.G + ($cBot.G - $cTop.G) * $t)
    $bb = [int]($cTop.B + ($cBot.B - $cTop.B) * $t)
    $banner.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($r, $gg, $bb))
  }
}
Write-Output ("  painted out serif wordmark in x " + $PX0 + "-" + $PX1 + ", y " + $PY0 + "-" + $PY1)
# No scrim: the banner's title slot is already on plain ground.
Add-NameOverlay $banner 10 $false 875 222
$banner.Save($bannerOut, [System.Drawing.Imaging.ImageFormat]::Png)
$banner.Dispose()
Write-Output ("  wrote " + $bannerOut + "   1280x640")

$atlas.Dispose()
