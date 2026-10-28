package org.bitwisemadness.pseudocore.mixin;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.bitwisemadness.pseudocore.client.TintedGlint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The tinted per-mob glint switch for filled Pseudocores (#55).
 *
 * <p>Vanilla 1.21.1 has no per-stack glint colour hook: {@code Item#isFoil}
 * only toggles the hardcoded violet of {@code RenderType.glint()}. So the
 * selection is intercepted here, in two steps, both scoped to
 * {@link ItemRenderer}:</p>
 *
 * <ol>
 * <li>In {@code render}, the {@code ItemStack.hasFoil()} reads are redirected:
 * a filled pseudocore arms {@link TintedGlint} and reports true, so vanilla
 * takes its foil path. Every other stack passes through untouched - and
 * because {@code Item#isFoil} itself is never overridden, a mixin that fails
 * to apply simply leaves pseudocores with no glint at all, exactly the
 * failure mode #55 requires.</li>
 * <li>Inside {@code getFoilBuffer}/{@code getFoilBufferDirect} - entered
 * immediately afterwards - the {@code RenderType.*glint*} picks are redirected
 * to the armed tinted type. The arm/consume pair is one straight-line stretch
 * of the same render call, so the flag can never leak to another item.</li>
 * </ol>
 *
 * <p>This class lives in the client mixin config, whose {@code client} array
 * keeps it off a dedicated server, and that config is {@code required:false}
 * so a target that disappears in some future mapping never fails at load time.
 * Degradation order: no shader -&gt; {@link TintedGlint} refuses to arm -&gt;
 * no glint; no mixin -&gt; {@code isFoil} stays false -&gt; no glint. A
 * missing shimmer is cosmetic; a crashed inventory render is not.</p>
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    /** ItemRenderer#render(ItemStack, ItemDisplayContext, boolean, PoseStack, MultiBufferSource, int, int, BakedModel). */
    private static final String RENDER =
            "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;"
                    + "ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "IILnet/minecraft/client/resources/model/BakedModel;)V";

    private static final String FOIL_BUFFER =
            "(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;";

    /** Drop any flag a previous render failed to consume. */
    @Inject(method = RENDER, at = @At("HEAD"))
    private void pseudocore$clearPendingGlint(CallbackInfo ci) {
        TintedGlint.clearPending();
    }

    /**
     * Makes a filled pseudocore take vanilla's foil path while arming the
     * tinted substitution; every other stack keeps vanilla's own answer.
     */
    @Redirect(method = RENDER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"))
    private boolean pseudocore$tintedFoil(ItemStack stack) {
        return TintedGlint.arm(stack) || stack.hasFoil();
    }

    /** The common pick inside both foil helpers (skipFoil variants). */
    @Redirect(method = {"getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            "getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;glint()Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType pseudocore$tintedGlint() {
        return TintedGlint.takePending(RenderType.glint());
    }

    /** The translucent-sheet variant (used when the base layer is translucent). */
    @Redirect(method = "getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;"
                    + "glintTranslucent()Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType pseudocore$tintedGlintTranslucent() {
        return TintedGlint.takePending(RenderType.glintTranslucent());
    }

    /** The buffered entity variant of the non-direct helper. */
    @Redirect(method = "getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entityGlint()Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType pseudocore$tintedEntityGlint() {
        return TintedGlint.takePending(RenderType.entityGlint());
    }

    /** The direct variant of the direct helper. */
    @Redirect(method = "getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;"
                    + "entityGlintDirect()Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType pseudocore$tintedEntityGlintDirect() {
        return TintedGlint.takePending(RenderType.entityGlintDirect());
    }
}
