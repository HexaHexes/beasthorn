package org.bitwisemadness.pseudocore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bitwisemadness.pseudocore.util.DragonStages;
import org.junit.jupiter.api.Test;

/**
 * The stage arithmetic behind the dragon tooltip (#48), pinned numerically.
 *
 * <p>The reported pack cannot be reproduced in any of this mod's test
 * environments - it needs tensura-iaf-growth on the classpath - so the exact
 * report lives here as inputs: a 98-day dragon under a 512-day growth cap
 * reads stage 1, where the old copied bands said 4. The gametest covers the
 * other half, the equality between the rendered line and the living entity
 * for every cap this environment can configure.</p>
 */
class DragonStagesTest {

    @Test
    void stockDefaultBandsAtTheUntouchedCap() {
        assertEquals(1, DragonStages.stageFor(0, 128, false), "a newborn is stage 1");
        assertEquals(1, DragonStages.stageFor(24, 128, false), "day 24 is still stage 1");
        assertEquals(2, DragonStages.stageFor(25, 128, false), "stage 2 starts at day 25");
        assertEquals(2, DragonStages.stageFor(49, 128, false), "day 49 is still stage 2");
        assertEquals(3, DragonStages.stageFor(50, 128, false), "stage 3 starts at day 50");
        assertEquals(3, DragonStages.stageFor(74, 128, false), "day 74 is still stage 3");
        assertEquals(4, DragonStages.stageFor(75, 128, false), "stage 4 starts at day 75");
        assertEquals(4, DragonStages.stageFor(99, 128, false), "day 99 is still stage 4");
        assertEquals(5, DragonStages.stageFor(100, 128, false), "stage 5 starts at day 100");
        assertEquals(5, DragonStages.stageFor(500, 128, false), "stage 5 never grows past 5");
    }

    @Test
    void reportedPackReadsStageOneAtNinetyEightDays() {
        assertEquals(1, DragonStages.stageFor(98, 512, true),
                "the reported dragon: 98 days over a 512-day growth cap is stage 1");
        assertEquals(1, DragonStages.stageFor(99, 512, true), "the stretched stage-2 band starts at day 100");
        assertEquals(2, DragonStages.stageFor(100, 512, true), "stage 2 starts at stretched day 100");
        assertEquals(3, DragonStages.stageFor(200, 512, true), "stage 3 starts at stretched day 200");
        assertEquals(4, DragonStages.stageFor(300, 512, true), "stage 4 starts at stretched day 300");
        assertEquals(5, DragonStages.stageFor(400, 512, true), "stage 5 starts at stretched day 400");
    }

    @Test
    void raisedCapWithoutTheAddonKeepsStockBands() {
        assertEquals(4, DragonStages.stageFor(98, 512, false),
                "without tensura-iaf-growth the config value cannot stretch the bands: stock stage 4");
    }

    @Test
    void addonAtTheDefaultCapIsStockForStock() {
        assertEquals(4, DragonStages.stageFor(98, 128, true),
                "the addon at its default cap must be byte-for-byte the stock answer");
        assertEquals(5, DragonStages.stageFor(100, 128, true), "identity stretch keeps the stock boundary");
        assertEquals(2, DragonStages.stageFor(25, 128, true), "identity stretch keeps the stock boundary");
        assertEquals(1, DragonStages.stageFor(24, 128, true), "identity stretch keeps the stock boundary");
    }

    @Test
    void aLoweredCapClampsTheAgeBeforeBanding() {
        assertEquals(3, DragonStages.stageFor(98, 64, false),
                "a tamed dragon stops ageing at the cap: day 64 of the stock bands is stage 3");
        assertEquals(5, DragonStages.stageFor(98, 64, true),
                "with the addon the cap both clamps and stretches: bands 13/25/38/50 make day 64 stage 5");
        assertEquals(1, DragonStages.stageFor(98, 0, false), "a zero cap clamps every dragon to day 0, stage 1");
        assertEquals(1, DragonStages.stageFor(98, 0, true), "the stretched world clamps the same way");
    }

    @Test
    void stretchedBandsRoundLikeTheAddonsGrowthMath() {
        // 4096 is the addon's widened range top: 25 * 4096 / 128 = 800 exactly.
        assertEquals(1, DragonStages.stageFor(799, 4096, true), "day 799 is below the stretched stage-2 band");
        assertEquals(2, DragonStages.stageFor(800, 4096, true), "the stretched stage-2 band lands on day 800");
    }
}
