package org.bitwisemadness.pseudocore;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tinted glint (#55) is pure client render plumbing - shaders, a mixin
 * config, and their registrations - so none of its failure modes are visible
 * to {@code runGameTestServer}: a missing shader file or a mixin config that
 * never got registered means the feature silently does not exist in game,
 * exactly the "#33 bug class" this suite exists for. The checks are therefore
 * file-based, in the style of {@link ModelCoverageTest} and
 * {@link MetallicPackTest} - plain text reads, since the junit source set has
 * no Minecraft or gson classes.
 */
class TintedGlintAssetsTest {

    private static final Path SHADERS =
            Path.of("src/main/resources/assets/pseudocore/shaders/core");
    private static final Path PROGRAM = SHADERS.resolve("tinted_glint");
    private static final Path CLIENT_MIXINS =
            Path.of("src/main/resources/pseudocore.client.mixins.json");
    private static final Path MODS_TOML =
            Path.of("src/main/templates/META-INF/neoforge.mods.toml");
    private static final Path MIXIN_SOURCE =
            Path.of("src/main/java/org/bitwisemadness/pseudocore/mixin/ItemRendererMixin.java");
    private static final Path GLINT_SOURCE =
            Path.of("src/main/java/org/bitwisemadness/pseudocore/client/TintedGlint.java");

    @Test
    void shaderProgramFilesExist() {
        for (String ext : new String[]{"json", "vsh", "fsh"}) {
            Path file = PROGRAM.resolveSibling("tinted_glint." + ext);
            assertTrue(Files.exists(file),
                    "missing tinted glint shader file: tinted_glint." + ext
                            + " - shader registration would throw at client startup");
        }
    }

    @Test
    void programIsNamespacedAndCarriesTheGlintContract() throws IOException {
        String json = Files.readString(PROGRAM.resolveSibling("tinted_glint.json"));
        assertTrue(json.contains("\"pseudocore:tinted_glint\""),
                "the program must reference its own namespaced shader files");
        assertTrue(json.contains("\"TextureMat\""),
                "TextureMat is what scrolls the enchantment glint; without it the glint is static");
        assertTrue(json.contains("\"GlintAlpha\"") && json.contains("\"ColorModulator\""),
                "the glint contract uniforms (GlintAlpha, ColorModulator) are missing");

        String vsh = Files.readString(PROGRAM.resolveSibling("tinted_glint.vsh"));
        assertTrue(vsh.contains("in vec4 Color;"),
                "the vertex shader must read the Color attribute - that is the whole tint");
        assertTrue(vsh.contains("TextureMat * vec4(UV0"),
                "the vertex shader must apply TextureMat, or the glint never scrolls");

        String fsh = Files.readString(PROGRAM.resolveSibling("tinted_glint.fsh"));
        assertTrue(fsh.contains("* vertexColor"),
                "the fragment shader must multiply by the vertex colour - otherwise the glint ignores the tint");
    }

    @Test
    void clientMixinConfigIsRegisteredAndOptional() throws IOException {
        String config = Files.readString(CLIENT_MIXINS);
        assertTrue(config.contains("\"required\": false"),
                "the client mixin config must be optional: a missing target may cost the "
                        + "glint but must never fail at load time (#55)");
        assertTrue(config.contains("\"client\"") && config.contains("ItemRendererMixin"),
                "ItemRendererMixin must sit in the config's client array so it never "
                        + "loads on a dedicated server");

        String toml = Files.readString(MODS_TOML);
        assertTrue(toml.contains("pseudocore.client.mixins.json"),
                "neoforge.mods.toml must register the client mixin config - an "
                        + "unregistered config means the mixin silently never applies");

        assertTrue(Files.exists(MIXIN_SOURCE),
                "the mixin config references ItemRendererMixin but the class is gone");
        assertTrue(Files.exists(GLINT_SOURCE),
                "the mixin depends on TintedGlint but the class is gone");
    }
}
