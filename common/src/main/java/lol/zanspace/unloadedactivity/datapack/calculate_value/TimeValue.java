package lol.zanspace.unloadedactivity.datapack.calculate_value;

import com.mojang.datafixers.util.Pair;
import lol.zanspace.unloadedactivity.datapack.CalculateValue;
import lol.zanspace.unloadedactivity.datapack.CalculationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TimeValue implements CalculateValue {
    private final List<Pair<Long, CalculateValue>> list;
    public TimeValue(List<Pair<Long, CalculateValue>> list) {
        list.sort(Comparator.comparing(Pair::getFirst));
        this.list = list;
    }

    @Override
    public double calculateValue(CalculationData data) {
        if (this.list.isEmpty())
            return 0;

        long length = 24000;
        long modCurrentTime = Math.floorMod(data.currentTime, length);

        var currentPair = this.list.get(this.list.size() - 1);

        for (var pair : this.list) {
            if (pair.getFirst() <= modCurrentTime) {
                currentPair = pair;
            } else {
                break;
            }
        }

        return currentPair.getSecond().calculateValue(data);
    }
    @Override
    public boolean canBeAffectedByWeather() {
        return false;
    }

    @Override
    public boolean canBeAffectedByTime() {
        return true;
    }

    @Override
    public long getNextValueSwitchDuration(CalculationData data) {
        if (this.list.isEmpty())
            return Long.MAX_VALUE;

        long length = 24000;
        long modCurrentTime = Math.floorMod(data.currentTime, length);

        var currentPair = this.list.get(this.list.size() - 1);
        Pair<Long, CalculateValue> nextPair = null;

        for (var pair : this.list) {
            if (pair.getFirst() <= modCurrentTime) {
                currentPair = pair;
            } else {
                nextPair = pair;
                break;
            }
        }

        if (nextPair == null) {
            nextPair = this.list.get(0);
        }

        long currentNextOddsSwitch = currentPair.getSecond().getNextValueSwitchDuration(data);
        long nextOddsSwitch;

        if (nextPair.getFirst() == currentPair.getFirst()) {
            nextOddsSwitch = Long.MAX_VALUE;
        } else {
            nextOddsSwitch = nextPair.getFirst() - modCurrentTime;
            if (nextOddsSwitch < 0) {
                nextOddsSwitch += 24000;
            }
        }


        return Math.min(currentNextOddsSwitch, nextOddsSwitch);
    }

    @Override
    public CalculateValue replicate() {
        List<Pair<Long, CalculateValue>> newList = new ArrayList<>();
        for (var pair : this.list) {
            newList.add(Pair.of(pair.getFirst(), pair.getSecond().replicate()));
        }
        return new TimeValue(newList);
    }

    @Override
    public void replaceSuper(CalculateValue superValue) {
        for (int i=0; i < this.list.size(); i++) {
            Pair<Long, CalculateValue> pair = this.list.get(i);
            CalculateValue value = pair.getSecond();

            if (value.isSuper()) {
                this.list.set(i, new Pair<>(pair.getFirst(), superValue));
            } else {
                value.replaceSuper(superValue);
            }
        }
    }
}
