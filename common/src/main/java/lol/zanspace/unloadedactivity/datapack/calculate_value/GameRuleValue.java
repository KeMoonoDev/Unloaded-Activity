package lol.zanspace.unloadedactivity.datapack.calculate_value;

import lol.zanspace.unloadedactivity.datapack.CalculateValue;
import lol.zanspace.unloadedactivity.datapack.CalculationData;
import lol.zanspace.unloadedactivity.mixin.GameRulesAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Objects;
import java.util.Optional;

import static lol.zanspace.unloadedactivity.interfaces.SimulateChunkBlocks.getProperty;

public class GameRuleValue implements CalculateValue {
    private String gameRuleName;

    public GameRuleValue(String gameRuleName) {
        this.gameRuleName = gameRuleName;
    }

    @Override
    public double calculateValue(CalculationData data) {
        GameRules gameRules = data.level.getGameRules();

        for (var entry : ((GameRulesAccessor)gameRules).unloaded_activity$getRules().entrySet()) {
            String gameRuleId = entry.getKey().getId();
            if (Objects.equals(gameRuleId, gameRuleName)) {
                GameRules.Value<?> value = entry.getValue();

                if (value instanceof GameRules.IntegerValue intValue) {
                    return intValue.get();
                } else if (value instanceof GameRules.BooleanValue boolValue) {
                    return boolValue.get() ? 1 : 0;
                }
            }
        }

        return Double.NaN;
    }

    @Override
    public boolean canBeAffectedByWeather() {
        return false;
    }

    @Override
    public boolean canBeAffectedByTime() {
        return false;
    }

    @Override
    public long getNextValueSwitchDuration(CalculationData data) {
        return Long.MAX_VALUE;
    }

    @Override
    public CalculateValue replicate() {
        return this;
    }

    @Override
    public void replaceSuper(CalculateValue superValue) {

    }
}
