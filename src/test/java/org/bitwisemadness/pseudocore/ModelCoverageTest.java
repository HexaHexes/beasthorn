package org.bitwisemadness.pseudocore;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every pseudocore item must have an item model, and every model must swap
 * between the empty and filled orb art (#53). These are the bug classes the
 * game tests cannot see: item models are client-only assets, invisible to
 * {@code runGameTestServer}, so a shipped item without a model renders as the
 * missing-model checkerboard in game and nothing in CI blinks - which is exactly
 * how every tier but iron went out broken (issue #33).
 *
 * <p>So the checks are file-based instead, on the assets this repo ships: every
 * {@code item.pseudocore.<id>} lang key must have a matching
 * {@code models/item/<id>.json} carrying the two tint layers, that model must
 * point at empty-state orb art and override on {@code pseudocore:filled} to a
 * {@code <id>_filled} twin carrying both layers over the glossy art, and every
 * texture either side must exist - a missing twin or texture is invisible to CI
 * exactly like a missing base model. The junit source set has no Minecraft or
 * gson classes, hence the plain regex over the lang file rather than a JSON
 * parse - the keys are flat and one per line, which the test also relies on
 * when counting them.</p>
 */
class ModelCoverageTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/pseudocore");
    private static final Path MODELS = ASSETS.resolve("models/item");
    private static final String ITEM_KEY_PREFIX = "item.pseudocore.";

    /** A layer0 texture reference, e.g. {@code "layer0": "pseudocore:item/pseudocore_orbs"}. */
    private static final Pattern LAYER0 =
            Pattern.compile("\"layer0\":\\s*\"pseudocore:item/([a-z0-9_]+)\"");

    @Test
    void everyItemLangKeyHasAModelFileWithBothLayers() throws IOException {
        for (String id : itemIds()) {
            Path model = MODELS.resolve(id + ".json");
            assertTrue(Files.exists(model),
                    "no item model for " + id + " - the item renders as a missing model in game");
            String content = Files.readString(model);
            assertTrue(content.contains("\"layer0\"") && content.contains("\"layer1\""),
                    id + "'s model must carry both tint layers (layer0 orbs, layer1 body)");
        }
    }

    /**
     * The empty/filled swap (#33's bug class, one level deeper): the base model
     * must show the empty art and switch on {@code pseudocore:filled} to a
     * {@code _filled} twin over the glossy art, and both textures must exist.
     * All three failures render silently wrong in game - empty horns looking
     * filled, filled horns never looking filled, or a missing texture
     * checkerboard - and none of them touch a server-side test.
     */
    @Test
    void everyItemModelSwapsBetweenEmptyAndFilledOrbArt() throws IOException {
        for (String id : itemIds()) {
            String base = Files.readString(MODELS.resolve(id + ".json"));
            assertTrue(base.contains("\"pseudocore:filled\""),
                    id + "'s model must switch on the pseudocore:filled predicate (#53)");
            assertTrue(base.contains("pseudocore:item/" + id + "_filled"),
                    id + "'s model must override to its " + id + "_filled twin");

            String emptyTexture = layer0Of(base, id + "'s base model");
            assertTrue(emptyTexture.endsWith("_empty"),
                    id + "'s base layer0 must point at the empty orb art, found " + emptyTexture);
            assertTextureExists(emptyTexture, id + "'s base model");

            Path twin = MODELS.resolve(id + "_filled.json");
            assertTrue(Files.exists(twin),
                    "no filled twin model for " + id
                            + " - a filled horn would render the empty art");
            String filledContent = Files.readString(twin);
            assertTrue(filledContent.contains("\"layer0\"") && filledContent.contains("\"layer1\""),
                    id + "_filled must carry both tint layers (layer0 orbs, layer1 body)");

            String glossyTexture = layer0Of(filledContent, id + "'s filled twin");
            assertTrue(!glossyTexture.endsWith("_empty"),
                    id + "_filled's layer0 must point at the glossy orb art, found "
                            + glossyTexture);
            assertTextureExists(glossyTexture, id + "'s filled twin");
        }
    }

    /** Every item id the lang file names - the same set the model checks cover. */
    private static Set<String> itemIds() throws IOException {
        Pattern key = Pattern.compile(Pattern.quote(ITEM_KEY_PREFIX) + "([a-z0-9_]+)\"");
        Set<String> ids = new HashSet<>();
        for (String line : Files.readAllLines(ASSETS.resolve("lang/en_us.json"))) {
            Matcher matcher = key.matcher(line);
            if (matcher.find()) {
                ids.add(matcher.group(1));
            }
        }
        assertTrue(ids.size() >= 12,
                "the lang file should name at least the twelve beast tiers, found " + ids.size());
        return ids;
    }

    private static String layer0Of(String modelJson, String where) {
        Matcher matcher = LAYER0.matcher(modelJson);
        assertTrue(matcher.find(),
                where + " must reference a pseudocore layer0 texture");
        return matcher.group(1);
    }

    private static void assertTextureExists(String texture, String where) {
        assertTrue(Files.exists(ASSETS.resolve("textures/item/" + texture + ".png")),
                where + " points at a texture missing from the repo: " + texture);
    }
}
