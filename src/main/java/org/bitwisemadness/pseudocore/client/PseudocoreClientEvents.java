package org.bitwisemadness.pseudocore.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.registry.ModDataComponents;
import org.bitwisemadness.pseudocore.registry.ModItems;
import org.bitwisemadness.pseudocore.util.IafHornAccess;

import java.io.IOException;

/**
 * Client-only wiring. Registers the orb tint handler, the empty/filled
 * model predicate, and the tinted glint shader.
 *
 * <p>Split from the tint logic itself so no client-only class is ever loaded
 * on a dedicated server.</p>
 *
 * <p>The bus is not spelled out: {@code AutomaticEventSubscriber} routes each
 * listener from its event type, and both events here are {@code IModBusEvent}s,
 * so they are registered on the mod bus automatically. (The annotation's own
 * {@code bus} attribute is deprecated for removal precisely because this
 * detection replaced it.)</p>
 */
@EventBusSubscriber(modid = Pseudocore.MOD_ID, value = Dist.CLIENT)
public final class PseudocoreClientEvents {

    private PseudocoreClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // Every tier shares the two-layer texture, so every tier needs the same
        // handler: layer0 tints to the captured mob, layer1 is the metal. The
        // per-metal body colours arrive separately; without the registration the
        // orbs of a new tier would simply never take a colour.
        event.register(PseudocoreTintHandler.TINT, ModItems.all().toArray(Item[]::new));
    }

    /**
     * Registers the item-model predicate behind the empty/filled orb swap (#53):
     * {@code pseudocore:filled} reads 1 while the stack holds a captured mob,
     * so the base model's override picks the glossy {@code _filled} twin; an
     * empty horn reads 0 and keeps the dull empty art. Vanilla's
     * {@link ItemProperties} is the only registration path (NeoForge ships no
     * mod-bus event for item properties), enqueued so it runs off the event
     * thread before the first model render.
     *
     * <p>Both storage formats count (#65): the beast line writes
     * {@code CONTAINED_MOB}, while a dragon core keeps its capture in Ice and
     * Fire's horn component, so asking only the mod's own component left every
     * captured dragon rendering the empty orb art.</p>
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation filled =
                    ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, "filled");
            for (Item item : ModItems.all()) {
                ItemProperties.register(item, filled, (stack, level, entity, seed) ->
                        stack.has(ModDataComponents.CONTAINED_MOB.get())
                                || IafHornAccess.storedDragonType(stack) != null ? 1.0F : 0.0F);
            }
        });
    }

    /**
     * Registers the tinted glint program (#55): vanilla's glint scroll, blend
     * and fog, with a Color attribute so the shimmer multiplies by the quad's
     * own colour - the same {@code ItemColors} result the orbs render with.
     * NeoForge's event hands the instance to vanilla's shader lifecycle, and
     * the consumer re-publishes it on reload. A load failure is caught here
     * (the bus invokes listeners reflectively, so no checked exception may
     * escape) and simply leaves the glint unavailable - see {@link TintedGlint}.
     */
    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            ShaderInstance shader = new ShaderInstance(
                    event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(Pseudocore.MOD_ID, "tinted_glint"),
                    // Must match TintedGlint's RenderType format: Position + UV0 + Color.
                    DefaultVertexFormat.POSITION_TEX_COLOR);
            TintedGlint.setShader(shader);
            event.registerShader(shader, TintedGlint::setShader);
        } catch (IOException e) {
            Pseudocore.LOGGER.warn("Could not load the tinted glint shader; "
                    + "filled pseudocores will render without a glint", e);
        }
    }
}
