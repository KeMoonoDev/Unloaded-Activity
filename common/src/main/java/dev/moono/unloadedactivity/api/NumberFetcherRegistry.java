package dev.moono.unloadedactivity.api;

import dev.moono.unloadedactivity.api.number_fetcher.NumberFetcher;
import dev.moono.unloadedactivity.api.number_fetcher.NumberFetcherFactory;
import dev.moono.unloadedactivity.api.value_expression.ValueExpression;
import net.minecraft.resources.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class NumberFetcherRegistry {
    private HashMap<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, ValueExpression<Number>> numberFetchers = new HashMap<>();
    private HashMap<#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif, Number> numbers = new HashMap<>();
    private ArrayList<NumberFetcherFactory> dynamicNumberFetchers = new ArrayList<>();

    public void register(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id, ValueExpression<Number> value) {
        numberFetchers.put(id, value);
    };

    public void registerNumber(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id, Number number) {
        numbers.put(id, number);
    };

    public void registerDynamic(String namespace, Predicate<String> matches, Function<String, NumberFetcher> factory) {
        registerDynamic(new NumberFetcherFactory() {
            @Override
            public String namespace() {
                return namespace;
            }

            @Override
            public boolean matches(String path) {
                return matches.test(path);
            }

            @Override
            public NumberFetcher create(String path) {
                return factory.apply(path);
            }
        });
    };

    public void registerDynamic(NumberFetcherFactory factory) {
        dynamicNumberFetchers.add(factory);
    };

    public Optional<Number> getNumber(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id) {
        return Optional.ofNullable(numbers.get(id));
    };

    public Optional<ValueExpression<Number>> resolve(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id) {
        ValueExpression<Number> fetcher = numberFetchers.get(id);
        if (fetcher != null) return Optional.of(fetcher);

        return Optional.ofNullable(resolveDynamicFetcher(id));
    };

    @Nullable
    private NumberFetcher resolveDynamicFetcher(#if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif id) {
        for (var dynamicNumberFetcher : dynamicNumberFetchers) {
            if (!dynamicNumberFetcher.namespace().equals(id.getNamespace())) {
                continue;
            }

            if (!dynamicNumberFetcher.matches(id.getPath())) {
                continue;
            }

            return dynamicNumberFetcher.create(id.getPath());
        }
        return null;
    };
}
