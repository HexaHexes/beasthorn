package org.bitwisemadness.pseudocore.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.item.recipe.CoreUpgradeRecipe;

/**
 * The mod's recipe serializers (#113).
 *
 * <p>JSON recipe {@code type} fields dispatch through the recipe <b>serializer</b>
 * registry (verified against the 1.21.1 {@code RecipeManager} /
 * {@code Recipe.CODEC}), while recipes bucket into the recipe book by
 * {@code Recipe.getType()} - which {@code CraftingRecipe} defaults to
 * {@code CRAFTING}. So this registry is all the wiring a custom shaped
 * recipe needs: the recipes stay shaped recipes everywhere they are
 * consumed, and the serializer adds the one behaviour vanilla lacks -
 * carrying the core's components to the result.</p>
 *
 * <p>Registered on the mod bus from the main mod class, before any data
 * pack is parsed.</p>
 */
public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Pseudocore.MOD_ID);

    /** The core-upgrade recipe: cross of next-tier ingots, contents preserved. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CoreUpgradeRecipe>> CORE_UPGRADE =
            SERIALIZERS.register("core_upgrade", CoreUpgradeRecipe.Serializer::new);

    private ModRecipes() {
    }
}
