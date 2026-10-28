package org.bitwisemadness.pseudocore;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The embedded "Metallic Bodies" resource pack (#56) must be complete and must
 * stay narrow. The pack lives outside {@code assets/} on purpose, so no other
 * CI check sees it: a missing or malformed pack.mcmeta makes the pack silently
 * vanish from the Resource Packs screen, and a stray override would silently
 * change art the player never opted into. Both failures are invisible to
 * {@code runGameTestServer}, hence this file-based junit guard in the style of
 * {@link ModelCoverageTest}.
 */
class MetallicPackTest {

    private static final Path PACK = Path.of("src/main/resources/metallic");
    private static final Path BASE = Path.of("src/main/resources/assets/pseudocore/textures/item");

    /** The two body overrides, and nothing else, may ship in the pack. */
    private static final String[] OVERRIDES = {
            "pseudocore_body.png",
            "dragon_pseudocore_body.png"};

    @Test
    void packMetaAndIconExist() throws IOException {
        assertTrue(Files.exists(PACK.resolve("pack.mcmeta")),
                "pack.mcmeta missing - the pack cannot be listed on the Resource Packs screen");
        String meta = Files.readString(PACK.resolve("pack.mcmeta"));
        assertTrue(meta.contains("\"pack_format\""),
                "pack.mcmeta must declare a pack_format");
        assertTrue(Files.exists(PACK.resolve("pack.png")),
                "pack.png missing - the pack lists without an icon");
    }

    @Test
    void packOverridesExactlyTheTwoBodies() throws IOException {
        for (String name : OVERRIDES) {
            Path override = PACK.resolve("assets/pseudocore/textures/item").resolve(name);
            assertTrue(Files.exists(override), "missing pack override: " + name);
            Path base = BASE.resolve(name);
            assertTrue(Files.exists(base), "the base texture the pack overrides is gone: " + name);
            assertArrayEquals(pngHeader(base), pngHeader(override),
                    name + " override must be a PNG");
            assertTrue(!java.util.Arrays.equals(Files.readAllBytes(base), Files.readAllBytes(override)),
                    name + " override is byte-identical to the base - the enhancement pass was never applied");
        }
    }

    @Test
    void packHidesNoExtraOverrides() throws IOException {
        Path assets = PACK.resolve("assets");
        try (var stream = Files.walk(assets)) {
            long extra = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !java.util.Arrays.asList(OVERRIDES).contains(p.getFileName().toString()))
                    .count();
            assertEquals(0, extra,
                    "the pack may override only the two body textures - orb art belongs to #53");
        }
    }

    /** The first eight bytes of a PNG: fixed magic, enough to prove the format. */
    private static byte[] pngHeader(Path png) throws IOException {
        byte[] bytes = Files.readAllBytes(png);
        byte[] header = new byte[Math.min(8, bytes.length)];
        System.arraycopy(bytes, 0, header, 0, header.length);
        return header;
    }
}
