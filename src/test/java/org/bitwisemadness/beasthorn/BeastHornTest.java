package org.bitwisemadness.beasthorn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the junit CI check non-vacuous: the suite must always assert something
 * real, so the check cannot pass by running zero tests.
 */
class BeastHornTest {
    @Test
    void modIdIsPlainLowercaseIdentifier() {
        assertTrue(BeastHorn.MOD_ID.matches("[a-z][a-z0-9_]*"),
                "MOD_ID must be a plain lowercase identifier, got: " + BeastHorn.MOD_ID);
    }
}
