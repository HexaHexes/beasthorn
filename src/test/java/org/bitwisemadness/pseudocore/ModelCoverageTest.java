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
 * Every pseudocore item must have an item model. This is the one bug class the
 * game tests cannot see: item models are client-only assets, invisible to
 * {@code runGameTestServer}, so a shipped item without a model renders as the
 * missing-model checkerboard in game and nothing in CI blinks - which is exactly
 * how every tier but iron went out broken (issue #33).
 *
 * <p>So the check is file-based instead, on the assets this repo ships: every
 * {@code item.pseudocore.<id>} lang key must have a matching
 * {@code models/item/<id>.json} carrying the two tint layers. The junit source
 * set has no Minecraft or gson classes, hence the plain regex over the lang file
 * rather than a JSON parse - the keys are flat and one per line, which the test
 * also relies on when counting them.</p>
 */
class ModelCoverageTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/pseudocore");
    private static final String ITEM_KEY_PREFIX = "item.pseudocore.";

    @Test
    void everyItemLangKeyHasAModelFileWithBothLayers() throws IOException {
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

        Path models = ASSETS.resolve("models/item");
        for (String id : ids) {
            Path model = models.resolve(id + ".json");
            assertTrue(Files.exists(model),
                    "no item model for " + id + " - the item renders as a missing model in game");
            String content = Files.readString(model);
            assertTrue(content.contains("\"layer0\"") && content.contains("\"layer1\""),
                    id + "'s model must carry both tint layers (layer0 orbs, layer1 body)");
        }
    }
}
