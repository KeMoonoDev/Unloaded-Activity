package dev.moono.unloadedactivity.datapack.value_expression;

import com.mojang.datafixers.util.Pair;
import dev.moono.unloadedactivity.datapack.ValueExpression;
import dev.moono.unloadedactivity.datapack.ExpressionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class TimeValue<T> implements ValueExpression<T> {
    private final List<Pair<Long, ValueExpression<T>>> list;
    public TimeValue(List<Pair<Long, ValueExpression<T>>> list) {
        list.sort(Comparator.comparing(Pair::getFirst));
        this.list = list;
    }

    @Override
    public T evaluate(ExpressionContext context) {
        if (this.list.isEmpty())
            return null;

        long length = 24000;
        long modCurrentTime = Math.floorMod(context.currentTime, length);

        var currentPair = this.list.get(this.list.size() - 1);

        for (var pair : this.list) {
            if (pair.getFirst() <= modCurrentTime) {
                currentPair = pair;
            } else {
                break;
            }
        }

        return currentPair.getSecond().evaluate(context);
    }

    @Override
    public Stream<T> getPossibleValues() {
        return list.stream().flatMap(pair -> pair.getSecond().getPossibleValues());
    }

    @Override
    public boolean canBeAffectedByWeather() {
        for (var pair : this.list) {
            if (pair.getSecond().canBeAffectedByWeather()) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean isRandom() {
        for (var pair : this.list) {
            if (pair.getSecond().isRandom()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canBeAffectedByTime() {
        return true;
    }

    @Override
    public <U> ValueExpression<U> map(Function<T, U> mapFunction) {
        ArrayList<Pair<Long, ValueExpression<U>>> newList = new ArrayList<>();
        for (var pair : list) {
            newList.add(pair.mapSecond((tCalculateValue -> tCalculateValue.map(mapFunction))));
        }
        return new TimeValue<>(newList);
    }

    @Override
    public long getNextValueSwitchDuration(ExpressionContext context) {
        if (this.list.isEmpty())
            return Long.MAX_VALUE;

        long length = 24000;
        long modCurrentTime = Math.floorMod(context.currentTime, length);

        var currentPair = this.list.get(this.list.size() - 1);
        Pair<Long, ValueExpression<T>> nextPair = null;

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

        long currentNextOddsSwitch = currentPair.getSecond().getNextValueSwitchDuration(context);
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
    public ValueExpression<T> replicate() {
        List<Pair<Long, ValueExpression<T>>> newList = new ArrayList<>();
        for (var pair : this.list) {
            newList.add(Pair.of(pair.getFirst(), pair.getSecond().replicate()));
        }
        return new TimeValue<>(newList);
    }

    @Override
    public void replaceSuper(ValueExpression<T> superValue) {
        for (int i=0; i < this.list.size(); i++) {
            Pair<Long, ValueExpression<T>> pair = this.list.get(i);
            ValueExpression<T> value = pair.getSecond();

            if (value.isSuper()) {
                this.list.set(i, new Pair<>(pair.getFirst(), superValue));
            } else {
                value.replaceSuper(superValue);
            }
        }
    }
}
