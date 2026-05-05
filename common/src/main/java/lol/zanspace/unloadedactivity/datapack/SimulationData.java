package lol.zanspace.unloadedactivity.datapack;

import java.util.*;

public class SimulationData {

    public Map<String, SimulateProperty> propertyMap;

    public final boolean hasRandTicksWithoutGroup;
    public final boolean hasPrecTicksWithoutGroup;

    public SimulationData(IncompleteSimulationData incomplete) {
        HashMap<String, SimulateProperty> newPropertyMap = new HashMap<>();

        for (var entry : incomplete.propertyMap.entrySet()) {
            String key = entry.getKey();
            try {
                newPropertyMap.put(key, new SimulateProperty(entry.getValue(), key));
            } catch (Exception e) {
                throw new RuntimeException("Failed to verify property " + key + ".\n" + e.getMessage());
            }
        }

        this.propertyMap = Map.copyOf(newPropertyMap);
        this.hasRandTicksWithoutGroup = this.propertyMap.values().stream().anyMatch(property -> !property.isPrecipitation && property.simulateWithGroup.isEmpty());
        this.hasPrecTicksWithoutGroup = this.propertyMap.values().stream().anyMatch(property -> property.isPrecipitation && property.simulateWithGroup.isEmpty());
    }

    public boolean isEmpty() {
        return this.propertyMap.isEmpty();
    }
}
