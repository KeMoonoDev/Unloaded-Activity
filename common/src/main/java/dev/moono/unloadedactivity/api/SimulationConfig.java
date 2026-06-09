package dev.moono.unloadedactivity.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.moono.unloadedactivity.datapack.Condition;
import dev.moono.unloadedactivity.datapack.ValueExpression;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimulationConfig {
    private final HashMap<String, ArrayList<JsonElement>> fieldValues = new HashMap<>();;

    public SimulationConfig(String target) {
        ArrayList<JsonElement> targetList = new ArrayList<>();
        targetList.add(new JsonPrimitive(target));
        this.fieldValues.put("target", targetList);
    }

    public SimulationConfig() {}

    public void merge(JsonObject jsonObject) {
        for (var entry : jsonObject.entrySet()) {
            ArrayList<JsonElement> list = fieldValues.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>());
            list.add(entry.getValue());
        }
        /*
        for (var pair : schema.fieldsList) {
            String fieldName = pair.getFirst();
            TypeDescriptor typeDescriptor = pair.getSecond();
            JsonElement input = jsonObject.get(fieldName);
            if (input == null) continue;
            switch (typeDescriptor.container()) {
                case ContainerKind.NONE, ContainerKind.NULLABLE -> {
                    fieldValues.put(fieldName, typeDescriptor.type().deserialize(JsonOps.INSTANCE, input, fieldValues.get(fieldName)));
                }
                case ContainerKind.LIST -> {
                    ArrayList<Object> list = getList(fieldName, typeDescriptor.type());
                    input.getAsJsonArray().forEach(element -> {
                        list.add(typeDescriptor.type().deserialize(JsonOps.INSTANCE, element, null));
                    });
                }
                case ContainerKind.MAP -> {
                    HashMap<String, Object> map = getMap(fieldName, typeDescriptor.type());
                    input.getAsJsonObject().entrySet().forEach(entry -> {
                        String key = entry.getKey();
                        map.compute(key, (k, superData) -> typeDescriptor.type().deserialize(JsonOps.INSTANCE, entry.getValue(), superData));
                    });
                }
            }
        }

         */
    }

    @Nullable
    private<T> T getValue(String fieldName, FieldType type) {
        List<JsonElement> elements = this.fieldValues.get(fieldName);
        T finalValue = null;
        if (elements == null) return finalValue;
        for (JsonElement element : elements) {
            @SuppressWarnings("unchecked")
            T deserializedValue = (T) type.deserialize(element, finalValue);
            finalValue = deserializedValue;
        }

        return finalValue;
    }

    private<T> ArrayList<T> getList(String fieldName, FieldType type) {
        List<JsonElement> elements = this.fieldValues.get(fieldName);
        ArrayList<T> finalValue = new ArrayList<>();
        if (elements == null) return finalValue;
        for (JsonElement element : elements) {
            element.getAsJsonArray().forEach(element2 -> {
                @SuppressWarnings("unchecked")
                T addValue = (T) type.deserialize(element2, null);
                finalValue.add(addValue);
            });
        }
        return finalValue;
    }

    private<T> HashMap<String, T> getMap(String fieldName, FieldType type) {
        List<JsonElement> elements = this.fieldValues.get(fieldName);
        HashMap<String, T> finalValue = new HashMap<>();
        if (elements == null) return finalValue;
        for (JsonElement element : elements) {
            element.getAsJsonObject().entrySet().forEach(entry -> {
                String key = entry.getKey();
                finalValue.compute(key, (k, superData) -> {
                    @SuppressWarnings("unchecked")
                    T addValue = (T) type.deserialize(entry.getValue(), superData);
                    return addValue;
                });
            });
        }
        return finalValue;
    }


    @Nullable
    public Number getNumberNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.NUMBER);
    }

    public Number getNumber(String fieldName) {
        var value = getNumberNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public Number getNumberOrDefault(String fieldName, Number defaultValue) {
        var value = getNumberNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<Number> getNumberList(String fieldName) {
        return this.getList(fieldName, FieldType.NUMBER);
    }

    public HashMap<String, Number> getNumberMap(String fieldName) {
        return this.getMap(fieldName, FieldType.NUMBER);
    }


    @Nullable
    public Boolean getBooleanNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.BOOLEAN);
    }

    public boolean getBoolean(String fieldName) {
        var value = getBooleanNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public boolean getBooleanOrDefault(String fieldName, boolean defaultValue) {
        var value = this.getBooleanNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<Boolean> getBooleanList(String fieldName) {
        return this.getList(fieldName, FieldType.BOOLEAN);
    }

    public HashMap<String, Boolean> getBooleanMap(String fieldName) {
        return this.getMap(fieldName, FieldType.BOOLEAN);
    }


    @Nullable
    public String getStringNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.STRING);
    }

    public String getString(String fieldName) {
        var value = getStringNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public String getStringOrDefault(String fieldName, String defaultValue) {
        var value = this.getStringNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<String> getStringList(String fieldName) {
        return this.getList(fieldName, FieldType.STRING);
    }

    public HashMap<String, String> getStringMap(String fieldName) {
        return this.getMap(fieldName, FieldType.STRING);
    }


    @Nullable
    public Block getBlockNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.BLOCK);
    }

    public Block getBlock(String fieldName) {
        var value = getBlockNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public Block getBlockOrDefault(String fieldName, Block defaultValue) {
        var value = this.getBlockNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<Block> getBlockList(String fieldName) {
        return this.getList(fieldName, FieldType.BLOCK);
    }

    public HashMap<String, Block> getBlockMap(String fieldName) {
        return this.getMap(fieldName, FieldType.BLOCK);
    }


    @Nullable
    public EntityType<?> getEntityTypeNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.ENTITY_TYPE);
    }

    public EntityType<?> getEntityType(String fieldName) {
        var value = getEntityTypeNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public EntityType<?> getEntityTypeOrDefault(String fieldName, EntityType<?> defaultValue) {
        EntityType<?> value = this.getEntityTypeNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<EntityType<?>> getEntityTypeList(String fieldName) {
        return this.getList(fieldName, FieldType.ENTITY_TYPE);
    }

    public HashMap<String, EntityType<?>> getEntityTypeMap(String fieldName) {
        return this.getMap(fieldName, FieldType.ENTITY_TYPE);
    }


    @Nullable
    public ValueExpression<Number> getFixedNumberExpressionNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.FIXED_NUMBER_EXPRESSION);
    }

    public ValueExpression<Number> getFixedNumberExpression(String fieldName) {
        var value = getFixedNumberExpressionNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public ValueExpression<Number> getFixedNumberExpressionOrDefault(String fieldName, ValueExpression<Number> defaultValue) {
        var value = getFixedNumberExpressionNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<ValueExpression<Number>> getFixedNumberExpressionList(String fieldName) {
        return this.getList(fieldName, FieldType.FIXED_NUMBER_EXPRESSION);
    }

    public HashMap<String, ValueExpression<Number>> getFixedNumberExpressionMap(String fieldName) {
        return this.getMap(fieldName, FieldType.FIXED_NUMBER_EXPRESSION);
    }


    @Nullable
    public ValueExpression<Number> getUpdatingNumberExpressionNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.UPDATING_NUMBER_EXPRESSION);
    }

    public ValueExpression<Number> getUpdatingNumberExpression(String fieldName) {
        var value = getUpdatingNumberExpressionNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public ValueExpression<Number> getUpdatingNumberExpressionOrDefault(String fieldName, ValueExpression<Number> defaultValue) {
        var value = getUpdatingNumberExpressionNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<ValueExpression<Number>> getUpdatingNumberExpressionList(String fieldName) {
        return this.getList(fieldName, FieldType.UPDATING_NUMBER_EXPRESSION);
    }

    public HashMap<String, ValueExpression<Number>> getUpdatingNumberExpressionMap(String fieldName) {
        return this.getMap(fieldName, FieldType.UPDATING_NUMBER_EXPRESSION);
    }


    @Nullable
    public ValueExpression<Number> getRandomizedNumberExpressionNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.RANDOMIZED_NUMBER_EXPRESSION);
    }

    public ValueExpression<Number> getRandomizedNumberExpression(String fieldName) {
        var value = getRandomizedNumberExpressionNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public ValueExpression<Number> getRandomizedNumberExpressionOrDefault(String fieldName, ValueExpression<Number> defaultValue) {
        var value = getRandomizedNumberExpressionNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<ValueExpression<Number>> getRandomizedNumberExpressionList(String fieldName) {
        return this.getList(fieldName, FieldType.RANDOMIZED_NUMBER_EXPRESSION);
    }

    public HashMap<String, ValueExpression<Number>> getRandomizedNumberExpressionMap(String fieldName) {
        return this.getMap(fieldName, FieldType.RANDOMIZED_NUMBER_EXPRESSION);
    }


    @Nullable
    public ValueExpression<Block> getFixedBlockExpressionNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.FIXED_BLOCK_EXPRESSION);
    }

    public ValueExpression<Block> getFixedBlockExpression(String fieldName) {
        var value = getFixedBlockExpressionNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public ValueExpression<Block> getFixedBlockExpressionOrDefault(String fieldName, ValueExpression<Block> defaultValue) {
        var value = getFixedBlockExpressionNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<ValueExpression<Block>> getFixedBlockExpressionList(String fieldName) {
        return this.getList(fieldName, FieldType.FIXED_BLOCK_EXPRESSION);
    }

    public HashMap<String, ValueExpression<Block>> getFixedBlockExpressionMap(String fieldName) {
        return this.getMap(fieldName, FieldType.FIXED_BLOCK_EXPRESSION);
    }


    @Nullable
    public ValueExpression<Block> getUpdatingBlockExpressionNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.UPDATING_BLOCK_EXPRESSION);
    }

    public ValueExpression<Block> getUpdatingBlockExpression(String fieldName) {
        var value = getUpdatingBlockExpressionNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public ValueExpression<Block> getUpdatingBlockExpressionOrDefault(String fieldName, ValueExpression<Block> defaultValue) {
        var value = getUpdatingBlockExpressionNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<ValueExpression<Block>> getUpdatingBlockExpressionList(String fieldName) {
        return this.getList(fieldName, FieldType.UPDATING_BLOCK_EXPRESSION);
    }

    public HashMap<String, ValueExpression<Block>> getUpdatingBlockExpressionMap(String fieldName) {
        return this.getMap(fieldName, FieldType.UPDATING_BLOCK_EXPRESSION);
    }


    @Nullable
    public ValueExpression<Block> getRandomizedBlockExpressionNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.RANDOMIZED_BLOCK_EXPRESSION);
    }

    public ValueExpression<Block> getRandomizedBlockExpression(String fieldName) {
        var value = getRandomizedBlockExpressionNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public ValueExpression<Block> getRandomizedBlockExpressionOrDefault(String fieldName, ValueExpression<Block> defaultValue) {
        var value = getRandomizedBlockExpressionNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<ValueExpression<Block>> getRandomizedBlockExpressionList(String fieldName) {
        return this.getList(fieldName, FieldType.RANDOMIZED_BLOCK_EXPRESSION);
    }

    public HashMap<String, ValueExpression<Block>> getRandomizedBlockExpressionMap(String fieldName) {
        return this.getMap(fieldName, FieldType.RANDOMIZED_BLOCK_EXPRESSION);
    }


    @Nullable
    public Condition getFixedConditionNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.FIXED_CONDITION);
    }

    public Condition getFixedCondition(String fieldName) {
        var value = getFixedConditionNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public Condition getFixedConditionOrDefault(String fieldName, Condition defaultValue) {
        var value = getFixedConditionNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<Condition> getFixedConditionList(String fieldName) {
        return this.getList(fieldName, FieldType.FIXED_CONDITION);
    }

    public HashMap<String, Condition> getFixedConditionMap(String fieldName) {
        return this.getMap(fieldName, FieldType.FIXED_CONDITION);
    }


    @Nullable
    public Condition getUpdatingConditionNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.UPDATING_CONDITION);
    }

    public Condition getUpdatingCondition(String fieldName) {
        var value = getUpdatingConditionNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public Condition getUpdatingConditionOrDefault(String fieldName, Condition defaultValue) {
        var value = getUpdatingConditionNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<Condition> getUpdatingConditionList(String fieldName) {
        return this.getList(fieldName, FieldType.UPDATING_CONDITION);
    }

    public HashMap<String, Condition> getUpdatingConditionMap(String fieldName) {
        return this.getMap(fieldName, FieldType.UPDATING_CONDITION);
    }


    @Nullable
    public Condition getRandomizedConditionNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.RANDOMIZED_CONDITION);
    }

    public Condition getRandomizedCondition(String fieldName) {
        var value = getRandomizedConditionNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public Condition getRandomizedConditionOrDefault(String fieldName, Condition defaultValue) {
        var value = getRandomizedConditionNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<Condition> getRandomizedConditionList(String fieldName) {
        return this.getList(fieldName, FieldType.RANDOMIZED_CONDITION);
    }

    public HashMap<String, Condition> getRandomizedConditionMap(String fieldName) {
        return this.getMap(fieldName, FieldType.RANDOMIZED_CONDITION);
    }


    @Nullable
    public SimulationConfig getConfigNullable(String fieldName) {
        return this.getValue(fieldName, FieldType.CONFIG);
    }

    public SimulationConfig getConfig(String fieldName) {
        var value = getConfigNullable(fieldName);
        return throwIfNull(value, fieldName);
    }

    public SimulationConfig getConfigOrDefault(String fieldName, SimulationConfig defaultValue) {
        var value = this.getConfigNullable(fieldName);
        return value != null ? value : defaultValue;
    }

    public ArrayList<SimulationConfig> getConfigList(String fieldName) {
        return this.getList(fieldName, FieldType.CONFIG);
    }

    public HashMap<String, SimulationConfig> getConfigMap(String fieldName) {
        return this.getMap(fieldName, FieldType.CONFIG);
    }


    public static <T> T throwIfNull(T result, String fieldName) {
        if (result == null) throw new RuntimeException("Field \""+fieldName+"\" was not present or was set to null.");
        return result;
    }
}
