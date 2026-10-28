package org.bitwisemadness.pseudocore;

import org.bitwisemadness.pseudocore.util.SpeciesWords;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the word matching behind the Race line (#69).
 *
 * <p>These tests load {@link SpeciesWords} rather than
 * {@link org.bitwisemadness.pseudocore.util.VariantHelper}, whose signatures
 * mention Minecraft types the junit classpath does not carry. The helper is
 * pure Java precisely so this logic can be pinned here.</p>
 */
public class VariantNamingTest {

    /**
     * The #69 report: "lime" is four letters and fell inside "slime" at index 1
     * under the old substring test, so the race word was dropped and the name
     * line read "Lime".
     */
    @Test
    public void limeDoesNotRetireSlime() {
        assertFalse(SpeciesWords.alreadyNamesMob("lime", "slime"),
                "a Lime Slime's name line dropped the species word (#69)");
    }

    /**
     * A compound species still retires on its inner word: a Mystic Wind Wolf
     * Direwolf reads as a wolf already. This word matches at the end of
     * "direwolf", not the start.
     */
    @Test
    public void wolfRetiresDirewolf() {
        assertTrue(SpeciesWords.alreadyNamesMob("Mystic Wind Wolf", "direwolf"));
    }

    /** A variant that is the species outright still retires it. */
    @Test
    public void direwolfRetiresDirewolf() {
        assertTrue(SpeciesWords.alreadyNamesMob("Direwolf", "direwolf"));
    }

    /** A variant with no species word in it keeps the race. */
    @Test
    public void unrelatedVariantKeepsRace() {
        assertFalse(SpeciesWords.alreadyNamesMob("Red Fang", "direwolf"));
    }

    /**
     * A species word that only begins a longer variant word does not retire it:
     * "slime" is what a slime variant would begin with, but a variant of some
     * other shape must not lose its race to that alone. The guard that keeps
     * this true is the compound check - "s" is not a half of "slime".
     */
    @Test
    public void speciesPrefixAloneDoesNotRetire() {
        assertFalse(SpeciesWords.alreadyNamesMob("king slime", "lime"),
                "a two-letter species must not retire a variant it merely begins");
    }

    /** A variant whose words do not name the species keeps the race. */
    @Test
    public void unrelatedWordsKeepRace() {
        assertFalse(SpeciesWords.alreadyNamesMob("red fang", "slime"));
    }

    /** Matching ignores case, so a title-cased variant behaves as its words. */
    @Test
    public void matchingIgnoresCase() {
        assertTrue(SpeciesWords.alreadyNamesMob("Mystic Wind Wolf", "DIREWOLF"));
        assertTrue(SpeciesWords.alreadyNamesMob("mystic wind wolf", "Direwolf"));
    }

    /** Null text means no variant to compare, which is not a match. */
    @Test
    public void nullTextIsNotAMatch() {
        assertFalse(SpeciesWords.alreadyNamesMob(null, "slime"));
        assertFalse(SpeciesWords.alreadyNamesMob("lime", null));
    }

    /** A variant that is only whitespace names nothing. */
    @Test
    public void blankVariantIsNotAMatch() {
        assertFalse(SpeciesWords.alreadyNamesMob("   ", "slime"));
    }
}
