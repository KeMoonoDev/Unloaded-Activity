package dev.moono.unloadedactivity.api;

import com.mojang.datafixers.util.Pair;
import dev.moono.unloadedactivity.GameUtils;
import dev.moono.unloadedactivity.api.context.UpdatingContext;
import dev.moono.unloadedactivity.api.simulation_method.GroupableSimulationMethod;
import dev.moono.unloadedactivity.api.context.ExpressionContext;
import dev.moono.unloadedactivity.datapack.group.GroupMemberInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

public class ActiveGroupSimulateData {
    // Simulation data that affects this data.
    public final ArrayList<ActiveGroupSimulateData> surroundingData;
    public final ArrayList<ActiveGroupSimulateData> extendingData;
    public boolean isActive;
    public int groupIndex;
    public final BlockPos position;
    private @Nullable BlockState blockState;
    public final ServerLevel level;
    private int currentUpdateCount;
    private int maxUpdateCount;
    public boolean placeBlock;
    public boolean blockIsReplaced;
    public int updateType;

    public long nextOddsSwitchDuration;
    public float currentOdds;

    private @Nullable GroupableSimulationMethod simulationMethod;
    private GroupMemberInfo groupMemberInfo;

    public ActiveGroupSimulateData(BlockPos position, BlockState blockState, @Nullable GroupableSimulationMethod simulationMethod, GroupMemberInfo groupMemberInfo, ServerLevel level) {
        this.surroundingData = new ArrayList<>();
        this.extendingData = new ArrayList<>();
        this.position = position;
        this.level = level;
        this.groupIndex = -1;
        this.currentUpdateCount = 0;
        this.maxUpdateCount = 0;
        this.placeBlock = false;
        this.updateType = Block.UPDATE_ALL;
        this.nextOddsSwitchDuration = 0;
        this.currentOdds = 0;

        this.updateBlockInfo(blockState, simulationMethod, groupMemberInfo);
    }

    public GroupMemberInfo getGroupMemberInfo() {
        return groupMemberInfo;
    }

    public Optional<GroupableSimulationMethod> getSimulationMethod() {
        return Optional.ofNullable(simulationMethod);
    }

    public int getRemainingUpdates() {
        return maxUpdateCount - currentUpdateCount;
    }

    public void addUpdateCount(int count) {
        currentUpdateCount += count;
    }

    public int getCurrentUpdateCount() {
        return currentUpdateCount;
    }

    public @Nullable BlockState getState() {
        return blockState;
    }

    public Pair<Float, Long> updateAndGetOdds(long nextWeatherSwitchDuration, UpdatingContext context) {
        if (this.nextOddsSwitchDuration <= 0) {
            if (this.simulationMethod == null) {
                this.nextOddsSwitchDuration = Long.MAX_VALUE;
                this.currentOdds = 0;
                return Pair.of(this.currentOdds, this.nextOddsSwitchDuration);
            }

            if (this.simulationMethod.advanceProbability.canBeAffectedByTime) {
                this.nextOddsSwitchDuration = this.simulationMethod.advanceProbability.getNextValueSwitchDuration(context);
            } else {
                this.nextOddsSwitchDuration = Long.MAX_VALUE;
            }

            if (this.simulationMethod.advanceProbability.canBeAffectedByWeather) {
                this.nextOddsSwitchDuration = Math.min(this.nextOddsSwitchDuration, nextWeatherSwitchDuration);
            }

            this.currentOdds = this.simulationMethod.advanceProbability.evaluate(context).floatValue();
        }
        return Pair.of(this.currentOdds, this.nextOddsSwitchDuration);
    }

    public void passTime(long duration) {
        this.nextOddsSwitchDuration -= duration;
    }


    public void updateBlockInfo(@Nullable BlockState state, @Nullable GroupableSimulationMethod simulationMethod, @Nullable GroupMemberInfo groupMemberInfo) {
        this.simulationMethod = simulationMethod;
        this.blockState = state;
        this.maxUpdateCount = 0;
        this.currentUpdateCount = 0;
        if (this.simulationMethod != null) {
            this.isActive = true;

            if (this.simulationMethod.isPrecipitation) {
                BlockPos airPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(this.position.getX(),0,this.position.getZ()));
                if (!airPos.equals(this.position)) {
                    BlockPos groundPos = airPos.below();
                    if (!groundPos.equals(this.position)) {
                        this.isActive = false;
                    }
                }
            }

            if (this.isActive)
                this.isActive = this.simulationMethod.canSimulate(this.blockState, this.level, this.position);

            if (this.isActive) {
                this.maxUpdateCount = this.simulationMethod.getMaxUpdateCount(this.blockState, this.level, this.position);
                this.currentUpdateCount = 0;
            }
        } else {
            this.isActive = false;
        }

