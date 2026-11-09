package dev.moono.unloadedactivity.config;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum SimulationClock implements StringRepresentable {
    DAY_TIME,
    GAME_TIME,
    SYSTEM_TIME;

    @Override
    public @NotNull String getSerializedName() {
        return this.name();
    }
}
