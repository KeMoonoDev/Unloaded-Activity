package lol.zanspace.unloadedactivity.api;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;

import java.util.*;

public class Unused {
    public final ImmutableList<Pair<String, TypeDescriptor>> fieldsList;

    public Unused(List<Pair<String, TypeDescriptor>> fieldsList) {
        this.fieldsList = ImmutableList.copyOf(fieldsList);
    }

    public TypeDescriptor getTypeOrThrow(String fieldName) {
        for (var pair : fieldsList) {
            if (!pair.getFirst().equals(fieldName)) continue;
            return pair.getSecond();
        }

        throw new RuntimeException("Field " + fieldName + " does not exist.");
    }

    public static SchemaBuilder builder() {
        return new SchemaBuilder();
    }

    public static class SchemaBuilder {
        // Use a list instead of a map so that field errors can be consistent.
        private ArrayList<Pair<String, TypeDescriptor>> fieldsList = new ArrayList<>();

        public Unused build() {
            return new Unused(fieldsList);
        }

        public void throwIfPresent(String fieldName) {
            for (var triple : fieldsList) {
                if (triple.getFirst().equals(fieldName)) {
                    throw new RuntimeException("Field with name "+fieldName+" has already been added to the builder.");
                }
            }
        }

        public void addField(String fieldName, FieldType fieldType, boolean allowNull) {
            throwIfPresent(fieldName);
            fieldsList.add(Pair.of(fieldName, new TypeDescriptor(fieldType, allowNull ? ContainerKind.NULLABLE : ContainerKind.NONE)));
        }

        public void addListField(String fieldName, FieldType fieldType) {
            throwIfPresent(fieldName);
            fieldsList.add(Pair.of(fieldName, new TypeDescriptor(fieldType, ContainerKind.LIST)));

        }

        public void addMapField(String fieldName, FieldType fieldType) {
            throwIfPresent(fieldName);
            fieldsList.add(Pair.of(fieldName, new TypeDescriptor(fieldType, ContainerKind.MAP)));

        }

        public void addNumber(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.NUMBER, allowNull);
        }

        public void addNumberList(String fieldName) {
            addListField(fieldName, FieldType.NUMBER);
        }

        public void addNumberMap(String fieldName) {
            addMapField(fieldName, FieldType.NUMBER);
        }

        public void addBoolean(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.BOOLEAN, allowNull);
        }

        public void addBooleanList(String fieldName) {
            addListField(fieldName, FieldType.BOOLEAN);
        }

        public void addBooleanMap(String fieldName) {
            addMapField(fieldName, FieldType.BOOLEAN);
        }

        public void addString(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.STRING, allowNull);
        }

        public void addStringList(String fieldName) {
            addListField(fieldName, FieldType.STRING);
        }

        public void addStringMap(String fieldName) {
            addMapField(fieldName, FieldType.STRING);
        }

        public void addBlock(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.BLOCK, allowNull);
        }

        public void addBlockList(String fieldName) {
            addListField(fieldName, FieldType.BLOCK);
        }

        public void addBlockMap(String fieldName) {
            addMapField(fieldName, FieldType.BLOCK);
        }

        public void addEntityType(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.ENTITY_TYPE, allowNull);
        }

        public void addEntityTypeList(String fieldName) {
            addListField(fieldName, FieldType.ENTITY_TYPE);
        }

        public void addEntityTypeMap(String fieldName) {
            addMapField(fieldName, FieldType.ENTITY_TYPE);
        }

        public void addFixedNumberExpression(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.FIXED_NUMBER_EXPRESSION, allowNull);
        }

        public void addFixedNumberExpressionList(String fieldName) {
            addListField(fieldName, FieldType.FIXED_NUMBER_EXPRESSION);
        }

        public void addFixedNumberExpressionMap(String fieldName) {
            addMapField(fieldName, FieldType.FIXED_NUMBER_EXPRESSION);
        }

        public void addUpdatingNumberExpression(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.UPDATING_NUMBER_EXPRESSION, allowNull);
        }

        public void addUpdatingNumberExpressionList(String fieldName) {
            addListField(fieldName, FieldType.UPDATING_NUMBER_EXPRESSION);
        }

        public void addUpdatingNumberExpressionMap(String fieldName) {
            addMapField(fieldName, FieldType.UPDATING_NUMBER_EXPRESSION);
        }

        public void addRandomizedNumberExpression(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.RANDOMIZED_NUMBER_EXPRESSION, allowNull);
        }

        public void addRandomizedNumberExpressionList(String fieldName) {
            addListField(fieldName, FieldType.RANDOMIZED_NUMBER_EXPRESSION);
        }

        public void addRandomizedNumberExpressionMap(String fieldName) {
            addMapField(fieldName, FieldType.RANDOMIZED_NUMBER_EXPRESSION);
        }


        public void addFixedBlockExpression(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.FIXED_BLOCK_EXPRESSION, allowNull);
        }

        public void addFixedBlockExpressionList(String fieldName) {
            addListField(fieldName, FieldType.FIXED_BLOCK_EXPRESSION);
        }

        public void addFixedBlockExpressionMap(String fieldName) {
            addMapField(fieldName, FieldType.FIXED_BLOCK_EXPRESSION);
        }

        public void addUpdatingBlockExpression(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.UPDATING_BLOCK_EXPRESSION, allowNull);
        }

        public void addUpdatingBlockExpressionList(String fieldName) {
            addListField(fieldName, FieldType.UPDATING_BLOCK_EXPRESSION);
        }

        public void addUpdatingBlockExpressionMap(String fieldName) {
            addMapField(fieldName, FieldType.UPDATING_BLOCK_EXPRESSION);
        }

        public void addRandomizedBlockExpression(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.RANDOMIZED_BLOCK_EXPRESSION, allowNull);
        }

        public void addRandomizedBlockExpressionList(String fieldName) {
            addListField(fieldName, FieldType.RANDOMIZED_BLOCK_EXPRESSION);
        }

        public void addRandomizedBlockExpressionMap(String fieldName) {
            addMapField(fieldName, FieldType.RANDOMIZED_BLOCK_EXPRESSION);
        }


        public void addFixedCondition(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.FIXED_CONDITION, allowNull);
        }

        public void addFixedConditionList(String fieldName) {
            addListField(fieldName, FieldType.FIXED_CONDITION);
        }

        public void addFixedConditionMap(String fieldName) {
            addMapField(fieldName, FieldType.FIXED_CONDITION);
        }

        public void addUpdatingCondition(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.UPDATING_CONDITION, allowNull);
        }

        public void addUpdatingConditionList(String fieldName) {
            addListField(fieldName, FieldType.UPDATING_CONDITION);
        }

        public void addUpdatingConditionMap(String fieldName) {
            addMapField(fieldName, FieldType.UPDATING_CONDITION);
        }

        public void addRandomizedCondition(String fieldName, boolean allowNull) {
            addField(fieldName, FieldType.RANDOMIZED_CONDITION, allowNull);
        }

        public void addRandomizedConditionList(String fieldName) {
            addListField(fieldName, FieldType.RANDOMIZED_CONDITION);
        }

        public void addRandomizedConditionMap(String fieldName) {
            addMapField(fieldName, FieldType.RANDOMIZED_CONDITION);
        }
    }
}
