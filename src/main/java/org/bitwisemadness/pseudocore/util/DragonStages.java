package org.bitwisemadness.pseudocore.util;

/**
 * Ice and Fire's dragon stage arithmetic, reproduced without Ice and Fire.
 *
 * <p>Two contracts meet here, both verified against the jars the pack ships:</p>
 *
 * <ul>
 * <li><b>Stock Ice and Fire CE</b> - {@code DragonBaseEntity.getDragonStage}
 * compares {@code getAgeInDays()} against the hardcoded bands 25/50/75/100
 * (stage 2/3/4/5 from), and {@code getAgeInDays()} clamps the raw day count
 * into {@code [0, maxTamedDragonAge]} for a tamed dragon - bytecode verified
 * against CE 2.0-beta.15.</li>
 * <li><b>tensura-iaf-growth</b> (mod id {@code tensura_iaf_growth}) -
 * {@code DragonBaseEntityGrowthMixin} {@code @ModifyConstant}-replaces each of
 * those four band constants with
 * {@code max(1, round(band * maxTamedDragonAge / 128))}, so a raised growth
 * cap stretches the whole stage curve. At the untouched default of 128 every
 * stretched band equals the stock one.</li>
 * </ul>
 *
 * <p>The addon is detected by mod id rather than by the config value: stock
 * Ice and Fire clamps {@code maxTamedDragonAge}'s range to [0, 128], so a
 * world above that always has the addon - but the converse fails, a pack can
 * run the addon at the default cap (stock behaviour) or below it (a shortened
 * growth curve the stock bands would miss). The value alone cannot say which
 * world this is; the mod id can.</p>
 *
 * <p>The clamp mirrors the tamed branch of {@code getAgeInDays()}. A stored
 * dragon is always tame: both capture families refuse anything the player does
 * not own, and ownership implies taming, so the untamed 128-day branch is
 * unreachable for a horn's contents. Age does not advance while a dragon is
 * stored, so the frozen {@code AgeTicks} a tooltip reads and the value the
 * live entity reported at capture describe the same day count.</p>
 */
public final class DragonStages {

    /** The age Ice and Fire's growth curve was authored against. */
    private static final int STOCK_MAX_AGE = 128;

    private DragonStages() {
    }

    /**
     * The stage Ice and Fire would report for a dragon of this raw age under
     * this configured age cap and growth-addon presence.
     *
     * @param rawAgeDays         the raw {@code AgeTicks / 24000} day count
     * @param maxTamedDragonAge  the configured {@code maxTamedDragonAge}
     * @param growthAddonPresent whether tensura-iaf-growth is installed
     * @return the stage, 1 to 5
     */
    public static int stageFor(int rawAgeDays, int maxTamedDragonAge, boolean growthAddonPresent) {
        int days = Math.min(Math.max(rawAgeDays, 0), Math.max(maxTamedDragonAge, 0));
        int stageTwoFrom = band(25, maxTamedDragonAge, growthAddonPresent);
        int stageThreeFrom = band(50, maxTamedDragonAge, growthAddonPresent);
        int stageFourFrom = band(75, maxTamedDragonAge, growthAddonPresent);
        int stageFiveFrom = band(100, maxTamedDragonAge, growthAddonPresent);
        if (days >= stageFiveFrom) return 5;
        if (days >= stageFourFrom) return 4;
        if (days >= stageThreeFrom) return 3;
        if (days >= stageTwoFrom) return 2;
        return 1;
    }

    /**
     * One stage boundary: stock, or the addon's stretched form of it. The
     * formula is tensura-iaf-growth's {@code Growth.scaleDays} copied exactly
     * rather than called - the addon stays optional, so its maths is a
     * documented contract here instead of a dependency.
     */
    private static int band(int stockDays, int maxTamedDragonAge, boolean growthAddonPresent) {
        if (!growthAddonPresent) {
            return stockDays;
        }
        return Math.max(1, (int) Math.round(stockDays * ((double) maxTamedDragonAge / STOCK_MAX_AGE)));
    }
}
