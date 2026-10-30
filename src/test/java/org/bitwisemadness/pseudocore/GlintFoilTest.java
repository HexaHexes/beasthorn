package org.bitwisemadness.pseudocore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The glint in its simplified form (#86): a filled core takes vanilla's own
 * foil path through {@code Item#isFoil} overrides - no custom shader, no custom
 * RenderType, no client mixin. All file-based, in the style of
 * {@link ModelCoverageTest}, because the junit source set has no Minecraft
 * classes and the client render plumbing is invisible to
 * {@code runGameTestServer}.
 *
 * <p>Two directions are pinned: the wiring that must exist (the overrides and
 * their storage checks) and the wiring that must stay dead (the #55/#67
 * machinery was deleted, and a silent resurrection of any part of it is
 * exactly the failure mode that cost #67 four diagnostic rounds).</p>
 */
class GlintFoilTest {

    private static final Path MODS_TOML =
            Path.of("src/main/templates/META-INF/neoforge.mods.toml");
    private static final Path BEAST_ITEM = Path.of(
            "src/main/java/org/bitwisemadness/pseudocore/item/PseudocoreItem.java");
    private static final Path DRAGON_ITEM = Path.of(
            "src/main/java/org/bitwisemadness/pseudocore/item/DragonPseudocoreItem.java");

    /** The whole feature: override isFoil when the stack holds a capture. */
    @Test
    void bothLinesOverrideIsFoilOnTheirOwnStorage() throws IOException {
        String beast = Files.readString(BEAST_ITEM);
        assertTrue(beast.contains("public boolean isFoil(ItemStack stack)"),
                "PseudocoreItem must override isFoil - it is the entire glint feature (#86)");
        assertTrue(beast.contains("stack.has(ModDataComponents.CONTAINED_MOB.get())"),
                "the beast foil check must read CONTAINED_MOB, the beast line's own storage");

        String dragon = Files.readString(DRAGON_ITEM);
        assertTrue(dragon.contains("public boolean isFoil(ItemStack stack)"),
                "DragonPseudocoreItem must override isFoil (#86)");
        assertTrue(dragon.contains("IafHornAccess.read(stack)"),
                "the dragon foil check must read through IafHornAccess - the dragon line "
                        + "stores in Ice and Fire's horn component, not CONTAINED_MOB (#65)");
        assertTrue(dragon.contains("super.isFoil(stack)"),
                "both overrides must fall through to super so enchanted stacks keep "
                        + "vanilla behaviour");
    }

    /**
     * The deleted machinery must stay deleted: each part that silently failing
     * was invisible in #67, and the simplest defence is the absence of the code
     * plus the absence of its registrations.
     */
    @Test
    void customGlintMachineryStaysDeleted() {
        String[] gone = {
                "src/main/resources/pseudocore.client.mixins.json",
                "src/main/java/org/bitwisemadness/pseudocore/mixin/ItemRendererMixin.java",
                "src/main/java/org/bitwisemadness/pseudocore/client/TintedGlint.java",
                "src/main/resources/assets/pseudocore/shaders/core/tinted_glint.json",
                "src/main/resources/assets/pseudocore/shaders/core/tinted_glint.vsh",
                "src/main/resources/assets/pseudocore/shaders/core/tinted_glint.fsh"};
        for (String path : gone) {
            assertFalse(Files.exists(Path.of(path)),
                    "deleted glint machinery is back: " + path
                            + " - the foil path must stay vanilla (#86)");
        }
    }

    @Test
    void modsTomlRegistersNoClientMixinConfig() throws IOException {
        String toml = Files.readString(MODS_TOML);
        assertTrue(!toml.contains("pseudocore.client.mixins.json"),
                "mods.toml still registers the deleted client mixin config (#86)");
        assertTrue(toml.contains("pseudocore.iaf.mixins.json"),
                "the IaF mixin config must stay registered - it is unrelated to the glint");
    }

    /**
     * The #65 read-path assertions that outlive the glint rewrite: the filled
     * model predicate and the orb tint still read both storage formats, and no
     * client class imports Ice and Fire directly.
     */
    @Test
    void dragonStorageReadsAndLayeringSurviveTheRewrite() throws IOException {
        String events = Files.readString(Path.of(
                "src/main/java/org/bitwisemadness/pseudocore/client/PseudocoreClientEvents.java"));
        assertTrue(events.contains("IafHornAccess.storedDragonType"),
                "the pseudocore:filled item property must accept a dragon core (#65)");

        String tint = Files.readString(Path.of(
                "src/main/java/org/bitwisemadness/pseudocore/client/PseudocoreTintHandler.java"));
        assertTrue(tint.contains("IafHornAccess.storedDragonType")
                        && tint.contains("IafHornAccess.storedDragonData"),
                "the orb tint must read a dragon core's element and variant (#65)");

        for (String clientSource : new String[]{
                "src/main/java/org/bitwisemadness/pseudocore/client/PseudocoreClientEvents.java",
                "src/main/java/org/bitwisemadness/pseudocore/client/PseudocoreTintHandler.java"}) {
            String source = Files.readString(Path.of(clientSource));
            assertTrue(!source.contains("import com.iafenvoy"),
                    "an IaF import crept into a client class: " + clientSource);
        }
    }
}
