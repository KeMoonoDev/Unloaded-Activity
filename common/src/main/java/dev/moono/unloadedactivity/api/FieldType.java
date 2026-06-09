package dev.moono.unloadedactivity.api;

#if MC_VER >= MC_1_19_4
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.moono.unloadedactivity.UnloadedActivity;
import dev.moono.unloadedactivity.datapack.Condition;
import dev.moono.unloadedactivity.datapack.ValueExpression;
import net.minecraft.core.registries.BuiltInRegistries;
#else
import net.minecraft.core.Registry;
#endif

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum FieldType {
    NUMBER {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            DataResult<Number> numberResult = ops.getNumberValue(input);
            if (numberResult.result().isPresent()) {
                return numberResult.result().get();
            }

            DataResult<String> stringResult = ops.getStringValue(input);

            if (stringResult.result().isPresent()) {
                String idUnparsed = stringResult.result().get();
                Identifier id;
                if (idUnparsed.indexOf(':') >= 0) {
                    id = Identifier.parse(idUnparsed);
                } else {
                    id = Identifier.parse(UnloadedActivity.MOD_ID+":"+idUnparsed);
                }
                Optional<Number> maybeNumber = UnloadedActivity.numberFetcherRegistry.getNumber(id);
                if (maybeNumber.isPresent()) return maybeNumber.get();
                throw new RuntimeException("The ID \""+id+"\" is not mapped to a number.");
            }

            throw new RuntimeException("Number field wasn't a number value or an ID mapped to a number.");
        }
    },
    BOOLEAN {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            return ops.getBooleanValue(input).getOrThrow();
        }
    },
    STRING {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            return ops.getStringValue(input).getOrThrow();
        }
    },
    BLOCK {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            String unparsedBlockId = ops.getStringValue(input).getOrThrow();
            var blockId = Identifier.parse(unparsedBlockId);

            #if MC_VER >= MC_1_19_4
            Optional<Block> maybeBlock = BuiltInRegistries.BLOCK.getOptional(blockId);
            #else
            Optional<Block> maybeBlock = Registry.BLOCK.getOptional(blockId);
            #endif

            if (maybeBlock.isEmpty())
                throw new RuntimeException(blockId + " is not a valid block.");

            return maybeBlock.get();
        }
    },
    ENTITY_TYPE {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            String unparsedEntityId = ops.getStringValue(input).getOrThrow();
            var entityId = Identifier.parse(unparsedEntityId);

            #if MC_VER >= MC_1_19_4
            Optional<EntityType<?>> maybeEntity = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId);
            #else
            Optional<EntityType<?>> maybeEntity = Registry.ENTITY_TYPE.getOptional(entityId);
            #endif

            if (maybeEntity.isEmpty())
                throw new RuntimeException(entityId + " is not a valid entity.");

            return maybeEntity.get();
        }
    },
    FIXED_NUMBER_EXPRESSION {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            ValueExpression<Number> numberExpression = ValueExpression.parseNumber(ops, input);

            if (numberExpression.isRandom())
                throw new RuntimeException("Number expression is not fixed. The result can be random.");

            if (numberExpression.canBeAffectedByTime())
                throw new RuntimeException("Number expression is not fixed. It can be affected by time.");

            if (numberExpression.canBeAffectedByWeather())
                throw new RuntimeException("Number expression is not fixed. It can be affected by weather.");

            if (superData != null) numberExpression.replaceSuper((ValueExpression<Number>)superData);

            return numberExpression;
        }
    },
    UPDATING_NUMBER_EXPRESSION {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            ValueExpression<Number> numberExpression = ValueExpression.parseNumber(ops, input);

            if (numberExpression.isRandom())
                throw new RuntimeException("Number expression is not updating. The result can be random.");

            if (superData != null) numberExpression.replaceSuper((ValueExpression<Number>)superData);

            return numberExpression;
        }
    },
    RANDOMIZED_NUMBER_EXPRESSION {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            ValueExpression<Number> numberExpression = ValueExpression.parseNumber(ops, input);

            if (superData != null) numberExpression.replaceSuper((ValueExpression<Number>)superData);

            return numberExpression;
        }
    },
    FIXED_BLOCK_EXPRESSION {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            ValueExpression<String> stringExpression = ValueExpression.parseString(ops, input);

            ValueExpression<Block> blockExpression = stringExpression.map(stringId -> {
                if (stringId == null) return null;

                var blockId = #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif.parse(stringId);

                #if MC_VER >= MC_1_19_4
                Optional<Block> maybeBlock = BuiltInRegistries.BLOCK.getOptional(blockId);
                #else
                Optional<Block> maybeBlock = Registry.BLOCK.getOptional(blockId);
                #endif

                if (maybeBlock.isEmpty())
                    throw new RuntimeException(blockId + " is not a valid block.");

                return maybeBlock.get();
            });

            if (blockExpression.isRandom())
                throw new RuntimeException("Block expression is not fixed. The result can be random.");

            if (blockExpression.canBeAffectedByTime())
                throw new RuntimeException("Block expression is not fixed. It can be affected by time.");

            if (blockExpression.canBeAffectedByWeather())
                throw new RuntimeException("Block expression is not fixed. It can be affected by weather.");

            if (superData != null) blockExpression.replaceSuper((ValueExpression<Block>)superData);

            return blockExpression;
        }
    },
    UPDATING_BLOCK_EXPRESSION {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            ValueExpression<String> stringExpression = ValueExpression.parseString(ops, input);

            ValueExpression<Block> blockExpression = stringExpression.map(stringId -> {
                if (stringId == null) return null;

                var blockId = #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif.parse(stringId);

                #if MC_VER >= MC_1_19_4
                Optional<Block> maybeBlock = BuiltInRegistries.BLOCK.getOptional(blockId);
                #else
                Optional<Block> maybeBlock = Registry.BLOCK.getOptional(blockId);
                #endif

                if (maybeBlock.isEmpty())
                    throw new RuntimeException(blockId + " is not a valid block.");

                return maybeBlock.get();
            });

            if (blockExpression.isRandom())
                throw new RuntimeException("Block expression is not updating. The result can be random.");

            if (superData != null) blockExpression.replaceSuper((ValueExpression<Block>)superData);

            return blockExpression;
        }
    },
    RANDOMIZED_BLOCK_EXPRESSION {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            ValueExpression<String> stringExpression = ValueExpression.parseString(ops, input);

            ValueExpression<Block> blockExpression = stringExpression.map(stringId -> {
                if (stringId == null) return null;

                var blockId = #if MC_VER >= MC_1_21_11 Identifier #else ResourceLocation #endif.parse(stringId);

                #if MC_VER >= MC_1_19_4
                Optional<Block> maybeBlock = BuiltInRegistries.BLOCK.getOptional(blockId);
                #else
                Optional<Block> maybeBlock = Registry.BLOCK.getOptional(blockId);
                #endif

                if (maybeBlock.isEmpty())
                    throw new RuntimeException(blockId + " is not a valid block.");

                return maybeBlock.get();
            });

            if (superData != null) blockExpression.replaceSuper((ValueExpression<Block>)superData);

            return blockExpression;
        }
    },
    FIXED_CONDITION {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            Condition condition = Condition.parse(ops, input).getOrThrow();

            if (condition.isRandom())
                throw new RuntimeException("Condition is not fixed. The result can be random.");

            if (condition.canBeAffectedByTime())
                throw new RuntimeException("Condition is not fixed. It can be affected by time.");

            if (condition.canBeAffectedByWeather())
                throw new RuntimeException("Condition is not fixed. It can be affected by weather.");

            return condition;
        }
    },
    UPDATING_CONDITION {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            Condition condition = Condition.parse(ops, input).getOrThrow();

            if (condition.isRandom())
                throw new RuntimeException("Condition is not updating. The result can be random.");

            return condition;
        }
    },
    RANDOMIZED_CONDITION {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            JsonOps ops = JsonOps.INSTANCE;
            Condition condition = Condition.parse(ops, input).getOrThrow();

            return condition;
        }
    },
    CONFIG {
        @Override
        public <T> Object deserialize(JsonElement input, @Nullable Object superData) {
            if (!input.isJsonObject()) {
                throw new RuntimeException("Config wasn't a JsonObject.");
            }
            SimulationConfig finalConfig = superData == null ? new SimulationConfig() : (SimulationConfig)superData;
            finalConfig.merge(input.getAsJsonObject());
            return finalConfig;
        }
    };

    public abstract<T> Object deserialize(JsonElement input, @Nullable Object superData);
}
