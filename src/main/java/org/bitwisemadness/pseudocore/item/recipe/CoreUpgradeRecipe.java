package org.bitwisemadness.pseudocore.item.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.bitwisemadness.pseudocore.Pseudocore;
import org.bitwisemadness.pseudocore.registry.ModRecipes;

/**
 * A shaped crafting recipe whose result carries the core input's components
 * (#113): the cross of four next-tier ingots around a core, crafted into the
 * next tier's core with its contents intact.
 *
 * <p>Vanilla's shaped recipes cannot do this - 1.21.1 has no
 * {@code crafting_transmute}, and this tree's only {@code copy_components} is
 * a loot function - so this subclass keeps every bit of vanilla's shape
 * machinery (pattern matching, mirroring, the JSON layout datapack authors
 * already know) and overrides exactly the steps a shaped recipe cannot:
 * {@link #assemble} starts
 * from vanilla's result and then copies <b>every</b> data component off the
 * core input, and {@link #getSerializer()} names this recipe's own
 * serializer so anything that <b>writes</b> recipes finds it. The mob
 * itself, EP progress, IaF's horn storage, and the
 * addon's linkage stamp all ride the upgrade - an upgrade is a tier change,
 * not a reset.</p>
 *
 * <p>The core input is found by registry namespace ({@code pseudocore} -
 * {@code ModItems} registers nothing but cores), never by class, so this
 * method loads without Ice and Fire. Both codecs reproduce vanilla's exact
 * JSON and wire shape (verified against the 1.21.1 sources), reading fields
 * through this class's own copies plus the one public one
 * ({@code pattern}) - nothing package-private in {@link ShapedRecipe} is
 * touched from outside its package.</p>
 *
 * <p>{@link #getSerializer()} is the second override, and it is not
 * optional: everything that <b>writes</b> a recipe - datagen (#116) first
 * among them, recipe sync on the network path second - dispatches through
 * the serializer registry via this method, while everything that
 * <b>reads</b> one dispatches on the JSON {@code type} field and never
 * consulted it. Inherited from {@link ShapedRecipe} it returned vanilla's
 * {@code crafting_shaped}, so the first-ever serialization of this recipe
 * (datagen) silently downgraded it to a plain shaped recipe - the
 * component copy would have quietly vanished from the shipped data.</p>
 */
public final class CoreUpgradeRecipe extends ShapedRecipe {

    public static final MapCodec<CoreUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.STRING.optionalFieldOf("group", "")
                            .forGetter(recipe -> recipe.groupValue),
                    CraftingBookCategory.CODEC.fieldOf("category")
                            .orElse(CraftingBookCategory.MISC)
                            .forGetter(recipe -> recipe.categoryValue),
                    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                    ItemStack.STRICT_CODEC.fieldOf("result")
                            .forGetter(recipe -> recipe.resultValue),
                    Codec.BOOL.optionalFieldOf("show_notification", Boolean.TRUE)
                            .forGetter(recipe -> recipe.showNotificationValue))
                    .apply(instance, CoreUpgradeRecipe::new));

    /**
     * Vanilla's wire format exactly: group utf, category enum, pattern,
     * result stack, notification flag - the same five fields in the same
     * order, so a client on either serializer decodes identically.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, CoreUpgradeRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                buf.writeUtf(recipe.groupValue);
                buf.writeEnum(recipe.categoryValue);
                ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.pattern);
                ItemStack.STREAM_CODEC.encode(buf, recipe.resultValue);
                buf.writeBoolean(recipe.showNotificationValue);
            },
            buf -> new CoreUpgradeRecipe(
                    buf.readUtf(),
                    buf.readEnum(CraftingBookCategory.class),
                    ShapedRecipePattern.STREAM_CODEC.decode(buf),
                    ItemStack.STREAM_CODEC.decode(buf),
                    buf.readBoolean()));

    private final String groupValue;
    private final CraftingBookCategory categoryValue;
    private final ItemStack resultValue;
    private final boolean showNotificationValue;

    public CoreUpgradeRecipe(String group, CraftingBookCategory category,
                             ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
        this.groupValue = group;
        this.categoryValue = category;
        this.resultValue = result;
        this.showNotificationValue = showNotification;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        ItemStack core = findCore(input);
        if (core != null) {
            for (TypedDataComponent<?> component : core.getComponents()) {
                @SuppressWarnings("unchecked")
                DataComponentType<Object> type = (DataComponentType<Object>) component.type();
                result.set(type, component.value());
            }
        }
        return result;
    }

    /** The core input by registry namespace; null when the pattern did not put one in. */
    private static ItemStack findCore(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem())
                    .getNamespace().equals(Pseudocore.MOD_ID)) {
                return stack;
            }
        }
        return null;
    }

    /**
     * This recipe's serializer, not the inherited shaped one: readers
     * dispatch on the JSON {@code type} and never needed this, but every
     * <b>writer</b> - datagen (#116) and the recipe sync packet - does,
     * and the inherited value serialized these recipes as plain
     * {@code minecraft:crafting_shaped}, silently dropping the upgrade
     * behaviour from the written data.
     */
    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CORE_UPGRADE.get();
    }

    /** Vanilla's serializer shape, producing this recipe type. */
    public static final class Serializer implements RecipeSerializer<CoreUpgradeRecipe> {
        @Override
        public MapCodec<CoreUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CoreUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
