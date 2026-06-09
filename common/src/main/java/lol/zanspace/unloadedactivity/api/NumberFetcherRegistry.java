package lol.zanspace.unloadedactivity.api;

import lol.zanspace.unloadedactivity.datapack.ValueExpression;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class NumberFetcherRegistry {
    private HashMap<Identifier, ValueExpression<Number>> numberFetchers = new HashMap<>();
    private HashMap<Identifier, Number> numbers = new HashMap<>();
    private ArrayList<NumberFetcherFactory> dynamicNumberFetchers = new ArrayList<>();

    public void register(Identifier id, ValueExpression<Number> value) {
        numberFetchers.put(id, value);
    };

    public void registerNumber(Identifier id, Number number) {
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

    public Optional<Number> getNumber(Identifier id) {
        return Optional.ofNullable(numbers.get(id));
    };

    public Optional<ValueExpression<Number>> resolve(Identifier id) {
        ValueExpression<Number> fetcher = numberFetchers.get(id);
        if (fetcher != null) return Optional.of(fetcher);

        return Optional.ofNullable(resolveDynamicFetcher(id));
    };

    @Nullable
    private NumberFetcher resolveDynamicFetcher(Identifier id) {
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
