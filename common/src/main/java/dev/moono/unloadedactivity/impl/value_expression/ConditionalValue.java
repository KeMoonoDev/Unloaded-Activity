package dev.moono.unloadedactivity.impl.value_expression;

import dev.moono.unloadedactivity.api.value_expression.ValueExpression;
import dev.moono.unloadedactivity.api.ExpressionContext;
import dev.moono.unloadedactivity.api.condition.Condition;

import java.util.function.Function;
import java.util.stream.Stream;

public class ConditionalValue<T> implements ValueExpression<T> {

    Condition condition;
    ValueExpression<T> trueValue;
    ValueExpression<T> falseValue;

    public ConditionalValue(Condition condition, ValueExpression<T> trueValue, ValueExpression<T> falseValue) {
        this.condition = condition;
        this.trueValue = trueValue;
        this.falseValue = falseValue;
    }

    @Override
    public T evaluate(ExpressionContext context) {
        if (condition.isValid(context)) {
            return trueValue.evaluate(context);
        } else {
            return falseValue.evaluate(context);
        }
    }

    @Override
    public Stream<T> getPossibleValues() {
        return Stream.concat(trueValue.getPossibleValues(), falseValue.getPossibleValues());
    }

    @Override
    public boolean canBeAffectedByWeather() {
        return condition.canBeAffectedByWeather()
                || trueValue.canBeAffectedByWeather()
                || falseValue.canBeAffectedByWeather();
    }

    @Override
    public boolean canBeAffectedByTime() {
        return condition.canBeAffectedByTime()
                || trueValue.canBeAffectedByTime()
                || falseValue.canBeAffectedByTime();
    }

    @Override
    public boolean isRandom() {
        return condition.isRandom()
            || trueValue.isRandom()
            || falseValue.isRandom();
    }

    @Override
    public long getNextValueSwitchDuration(ExpressionContext context) {
        boolean isValid = condition.isValid(context);

        long conditionSwitch = condition.getNextConditionSwitchDuration(context);

        return Math.min(
                isValid ?
                        trueValue.getNextValueSwitchDuration(context) :
                        falseValue.getNextValueSwitchDuration(context)
                ,
                conditionSwitch
        );
    }

    @Override
    public ValueExpression<T> replicate() {
        return new ConditionalValue<>(condition, trueValue.replicate(), falseValue.replicate());
    }

    @Override
    public void replaceSuper(ValueExpression<T> superValue) {
        if (trueValue.isSuper()) {
            trueValue = superValue;
        } else {
            trueValue.replaceSuper(superValue);
        }

        if (falseValue.isSuper()) {
            falseValue = superValue;
        } else {
            falseValue.replaceSuper(superValue);
        }
    }

    @Override
    public <U> ValueExpression<U> map(Function<T, U> mapFunction) {
        return new ConditionalValue<>(this.condition, trueValue.map(mapFunction), falseValue.map(mapFunction));
    }
}