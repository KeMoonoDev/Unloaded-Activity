package lol.zanspace.unloadedactivity.api;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SimulationMethodRegistry {
    private final HashMap<Identifier, Function<SimulationConfig, SimulationMethod>> simulationMethods = new HashMap<>();

    public void register(Identifier id, Function<SimulationConfig, SimulationMethod> construct) {
        simulationMethods.put(id, construct);
    }

    public Optional<Function<SimulationConfig, SimulationMethod>> get(Identifier id) {
        return Optional.ofNullable(simulationMethods.get(id));
    };
}
