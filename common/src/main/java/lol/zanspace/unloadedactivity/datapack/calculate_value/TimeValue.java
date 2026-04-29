package lol.zanspace.unloadedactivity.datapack.calculate_value;

import com.mojang.datafixers.util.Pair;
import lol.zanspace.unloadedactivity.datapack.CalculateValue;
import lol.zanspace.unloadedactivity.datapack.CalculationData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class TimeValue<T> implements CalculateValue<T> {
    private final List<Pair<Long, CalculateValue<T>>> list;
    public TimeValue(List<Pair<Long, CalculateValue<T>>> list) {
        list.sort(Comparator.comparing(Pair::getFirst));
        this.list = list;
    }

    @Override
    public T calculateValue(CalculationData data) {
        if (this.list.isEmpty())
            return null;

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
    public <U> CalculateValue<U> map(Function<T, U> mapFunction) {
        ArrayList<Pair<Long, CalculateValue<U>>> newList = new ArrayList<>();
        for (var pair : list) {
            newList.add(pair.mapSecond((tCalculateValue -> tCalculateValue.map(mapFunction))));
        }
        return new TimeValue<>(newList);
    }

    @Override
    public long getNextValueSwitchDuration(CalculationData data) {
        if (this.list.isEmpty())
            return Long.MAX_VALUE;

        long length = 24000;
        long modCurrentTime = Math.floorMod(data.currentTime, length);

        var currentPair = this.list.get(this.list.size() - 1);
        Pair<Long, CalculateValue<T>> nextPair = null;

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
    public CalculateValue<T> replicate() {
        List<Pair<Long, CalculateValue<T>>> newList = new ArrayList<>();
        for (var pair : this.list) {
            newList.add(Pair.of(pair.getFirst(), pair.getSecond().replicate()));
        }
        return new TimeValue<>(newList);
    }

    @Override
    public void replaceSuper(CalculateValue<T> superValue) {
        for (int i=0; i < this.list.size(); i++) {
            Pair<Long, CalculateValue<T>> pair = this.list.get(i);
            CalculateValue<T> value = pair.getSecond();

            if (value.isSuper()) {
                this.list.set(i, new Pair<>(pair.getFirst(), superValue));
            } else {
                value.replaceSuper(superValue);
            }
        }
    }
}
