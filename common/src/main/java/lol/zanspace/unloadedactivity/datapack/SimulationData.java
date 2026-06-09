package lol.zanspace.unloadedactivity.datapack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import lol.zanspace.unloadedactivity.UnloadedActivity;
import lol.zanspace.unloadedactivity.api.SimulationConfig;
import lol.zanspace.unloadedactivity.api.SimulationMethod;
import lol.zanspace.unloadedactivity.api.simulation_methods.GroupableSimulationMethod;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Function;

public class SimulationData {

    public Map<String, SimulationMethod> propertyMap = new HashMap<>();

    public final boolean hasRandTicksWithoutGroup;
    public final boolean hasPrecTicksWithoutGroup;

    public SimulationData(List<JsonObject> sortedData) {
        int startIndex = 0;

        for (int i = sortedData.size() - 1; i >= 0; i--) {
            JsonObject object = sortedData.get(i);
            JsonElement jsonReplace = object.get("replace");

            if (jsonReplace != null && jsonReplace.isJsonPrimitive() && jsonReplace.getAsBoolean()) {
                startIndex = i;
                break;
            }
        }

        List<JsonObject> mergingData = sortedData.subList(startIndex, sortedData.size());

        HashMap<String, ArrayList<JsonObject>> mergingProperties = new HashMap<>();

        for (JsonObject jsonObject : mergingData) {
            for (var entry : jsonObject.entrySet()) {
                String key = entry.getKey();

                if (key.equals("replace") || key.equals("priority")) {
                    continue;
                }

                JsonElement element = entry.getValue();
                if (!element.isJsonObject()) continue;
                JsonObject propertyObject = element.getAsJsonObject();

                mergingProperties.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(propertyObject);
            }
        }

        for (var entry : mergingProperties.entrySet()) {
            String key = entry.getKey();
            List<JsonObject> jsonObjects = entry.getValue();

            int propertyStartIndex = 0;

            for (int i = jsonObjects.size() - 1; i >= 0; i--) {
                JsonObject object = jsonObjects.get(i);
                JsonElement jsonReplace = object.get("replace");

                if (jsonReplace != null && jsonReplace.isJsonPrimitive() && jsonReplace.getAsBoolean()) {
                    propertyStartIndex = i;
                    break;
                }
            }

            List<JsonObject> mergingPropertyData = jsonObjects.subList(propertyStartIndex, jsonObjects.size());

            JsonObject firstProperty = mergingPropertyData.getFirst();
            JsonElement jsonSimulationMethod = firstProperty.get("simulation_method");

            if (jsonSimulationMethod == null) {
                throw new RuntimeException("The field \"simulation_method\" must always be defined in the first entry and when \"replace\" is true.");
            }

            if (!jsonSimulationMethod.isJsonPrimitive() || !jsonSimulationMethod.getAsJsonPrimitive().isString()) {
                throw new RuntimeException("Expected \"simulation_method\" to be a String. Got something that wasn't a String.");
            }

            String simulationTypeUnparsed = jsonSimulationMethod.getAsString();

            Identifier simulationMethodId;
            if (simulationTypeUnparsed.indexOf(':') >= 0) {
                simulationMethodId = Identifier.parse(simulationTypeUnparsed);
            } else {
                simulationMethodId = Identifier.parse(UnloadedActivity.MOD_ID+":"+simulationTypeUnparsed);
            }

            Optional<Function<SimulationConfig, SimulationMethod>> maybeSimulationMethodConstructor = UnloadedActivity.simulationMethodRegistry.get(simulationMethodId);

            if (maybeSimulationMethodConstructor.isEmpty()) {
                throw new RuntimeException(simulationMethodId + " is not a valid simulation method.");
            }

            Function<SimulationConfig, SimulationMethod> simulationMethodConstructor = maybeSimulationMethodConstructor.get();

            SimulationConfig simulationConfig = new SimulationConfig(key);

            boolean isFirst = true;

            for (JsonObject propertyObject : mergingPropertyData) {
                if (!isFirst) {
                    if (propertyObject.get("simulation_method") != null) {
                        throw new RuntimeException("The field \"simulation_method\" cannot be defined if it isn't the first entry and \"replace\" is false.");
                    }
                }
                isFirst = false;
                simulationConfig.merge(propertyObject);
            }

            this.propertyMap.put(key, simulationMethodConstructor.apply(simulationConfig));
        }

        this.hasRandTicksWithoutGroup = this.propertyMap
            .values()
            .stream()
            .anyMatch(method -> !method.isPrecipitation && !method.simulatesWithGroup());

        this.hasPrecTicksWithoutGroup = this.propertyMap
            .values()
            .stream()
            .anyMatch(method -> method.isPrecipitation && !method.simulatesWithGroup());
    }

    public boolean isEmpty() {
        return this.propertyMap.isEmpty();
    }
}
