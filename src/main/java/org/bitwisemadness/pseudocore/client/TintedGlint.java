package org.bitwisemadness.pseudocore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.registry.ModDataComponents;
import org.bitwisemadness.pseudocore.util.IafHornAccess;

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
 * vanilla's own shaders), and the RenderType claims a fixed render buffer
 * through {@code RegisterRenderBuffersEvent} - without that slot its geometry
 * would be dropped by BufferSource's shared path, which is the silent no-glint
 * failure #67 root-caused. If either registration is unavailable,
 * {@link #arm} refuses to switch the glint on and the item simply renders
 * without one - a missing shimmer, never a broken render.</p>
 */
public final class TintedGlint {

    /** Vanilla's glint batching: 1536 bytes, same as RenderType.GLINT. */
    private static final int GLINT_BUFFER_SIZE = 1536;

    /** Set by the shader registration; null until then, or after a failed load. */
    private static ShaderInstance shader;

    /**
     * Built exactly once and never rebuilt. The ShaderStateShard holds a lazy
     * supplier ({@code () -> shader}), so the instance outlives shader reloads
     * on its own - and it has to: NeoForge's fixed-buffer map is keyed by this
     * very instance, so a rebuild would desync the registered key from the type
     * the foil pick returns, and the glint would vanish into the shared-buffer
     * path again (#67).
     */
    private static RenderType type;

    /** Set when the RenderType build failed, so we do not retry every frame. */
    private static boolean typeFailed;

    /**
     * True once this instance is registered as a fixed render buffer through
     * {@code RegisterRenderBuffersEvent}. Until then it must not be handed to a
     * buffer pick: an unregistered RenderType lands on BufferSource's shared
     * {@code lastSharedType} path, which flushes it eagerly - while still empty -
     * so its geometry is dropped before a single quad is written, silently and
     * without error (#67). Registered types flush in the fixed sweep, after the
     * item's depth exists for the EQUAL_DEPTH_TEST the glint recipe needs.
     */
    private static boolean registeredFixedBuffer;

    /**
     * Armed by the mixin right before vanilla picks a foil buffer, consumed
     * inside that pick. Plain field: the mixin and this class run on the render
     * thread only, and every render sets it fresh before use.
     */
    private static boolean pending;

    /**
     * Glint-chain diagnostics for #67: the first few pseudocore renders log the
     * full arm state, and the first tinted pick logs once. One-shot by
     * construction, so a live session writes at most a handful of INFO lines
     * and the invisible-failure chain (shader? type? filled? picked?) becomes
     * readable from latest.log. Safe to delete once #67 is understood; nothing
     * behavioural depends on it.
     */
    private static int diagnosticsRemaining = 8;

    /** Whether the one-shot "tinted type was picked" line has been written. */
    private static boolean loggedTintedPick;

    /** Whether the one-shot "shader registered" line has been written. */
    private static boolean loggedShaderRegistration;

    private TintedGlint() {
    }

    /** Called by the shader registration event, including on shader reload. */
    public static void setShader(ShaderInstance instance) {
        // The RenderType is deliberately NOT dropped here: it must stay the
        // exact instance registered in the fixed-buffer map (#67), and its
        // ShaderStateShard supplier already resolves the live field per draw,
        // so a reload reaches it without a rebuild.
        shader = instance;
        if (!loggedShaderRegistration) {
            loggedShaderRegistration = true;
            Pseudocore.LOGGER.info("Glint debug: tinted glint shader registered ({})",
                    instance == null ? "null" : instance.getName());
        }
    }

    /**
     * Registers the tinted glint as a dedicated fixed render buffer, giving it
     * its own arena in {@code BufferSource} and a place in the final
     * {@code endBatch()} sweep - the flush position vanilla's own glint types
     * enjoy, after the item geometry has written the depth the glint recipe's
     * EQUAL_DEPTH_TEST compares against. Without this the type falls onto the
     * shared-buffer path, whose eager flush drops its geometry before any quad
     * is written: no draw, no GL error, no log line - just no glint (#67).
     *
     * <p>Called from {@code RegisterRenderBuffersEvent} (client mod bus), which
     * fires after vanilla's own buffers are seeded. The supplier-based shader
     * state means the type can be built here, before the shader itself
     * registers during the first resource reload.</p>
     */
    public static void registerRenderBuffer(RegisterRenderBuffersEvent event) {
        RenderType built = type();
        if (built == null) {
            Pseudocore.LOGGER.warn("Could not build the tinted glint RenderType; "
                    + "filled pseudocores will render without a glint");
            return;
        }
        try {
            event.registerRenderBuffer(built);
            registeredFixedBuffer = true;
            Pseudocore.LOGGER.info("Glint debug: tinted glint registered as a fixed render buffer");
        } catch (RuntimeException e) {
            Pseudocore.LOGGER.warn("Could not register the tinted glint render buffer; "
                    + "filled pseudocores will render without a glint", e);
        }
    }

    /**
     * Whether the tinted glint can be shown at all. False before shader
     * registration and before the fixed-buffer registration, in which case
     * items fall back to no glint rather than vanilla's violet one.
     */
    public static boolean available() {
        return shader != null && registeredFixedBuffer && type() != null;
    }

    /**
     * Prepares the glint for one render: returns true when this stack is a
     * filled pseudocore and the tinted glint is available, which both arms the
     * pending flag for the buffer pick and tells the mixin to make vanilla
     * take the foil path (its own {@code isFoil} stays false, so a mixin that
     * never applies leaves these items glintless - never violet).
     */
    public static boolean arm(ItemStack stack) {
        boolean filled = isFilledPseudocore(stack);
        boolean hasShader = shader != null;
        boolean hasType = type() != null && registeredFixedBuffer;
        pending = hasShader && hasType && filled;
        // #67 diagnostics: only pseudocore items are interesting, and only the
        // first few renders, so a session writes a handful of lines and no more.
        if (isPseudocoreItem(stack) && diagnosticsRemaining > 0) {
            diagnosticsRemaining--;
            Pseudocore.LOGGER.info(
                    "Glint debug: arm (pseudocore) filled={} shader={} registered={} -> pending={} [{}]",
                    filled, hasShader, hasType, pending,
                    BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        return pending;
    }

    /** Consumes the armed flag: tinted type when armed, vanilla's pick otherwise. */
    public static RenderType takePending(RenderType vanilla) {
        if (!pending) {
            return vanilla;
        }
        pending = false;
        if (!loggedTintedPick) {
            loggedTintedPick = true;
            // #67 probe: what the GL/render state actually is at the moment the
            // tinted type is handed over - pending GL errors (a non-zero code
            // means an earlier call already failed), the global shader colour
            // (a black value would make the SRC_COLOR glint blend add nothing),
            // and the glint alpha the program's fade multiplies by.
            float[] colour = RenderSystem.getShaderColor();
            Pseudocore.LOGGER.info(
                    "Glint debug: tinted pick; glError={} shaderColor={},{},{} glintAlpha={}",
                    org.lwjgl.opengl.GL11.glGetError(),
                    colour[0], colour[1], colour[2],
                    RenderSystem.getShaderGlintAlpha());
        }
        RenderType tinted = registeredFixedBuffer ? type() : null;
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
        // Either line's storage counts: the beast line writes CONTAINED_MOB, a
        // dragon core stores it in Ice and Fire's horn component instead (#65).
        if (!stack.has(ModDataComponents.CONTAINED_MOB.get())
                && IafHornAccess.storedDragonType(stack) == null) {
            return false;
        }
        return isPseudocoreItem(stack);
    }

    /**
     * Whether the stack is a registered pseudocore of either line, by item id
     * rather than by item class: {@code DragonPseudocoreItem} extends Ice and
     * Fire's horn, so an instanceof would try to load IaF on a client that
     * does not have it - while the id is there whatever is installed.
     */
    private static boolean isPseudocoreItem(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return Pseudocore.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith("_pseudocore");
    }

    /**
     * The tinted glint RenderType: vanilla's GLINT recipe with a shader that
     * reads Color. Built once, early (its shader state is a lazy supplier, so
     * no shader is needed yet) and cached forever - the fixed-buffer map holds
     * this exact instance (#67). Any failure is remembered so a broken build
     * degrades to "no glint" instead of attempting and throwing every frame.
     */
    private static RenderType type() {
        if (type == null && !typeFailed) {
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
