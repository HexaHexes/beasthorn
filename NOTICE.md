Beast Horn - Third-Party Credits and Notices
============================================

This mod embeds modified copies of assets originally created for other projects,
redistributed under the terms of their respective licenses.

---------------------------------------------------------------------------
Ice and Fire / Ice and Fire CE
---------------------------------------------------------------------------

The Beast Horn's model silhouette and horn body texture are derived from the
Dragon Horn texture shipped with "Ice and Fire CE"
(https://www.curseforge.com/minecraft/mc-mods/iceandfire-ce)
by IAFEnvoy, itself derived from "Ice and Fire" by Raptorfarian and Alexthe666.

Upstream project:  https://github.com/IAFEnvoy/IceAndFire-CE
License:           GNU Lesser General Public License v2.1 (LGPL-2.1)
Copyright:         Raptorfarian, Alexthe666, IAFEnvoy, and contributors.

Modifications made by Beast Horn:
  * The texture was split into two layers. "beast_horn_body.png" retains the
    horn body; the dragon-type colour orbs were removed from it.
  * "beast_horn_orbs.png" is a new layer containing only those orb pixels, in
    pure white, so they can be tinted at runtime to another mob's colour.
  * The in-game dragon-type colouring behaviour was removed.

The full, unmodified source of the Dragon Horn texture is available in the
Ice and Fire CE repository listed above.

This mod is not affiliated with or endorsed by IAFEnvoy, Raptorfarian,
Alexthe666, or the Ice and Fire project.

---------------------------------------------------------------------------
Ice and Fire CE - compatibility mixin
---------------------------------------------------------------------------

When Ice and Fire CE is installed, this mod applies a runtime mixin to
com.iafenvoy.iceandfire.item.DragonHornItem. No Ice and Fire source code is
copied or redistributed by this mod; the mixin is applied to the user's own
copy of Ice and Fire CE at load time, consistent with LGPL-2.1's provisions
for linking and runtime modification. The mixin's complete source is included
unmodified in this mod's published source.

---------------------------------------------------------------------------
Other
---------------------------------------------------------------------------

Minecraft, NeoForge, Tensura, and ManasCore remain the property of their
respective authors. Beast Horn contains no code from those projects; Tensura
and ManasCore integration uses their public API only, and neither is required
for this mod to load.
