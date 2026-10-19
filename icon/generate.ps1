# Generates the Beast Horn icon SVG by sweeping a tapering width along a
# centerline. Hand-authored control points kept producing a fat crescent with a
# detached mouth; computing the outline guarantees the body actually converges
# to a tip and that the mouth ellipse matches the tube's own opening chord.
#
# Note: all math is inlined. Passing @() into a function's positional params
# unrolls it across them, which is what broke the earlier version.

$out = "$PSScriptRoot\icon512.svg"

$cx0 = 150.0; $cy0 = 138.0     # mouth centre
$cx1 = 310.0; $cy1 = 186.0
$cx2 = 334.0; $cy2 = 340.0
$cx3 = 182.0; $cy3 = 432.0     # tip
$w0  = 110.0
$taper = 0.75

$N = 90
$ox = New-Object double[] ($N + 1)
$oy = New-Object double[] ($N + 1)
$ix = New-Object double[] ($N + 1)
$iy = New-Object double[] ($N + 1)
$cxm = New-Object double[] ($N + 1)
$cym = New-Object double[] ($N + 1)

for ($i = 0; $i -le $N; $i++) {
  $t  = $i / $N
  $m  = 1.0 - $t
  $px = $m*$m*$m*$cx0 + 3*$m*$m*$t*$cx1 + 3*$m*$t*$t*$cx2 + $t*$t*$t*$cx3
  $py = $m*$m*$m*$cy0 + 3*$m*$m*$t*$cy1 + 3*$m*$t*$t*$cy2 + $t*$t*$t*$cy3
  $dx = 3*$m*$m*($cx1-$cx0) + 6*$m*$t*($cx2-$cx1) + 3*$t*$t*($cx3-$cx2)
  $dy = 3*$m*$m*($cy1-$cy0) + 6*$m*$t*($cy2-$cy1) + 3*$t*$t*($cy3-$cy2)
  $len = [Math]::Sqrt($dx*$dx + $dy*$dy)
  $nx = -$dy / $len
  $ny =  $dx / $len
  $half = $w0 * [Math]::Pow(1.0 - $t, $taper) / 2.0
  $cxm[$i] = $px; $cym[$i] = $py
  $ox[$i] = $px + $nx*$half; $oy[$i] = $py + $ny*$half
  $ix[$i] = $px - $nx*$half; $iy[$i] = $py - $ny*$half
}

$body = "M {0:F1} {1:F1}" -f $ox[0], $oy[0]
for ($i = 1; $i -le $N; $i++) { $body += " L {0:F1} {1:F1}" -f $ox[$i], $oy[$i] }
for ($i = $N; $i -ge 0; $i--) { $body += " L {0:F1} {1:F1}" -f $ix[$i], $iy[$i] }
$body += " Z"

# rim light: follows the outer edge down from the mouth, clipped inside the body
$rimN = 46
$rim = "M {0:F1} {1:F1}" -f $ox[0], $oy[0]
for ($i = 1; $i -le $rimN; $i++) { $rim += " L {0:F1} {1:F1}" -f $ox[$i], $oy[$i] }

# mouth: ellipse spanning exactly the tube's opening chord
$half = $w0 / 2.0
$ang = [Math]::Atan2(($oy[0]-$iy[0]), ($ox[0]-$ix[0])) * 180.0 / [Math]::PI
$mrx = $half
$mry = $half * 0.66

