package org.bitwisemadness.pseudocore.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.registry.ModDataComponents;

/**
 * The tinted enchantment glint for filled Pseudocores (#55): a RenderType that
 * is vanilla's item glint in every respect - same enchanted-glint texture, same
 * scrolling texturing shard, same additive blend and depth state - except its
 * vertex format carries a Color attribute, so the shimmer multiplies by the
 * colour each quad already carries.
 *
 * <p>That colour is not resolved here on purpose. {@code ItemRenderer} computes
 * every quad's colour from {@code ItemColors} - which is this mod's own tint
 * handler - and passes it into the vertex stream ({@code putBulkData}), so a
 * Color-aware glint reads the orb colour for orb quads and the metal colour for
 * body quads straight out of the one colour pipeline. There is no second
 * colour path to drift out of sync: empty horns never glint at all (see the
 * mixin), and filled horns shimmer in exactly the colour their orbs show.</p>
 *
 * <p>Everything here is client-only: this class is referenced from the client
 * mixin config and from {@link PseudocoreClientEvents} only, so a dedicated
 * server never loads it.</p>
 *
 * <p>Failure is always cosmetic. The shader registers through
 * {@code RegisterShadersEvent} (resource reloads manage its lifecycle like
 * vanilla's own shaders); if it is ever unavailable, {@link #arm} refuses to
 * switch the glint on and the item simply renders without one - a missing
 * shimmer, never a broken render.</p>
 */
public final class TintedGlint {

    /** Vanilla's glint batching: 1536 bytes, same as RenderType.GLINT. */
    private static final int GLINT_BUFFER_SIZE = 1536;

    /** Set by the shader registration; null until then, or after a failed load. */
    private static ShaderInstance shader;

    /** Lazily built once the shader is known; null when glint is unavailable. */
    private static RenderType type;

    /** Set when the RenderType build failed, so we do not retry every frame. */
    private static boolean typeFailed;

    /**
     * Armed by the mixin right before vanilla picks a foil buffer, consumed
     * inside that pick. Plain field: the mixin and this class run on the render
     * thread only, and every render sets it fresh before use.
     */
    private static boolean pending;

    private TintedGlint() {
    }

    /** Called by the shader registration event, including on shader reload. */
    public static void setShader(ShaderInstance instance) {
        shader = instance;
        // A reload hands us a fresh program; drop the RenderType bound to the old
        // supplier state so the next frame rebuilds against the live shader.
        type = null;
        typeFailed = false;
    }

    /**
     * Whether the tinted glint can be shown at all. False before shader
     * registration and after any load failure, in which case items fall back
     * to no glint rather than vanilla's violet one.
     */
    public static boolean available() {
        return shader != null && type() != null;
    }

    /**
     * Prepares the glint for one render: returns true when this stack is a
     * filled pseudocore and the tinted glint is available, which both arms the
     * pending flag for the buffer pick and tells the mixin to make vanilla
     * take the foil path (its own {@code isFoil} stays false, so a mixin that
     * never applies leaves these items glintless - never violet).
     */
    public static boolean arm(ItemStack stack) {
        pending = shader != null && type() != null && isFilledPseudocore(stack);
        return pending;
    }

    /** Consumes the armed flag: tinted type when armed, vanilla's pick otherwise. */
    public static RenderType takePending(RenderType vanilla) {
        if (!pending) {
            return vanilla;
        }
        pending = false;
        RenderType tinted = type();
        return tinted != null ? tinted : vanilla;
    }

    /** Drops an armed flag that never reached a buffer pick. */
    public static void clearPending() {
        pending = false;
    }

    /**
     * A registered pseudocore item (either line) holding a captured mob. The
     * check is by item id rather than by item class: {@code DragonPseudocoreItem}
     * extends Ice and Fire's horn, so an instanceof would try to load IaF on a
     * client that does not have it - while the id is there whatever is installed.
     */
    public static boolean isFilledPseudocore(ItemStack stack) {
        if (!stack.has(ModDataComponents.CONTAINED_MOB.get())) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return Pseudocore.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith("_pseudocore");
    }

    /**
     * The tinted glint RenderType: vanilla's GLINT recipe with a shader that
     * reads Color. Built on first use (the shader only exists after client
     * setup) and cached; any failure is remembered so a broken build degrades
     * to "no glint" instead of attempting and throwing every frame.
     */
    private static RenderType type() {
        if (type == null && !typeFailed && shader != null) {
            try {
                type = RenderType.create(
                        "pseudocore_tinted_glint",
                        // Position + UV0 + Color: the glint's own scrolling UVs
                        // plus the quad colour ItemRenderer already computed from
                        // ItemColors. (There is no POSITION_COLOR_TEX in 1.21.1;
                        // POSITION_TEX_COLOR carries the same three attributes.)
                        DefaultVertexFormat.POSITION_TEX_COLOR,
                        VertexFormat.Mode.QUADS,
                        GLINT_BUFFER_SIZE,
                        RenderType.CompositeState.builder()
                                .setShaderState(new RenderStateShard.ShaderStateShard(() -> shader))
                                .setTextureState(new RenderStateShard.TextureStateShard(
                                        ItemRenderer.ENCHANTED_GLINT_ITEM, true, false))
                                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                                .setCullState(RenderStateShard.NO_CULL)
                                .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                                .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                                .setTexturingState(RenderStateShard.GLINT_TEXTURING)
                                .createCompositeState(false));
            } catch (RuntimeException e) {
                typeFailed = true;
                Pseudocore.LOGGER.warn("Could not build the tinted glint RenderType; "
                        + "filled pseudocores will render without a glint", e);
            }
        }
        return type;
    }
}
