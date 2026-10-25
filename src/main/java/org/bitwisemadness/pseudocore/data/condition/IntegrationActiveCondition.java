package org.bitwisemadness.pseudocore.data.condition;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.function.BooleanSupplier;

/**
 * A datapack condition that is true only while a named integration is both
 * installed and switched on.
 *
 * <p>Registered once per integration as a field-less condition
 * ({@code pseudocore:tensura_active}, {@code pseudocore:iceandfire_active}), so a
 * data file gates itself with one entry and no nesting:
 * {@code "conditions": [{"type": "pseudocore:tensura_active"}]}. That is what
 * lets a single condition object stand for "mod present <b>and</b> integration
 * enabled", instead of an {@code and} of {@code mod_loaded} plus a config test
 * that datapack JSON cannot express.</p>
 *
 * <p>The decision is injected as a {@link BooleanSupplier} rather than read from
 * {@code ModList} here. That keeps the logic unit-testable without a game launch,
 * and it defers the mod-list lookup to evaluation time - which is exactly when
 * data packs load, the earliest moment the answer is meaningful.</p>
 */
public final class IntegrationActiveCondition implements ICondition {

    private final String name;
    private final BooleanSupplier active;
    private final MapCodec<IntegrationActiveCondition> codec = MapCodec.unit(this);

    /**
     * @param name   the registry path, e.g. {@code tensura_active}, used by
     *               {@link #toString()} so a rejected data file names the
     *               condition rather than printing an object hash
     * @param active evaluated on every test
     */
    public IntegrationActiveCondition(String name, BooleanSupplier active) {
        this.name = name;
        this.active = active;
    }

    @Override
    public boolean test(IContext context) {
        return active.getAsBoolean();
    }

    /** The condition carries no fields, so its codec is a unit codec. */
    @Override
    public MapCodec<? extends ICondition> codec() {
        return codec;
    }

    @Override
    public String toString() {
        return "pseudocore:" + name;
    }
}