# gold band: quad across the tube at t=0.80
$tB = 0.68
$m  = 1.0 - $tB
$bx = $m*$m*$m*$cx0 + 3*$m*$m*$tB*$cx1 + 3*$m*$tB*$tB*$cx2 + $tB*$tB*$tB*$cx3
$by = $m*$m*$m*$cy0 + 3*$m*$m*$tB*$cy1 + 3*$m*$tB*$tB*$cy2 + $tB*$tB*$tB*$cy3
$bdx = 3*$m*$m*($cx1-$cx0) + 6*$m*$tB*($cx2-$cx1) + 3*$tB*$tB*($cx3-$cx2)
$bdy = 3*$m*$m*($cy1-$cy0) + 6*$m*$tB*($cy2-$cy1) + 3*$tB*$tB*($cy3-$cy2)
$blen = [Math]::Sqrt($bdx*$bdx + $bdy*$bdy)
$bnx = -$bdy/$blen; $bny = $bdx/$blen
$btx = $bdx/$blen;  $bty = $bdy/$blen
$bhw = $w0 * [Math]::Pow(1.0 - $tB, $taper) / 2.0 * 1.38
$bth = 34.0
$band = "M {0:F1} {1:F1} L {2:F1} {3:F1} L {4:F1} {5:F1} L {6:F1} {7:F1} Z" -f `
  ($bx + $bnx*$bhw - $btx*$bth/2), ($by + $bny*$bhw - $bty*$bth/2),
  ($bx - $bnx*$bhw - $btx*$bth/2), ($by - $bny*$bhw - $bty*$bth/2),
  ($bx - $bnx*$bhw + $btx*$bth/2), ($by - $bny*$bhw + $bty*$bth/2),
  ($bx + $bnx*$bhw + $btx*$bth/2), ($by + $bny*$bhw + $bty*$bth/2)

# orbs at three points along the centreline
$orbT  = @(0.24, 0.37, 0.50)
$orbF  = @("url(#o1)", "url(#o2)", "url(#o3)")
$orbE  = @("#0d2c15", "#0b264c", "#360f53")
$orbs = ""
foreach ($k in 0..2) {
  $t = $orbT[$k]; $m = 1.0 - $t
  $px = $m*$m*$m*$cx0 + 3*$m*$m*$t*$cx1 + 3*$m*$t*$t*$cx2 + $t*$t*$t*$cx3
  $py = $m*$m*$m*$cy0 + 3*$m*$m*$t*$cy1 + 3*$m*$t*$t*$cy2 + $t*$t*$t*$cy3
  $orbs += "`n  <circle cx=`"$('{0:F1}' -f $px)`" cy=`"$('{0:F1}' -f $py)`" r=`"31`" fill=`"url(#og)`"/>"
  $orbs += "`n  <circle cx=`"$('{0:F1}' -f $px)`" cy=`"$('{0:F1}' -f $py)`" r=`"19.5`" fill=`"$($orbF[$k])`" stroke=`"$($orbE[$k])`" stroke-width=`"3`"/>"
}

# Shared markup so the icon and the banner are provably the same artwork.
$defs = @"
    <radialGradient id="bg" cx="42%" cy="32%" r="82%">
      <stop offset="0%" stop-color="#2f2419"/><stop offset="56%" stop-color="#1a140e"/><stop offset="100%" stop-color="#0b0805"/>
    </radialGradient>
    <linearGradient id="hornBody" x1="14%" y1="6%" x2="86%" y2="98%">
      <stop offset="0%" stop-color="#b58f60"/><stop offset="30%" stop-color="#8f6e46"/>
      <stop offset="64%" stop-color="#5d4529"/><stop offset="100%" stop-color="#35240e"/>
    </linearGradient>
    <linearGradient id="mouth" x1="20%" y1="10%" x2="86%" y2="94%">
      <stop offset="0%" stop-color="#f2e4c2"/><stop offset="58%" stop-color="#c9ae82"/><stop offset="100%" stop-color="#87704e"/>
    </linearGradient>
    <linearGradient id="gold" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#f7d985"/><stop offset="44%" stop-color="#d9a440"/><stop offset="100%" stop-color="#855b14"/>
    </linearGradient>
    <clipPath id="hornClip"><path d="$body"/></clipPath>
    <radialGradient id="og" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#ffe9b0" stop-opacity="0.6"/>
      <stop offset="48%" stop-color="#ffd98a" stop-opacity="0.09"/>
      <stop offset="100%" stop-color="#ffd98a" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="o1" cx="34%" cy="28%" r="74%">
      <stop offset="0%" stop-color="#b6f8c2"/><stop offset="46%" stop-color="#3ecb5b"/><stop offset="100%" stop-color="#14632a"/>
    </radialGradient>
    <radialGradient id="o2" cx="34%" cy="28%" r="74%">
      <stop offset="0%" stop-color="#b6dcff"/><stop offset="46%" stop-color="#3a8cf0"/><stop offset="100%" stop-color="#0f3a76"/>
    </radialGradient>
    <radialGradient id="o3" cx="34%" cy="28%" r="74%">
      <stop offset="0%" stop-color="#eec4ff"/><stop offset="46%" stop-color="#b14fe6"/><stop offset="100%" stop-color="#49166d"/>
    </radialGradient>
