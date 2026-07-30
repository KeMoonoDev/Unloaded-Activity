package dev.moono.unloadedactivity.api;

import com.google.gson.JsonObject;
import dev.moono.unloadedactivity.api.value_expression.ValueExpression;
import net.minecraft.resources.*;

import java.util.*;
import java.util.function.Function;

public class NumberFetcherRegistry {
    private final HashMap<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, Function<JsonObject, ValueExpression<Number>>> numberFetchers = new HashMap<>();
    private final HashMap<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, Number> numbers = new HashMap<>();

    public void register(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id, ValueExpression<Number> value) {
        this.register(id, unused -> value);
    }

    public void register(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id, Function<JsonObject, ValueExpression<Number>> factory) {
        numberFetchers.put(id, factory);
    }

    public void registerNumber(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id, Number number) {
        numbers.put(id, number);
    }

    public Optional<Number> getNumber(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id) {
        return Optional.ofNullable(numbers.get(id));
    }

    public Optional<ValueExpression<Number>> resolve(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id) {
        return this.resolve(id, new JsonObject());
    }

    public Optional<ValueExpression<Number>> resolve(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id, JsonObject data) {
        Function<JsonObject, ValueExpression<Number>> fetcherFactory = numberFetchers.get(id);
        if (fetcherFactory != null) return Optional.of(fetcherFactory.apply(data));
        return Optional.empty();
    }
}