        boolean invalidateOtherCaches = this.groupMemberInfo != groupMemberInfo;
        GroupMemberInfo oldGroupMemberInfo = this.groupMemberInfo;

        this.groupMemberInfo = groupMemberInfo;
        this.invalidateAllCaches();
        for (var data : this.extendingData) {
            data.groupMemberInfo = groupMemberInfo;
            data.invalidateAllCaches();
        }

        if (invalidateOtherCaches) {
            for (var data : this.surroundingData) {
                data.invalidateAndFixCaches(oldGroupMemberInfo, this);
            }
            for (var extendedData : this.extendingData) {
                for (var data : this.surroundingData) {
                    data.invalidateAndFixCaches(oldGroupMemberInfo, extendedData);
                }
            }
        }
    }

    @Nullable
    private Float groupSum = null;

    @Nullable
    private Integer groupCount = null;

    @Nullable
    private Integer groupHigherValueCount = null;

    @Nullable
    private Integer groupLowerValueCount = null;

    @Nullable
    private Integer groupEqualValueCount = null;

    public void invalidateAllCaches() {
        this.nextOddsSwitchDuration = 0;
        this.currentOdds = 0;
        this.groupSum = null;
        this.groupHigherValueCount = null;
        this.groupLowerValueCount = null;
        this.groupEqualValueCount = null;
    }

    public boolean isIgnored(ActiveGroupSimulateData groupSimulateData) {
        if (this.groupMemberInfo == null || this.groupMemberInfo.ignoredOffsets.isEmpty()) {
            return false;
        }

        Vec3i deltaPos = groupSimulateData.position.subtract(this.position);

        return this.groupMemberInfo.ignoredOffsets.contains(deltaPos);
    }

    public void invalidateAndFixCaches(GroupMemberInfo oldMemberInfo, ActiveGroupSimulateData newGroupSimulateData) {
        this.nextOddsSwitchDuration = 0;
        this.currentOdds = 0;

        if (this.isIgnored(newGroupSimulateData)) {
            return;
        }

        GroupMemberInfo newMemberInfo = newGroupSimulateData.groupMemberInfo;

        if (groupSum != null) {
            groupSum -= oldMemberInfo.value;
            if (newMemberInfo != null) {
                groupSum += newMemberInfo.value;
            }
        }

        if (groupCount != null) {
            if (newMemberInfo == null) {
                groupCount -= 1;
            }
        }

        if (groupHigherValueCount != null) {
            if (oldMemberInfo.value > groupMemberInfo.value) {
                groupHigherValueCount -= 1;
            }
            if (newMemberInfo != null) {
                if (newMemberInfo.value > groupMemberInfo.value) {
                    groupHigherValueCount += 1;
                }
            }
        }

        if (groupLowerValueCount != null) {
            if (oldMemberInfo.value < groupMemberInfo.value) {
                groupLowerValueCount -= 1;
            }
            if (newMemberInfo != null) {
                if (newMemberInfo.value < groupMemberInfo.value) {
                    groupLowerValueCount += 1;
                }
            }
        }

        if (groupEqualValueCount != null) {
            if (oldMemberInfo.value == groupMemberInfo.value) {
                groupEqualValueCount -= 1;
            }
            if (newMemberInfo != null) {
                if (newMemberInfo.value == groupMemberInfo.value) {
                    groupEqualValueCount += 1;
                }
            }
        }

        if (newMemberInfo == null) {
            this.surroundingData.removeIf(data -> data == newGroupSimulateData);
        }
    }

    public float getGroupSum() {
        if (groupSum != null) {
            return groupSum;
        }

        float sum = this.groupMemberInfo.value;

        for (var surrounding : this.surroundingData) {
            if (this.isIgnored(surrounding)) {
                continue;
            }
            sum += surrounding.getGroupMemberInfo().value;
        }

        groupSum = sum;
        return sum;
    }

    public int getGroupCount() {
        if (groupCount != null) {
            return groupCount;
        }

        int count = 1;

        for (var surrounding : this.surroundingData) {
            if (this.isIgnored(surrounding)) {
                continue;
            }
            count++;
        }

        groupCount = count;
        return count;
    }

    public int getGroupHigherValueCount() {
        if (groupHigherValueCount != null) {
            return groupHigherValueCount;
        }

        int count = 0;

        float thisValue = this.getGroupMemberInfo().value;

        for (var surrounding : this.surroundingData) {
            if (this.isIgnored(surrounding)) {
                continue;
            }
            if (surrounding.getGroupMemberInfo().value > thisValue) {
                count++;
            }
        }

        groupHigherValueCount = count;

        return count;
    }

    public int getGroupLowerValueCount() {
        if (groupLowerValueCount != null) {
            return groupLowerValueCount;
        }

        int count = 0;

        float thisValue = this.getGroupMemberInfo().value;

        for (var surrounding : this.surroundingData) {
            if (this.isIgnored(surrounding)) {
                continue;
            }
            if (surrounding.getGroupMemberInfo().value < thisValue) {
                count++;
            }
        }

        groupLowerValueCount = count;

        return count;
    }

    public int getGroupEqualValueCount() {
        if (groupEqualValueCount != null) {
            return groupEqualValueCount;
        }

        int count = 1;

        float thisValue = this.getGroupMemberInfo().value;

        for (var surrounding : this.surroundingData) {
            if (this.isIgnored(surrounding)) {
                continue;
            }
            if (surrounding.getGroupMemberInfo().value == thisValue) {
                count++;
            }
        }

        groupEqualValueCount = count;

        return count;
    }

    public float getGroupRandomHigherValue() {
        ArrayList<Float> availableValues = new ArrayList<>(this.surroundingData.size());

        float thisValue = this.getGroupMemberInfo().value;

        for (var surrounding : this.surroundingData) {
            if (this.isIgnored(surrounding)) {
                continue;
            }
            float surroundingValue = surrounding.getGroupMemberInfo().value;
            if (surroundingValue > thisValue) {
                availableValues.add(surroundingValue);
            }
        }

        if (availableValues.isEmpty()) {
            return 0;
        }

        return availableValues.get(GameUtils.getRand(level).nextInt(availableValues.size()));
    }

    public float getGroupRandomHigherOrEqualValue() {
        ArrayList<Float> availableValues = new ArrayList<>(this.surroundingData.size() + 1);

        float thisValue = this.getGroupMemberInfo().value;
        availableValues.add(thisValue);

        for (var surrounding : this.surroundingData) {
            if (this.isIgnored(surrounding)) {
                continue;
            }
            float surroundingValue = surrounding.getGroupMemberInfo().value;
            if (surroundingValue >= thisValue) {
                availableValues.add(surroundingValue);
            }
        }

        if (availableValues.isEmpty()) {
            return 0;
        }

        return availableValues.get(GameUtils.getRand(level).nextInt(availableValues.size()));
    }

    public float getGroupRandomLowerValue() {
        ArrayList<Float> availableValues = new ArrayList<>(this.surroundingData.size());

        float thisValue = this.getGroupMemberInfo().value;

        for (var surrounding : this.surroundingData) {
            if (this.isIgnored(surrounding)) {
                continue;
            }
            float surroundingValue = surrounding.getGroupMemberInfo().value;
            if (surroundingValue < thisValue) {
                availableValues.add(surroundingValue);
            }
        }

        if (availableValues.isEmpty()) {
            return 0;
        }

        return availableValues.get(GameUtils.getRand(level).nextInt(availableValues.size()));
    }

    public float getGroupRandomLowerOrEqualValue() {
        ArrayList<Float> availableValues = new ArrayList<>(this.surroundingData.size() + 1);

        float thisValue = this.getGroupMemberInfo().value;
        availableValues.add(thisValue);

        for (var surrounding : this.surroundingData) {
            if (this.isIgnored(surrounding)) {
                continue;
            }
            float surroundingValue = surrounding.getGroupMemberInfo().value;
            if (surroundingValue <= thisValue) {
                availableValues.add(surroundingValue);
            }
        }

        if (availableValues.isEmpty()) {
            return 0;
        }

        return availableValues.get(GameUtils.getRand(level).nextInt(availableValues.size()));
    }

    public float getGroupRandomNotEqualValue() {
        ArrayList<Float> availableValues = new ArrayList<>(this.surroundingData.size());

        float thisValue = this.getGroupMemberInfo().value;

        for (var surrounding : this.surroundingData) {
            if (this.isIgnored(surrounding)) {
                continue;
            }
            float surroundingValue = surrounding.getGroupMemberInfo().value;
            if (surroundingValue != thisValue) {
                availableValues.add(surroundingValue);
            }
        }

        if (availableValues.isEmpty()) {
            return 0;
        }

        return availableValues.get(GameUtils.getRand(level).nextInt(availableValues.size()));
    }
}
