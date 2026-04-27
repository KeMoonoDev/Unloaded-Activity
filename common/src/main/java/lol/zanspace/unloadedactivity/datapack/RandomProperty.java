package lol.zanspace.unloadedactivity.datapack;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import net.minecraft.util.RandomSource;

import java.util.Optional;

import static lol.zanspace.unloadedactivity.datapack.IncompleteSimulationData.returnError;

public class RandomProperty {
    public String propertyName;
    public PropertyType propertyType;
    public float probability;
    public int successValue;
    public int failValue;

    public RandomProperty(IncompleteRandomProperty incomplete, String propertyName) {
        this.propertyName = propertyName;

        if (incomplete.propertyType.isEmpty())
            throw new RuntimeException("property_type cannot be empty.");

        this.propertyType = incomplete.propertyType.get();

        if (incomplete.probability.isEmpty())
            throw new RuntimeException("probability cannot be empty.");

        this.probability = incomplete.probability.get();

        if (this.propertyType != PropertyType.BOOL) {
            if (incomplete.successValue.isEmpty())
                throw new RuntimeException("success cannot be empty, unless property_type is bool.");

            if (incomplete.failValue.isEmpty())
                throw new RuntimeException("fail cannot be empty, unless property_type is bool.");
        }

        this.successValue = incomplete.successValue.orElse(1);
        this.failValue = incomplete.failValue.orElse(0);
    }

    public int getRandomValue(RandomSource random) {
        return random.nextDouble() < this.probability ? this.successValue : this.failValue;
    }
}
