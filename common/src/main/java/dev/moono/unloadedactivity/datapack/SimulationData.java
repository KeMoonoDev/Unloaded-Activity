package dev.moono.unloadedactivity.datapack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.api.SimulationConfig;
import dev.moono.unloadedactivity.api.SimulationMethod;
import net.minecraft.resources.*;

import java.util.*;
import java.util.function.Function;

public class SimulationData {

    public Map<String, SimulationMethod> methodMap = new HashMap<>();

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

        HashMap<String, ArrayList<JsonObject>> mergingMethods = new HashMap<>();

        for (JsonObject jsonObject : mergingData) {
            for (var entry : jsonObject.entrySet()) {
                String key = entry.getKey();

                if (key.equals("replace") || key.equals("priority")) {
                    continue;
                }

                JsonElement element = entry.getValue();
                if (!element.isJsonObject()) continue;
                JsonObject propertyObject = element.getAsJsonObject();

                mergingMethods.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(propertyObject);
            }
        }

        for (var entry : mergingMethods.entrySet()) {
            String key = entry.getKey();
            List<JsonObject> jsonObjects = entry.getValue();

            int methodStartIndex = 0;

            for (int i = jsonObjects.size() - 1; i >= 0; i--) {
                JsonObject object = jsonObjects.get(i);
                JsonElement jsonReplace = object.get("replace");

                if (jsonReplace != null && jsonReplace.isJsonPrimitive() && jsonReplace.getAsBoolean()) {
                    methodStartIndex = i;
                    break;
                }
            }

            List<JsonObject> mergingMethodData = jsonObjects.subList(methodStartIndex, jsonObjects.size());

            JsonObject firstProperty = mergingMethodData.get(0);
            JsonElement jsonSimulationMethod = firstProperty.get("simulation_method");

            if (jsonSimulationMethod == null) {
                throw new RuntimeException("The field \"simulation_method\" must always be defined in the first entry and when \"replace\" is true.");
            }

            if (!jsonSimulationMethod.isJsonPrimitive() || !jsonSimulationMethod.getAsJsonPrimitive().isString()) {
                throw new RuntimeException("Expected \"simulation_method\" to be a String. Got something that wasn't a String.");
            }

            String simulationTypeUnparsed = jsonSimulationMethod.getAsString();

            var simulationMethodId = UnloadedActivity.parseId(simulationTypeUnparsed);

            Optional<Function<SimulationConfig, SimulationMethod>> maybeSimulationMethodConstructor = UnloadedActivity.simulationMethodRegistry.get(simulationMethodId);

            if (maybeSimulationMethodConstructor.isEmpty()) {
                throw new RuntimeException(simulationMethodId + " is not a valid simulation method.");
            }

            Function<SimulationConfig, SimulationMethod> simulationMethodConstructor = maybeSimulationMethodConstructor.get();

            SimulationConfig simulationConfig = new SimulationConfig();

            boolean isFirst = true;

            for (JsonObject methodObject : mergingMethodData) {
                if (!isFirst) {
                    if (methodObject.get("simulation_method") != null) {
                        throw new RuntimeException("The field \"simulation_method\" cannot be defined if it isn't the first entry and \"replace\" is false.");
                    }
                }
                isFirst = false;
                simulationConfig.merge(methodObject);
            }

            this.methodMap.put(key, simulationMethodConstructor.apply(simulationConfig));
        }

        for (var entry : this.methodMap.entrySet()) {
            String key = entry.getKey();
            SimulationMethod method = entry.getValue();
            for (String dependency : method.dependencies) {
                if (dependency.equals(key)) throw new RuntimeException("Simulation method with key \""+key+"\" is depending on itself. This is not allowed.");
                SimulationMethod dependencyMethod = this.methodMap.get(dependency);
                if (dependencyMethod == null) throw new RuntimeException("Simulation method with key \""+key+"\" is depending on \""+dependency+"\", but there is no simulation method defined for that key.");
                boolean dependencyIsDependable = dependencyMethod.isDependable();
                if (!dependencyIsDependable) throw new RuntimeException("Simulation method with key \""+key+"\" is depending on \""+dependency+"\", but the simulation method assigned to that key is not dependable.");
            }
            // TODO maybe check for reference loops but I am too lazy to do that rn.
        }

        this.hasRandTicksWithoutGroup = this.methodMap
            .values()
            .stream()
            .anyMatch(method -> !method.isPrecipitation && !method.simulatesWithGroup());

        this.hasPrecTicksWithoutGroup = this.methodMap
            .values()
            .stream()
            .anyMatch(method -> method.isPrecipitation && !method.simulatesWithGroup());
    }

    public boolean isEmpty() {
        return this.methodMap.isEmpty();
    }
}
