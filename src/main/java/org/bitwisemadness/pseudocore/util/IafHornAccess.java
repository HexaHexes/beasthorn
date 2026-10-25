package org.bitwisemadness.pseudocore.util;

import com.iafenvoy.iceandfire.item.component.DragonHornComponent;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Reads Ice and Fire's Dragon Horn storage.
 *
 * <p>Never reference IaF types outside this class — the mixin is the only
 * caller, and it is skipped entirely when IaF is absent.</p>
 *
 * <p>Works by scanning the stack's components for a {@link DragonHornComponent}
 * value rather than looking IaF's {@code DRAGON_HORN} field up by name. The
 * field approach was tried first and failed silently: its static type differs
 * across CE builds (an Architectury {@code RegistrySupplier} in 2.0-beta.15, a
 * NeoForge {@code DeferredHolder} in 2.1.4), so a direct reference cannot be
 * compiled safely, and a reflective lookup funnels every possible failure into
 * one swallowed exception that produced no diagnostic at all.</p>
 *
 * <p>Scanning for the value type sidesteps all of it: no field, no descriptor,
 * no version coupling. If a Dragon Horn holds a dragon, one of its components
 * <i>is</i> a {@code DragonHornComponent}, and matching on the value is exact
 * rather than name-based.</p>
 */
public final class IafHornAccess {

    private IafHornAccess() {
    }

    /**
     * The stored dragon, or null when this stack is an empty Dragon Horn.
     *
     * <p>Because this mixin only ever runs on IaF's {@code DragonHornItem}, a
     * null return unambiguously means "no dragon stored" rather than "not a
     * Dragon Horn", so callers can treat it as the empty case.</p>
     */
    public static DragonHornComponent read(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        // ItemStack.getComponents() is a DataComponentMap, which is Iterable over
        // its TypedDataComponent entries - the full set, not just the patch.
        for (TypedDataComponent<?> component : stack.getComponents()) {
            if (component.value() instanceof DragonHornComponent horn) {
                return horn;
            }
        }
        return null;
    }
}