"@

$horn = @"
  <path d="$body" fill="url(#hornBody)" stroke="#2a1c0e" stroke-width="5" stroke-linejoin="round"/>
  <g clip-path="url(#hornClip)"><path d="$rim" fill="none" stroke="#f0dcb4" stroke-width="9" stroke-linecap="round" opacity="0.30"/></g>
  <g clip-path="url(#hornClip)"><path d="$band" fill="url(#gold)" stroke="#55380e" stroke-width="4" stroke-linejoin="round"/></g>
  <ellipse cx="$('{0:F1}' -f $cxm[0])" cy="$('{0:F1}' -f $cym[0])" rx="$('{0:F1}' -f $mrx)" ry="$('{0:F1}' -f $mry)" transform="rotate($('{0:F1}' -f $ang) $('{0:F1}' -f $cxm[0]) $('{0:F1}' -f $cym[0]))" fill="url(#mouth)" stroke="#2a1c0e" stroke-width="5"/>
$orbs
"@

$svg = @"
<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">
  <defs>
$defs
  </defs>
  <rect width="512" height="512" fill="url(#bg)"/>
  <ellipse cx="250" cy="436" rx="122" ry="22" fill="#000" opacity="0.32"/>
$horn
</svg>
"@
[System.IO.File]::WriteAllText($out, $svg)
Write-Output ("  wrote " + $out)

# ---- banner: same horn, wordmark beside it ----
$bannerOut = "$PSScriptRoot\banner1280.svg"
$banner = @"
<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="640" viewBox="0 0 1280 640">
  <defs>
$defs
    <linearGradient id="titleGrad" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#f7e7bd"/><stop offset="55%" stop-color="#e0b559"/><stop offset="100%" stop-color="#a97d24"/>
    </linearGradient>
  </defs>
  <rect width="1280" height="640" fill="url(#bg)"/>
  <g transform="translate(150,26) scale(1.16)">
    <ellipse cx="250" cy="436" rx="122" ry="22" fill="#000" opacity="0.30"/>
$horn
  </g>
  <text x="875" y="292" text-anchor="middle"
        font-family="'Palatino Linotype', Palatino, Georgia, 'Times New Roman', serif"
        font-size="86" letter-spacing="7" fill="url(#titleGrad)">BEAST HORN</text>
  <line x1="675" y1="330" x2="1075" y2="330" stroke="#8a6a3e" stroke-width="2" opacity="0.55"/>
  <text x="875" y="378" text-anchor="middle"
        font-family="'Palatino Linotype', Palatino, Georgia, 'Times New Roman', serif"
        font-size="26" letter-spacing="2.5" fill="#b3a184">Capture any mob &#8212; with all of its NBT</text>
</svg>
"@
[System.IO.File]::WriteAllText($bannerOut, $banner)
Write-Output ("  wrote " + $bannerOut)
Write-Output ("  mouth " + $('{0:F0},{1:F0}' -f $cxm[0],$cym[0]) + "  angle " + $('{0:F1}' -f $ang) + " deg  band " + $('{0:F0},{1:F0}' -f $bx,$by))
