package lol.zanspace.unloadedactivity.api.number_fetchers;

import lol.zanspace.unloadedactivity.api.NumberFetcher;
import lol.zanspace.unloadedactivity.datapack.ValueContext;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class IsBlockTagValue implements NumberFetcher {
    private Vec3i offset;
    private TagKey<Block> blockTag;

    public IsBlockTagValue(TagKey<Block> blockTag) {
        this(blockTag, Vec3i.ZERO);
    }

    public IsBlockTagValue(TagKey<Block> blockTag, Vec3i offset) {
        this.blockTag = blockTag;
        this.offset = offset;
    }

    @Override
    public Number evaluate(ValueContext context) {
        return context.level.getBlockState(context.pos.offset(offset)).is(blockTag) ? 1 : 0;
    }

    @Override
    public boolean canBeAffectedByWeather() {
        return false;
    }

    @Override
    public boolean canBeAffectedByTime() {
        return false;
    }

    @Override
    public boolean isRandom() {
        return false;
    }

    @Override
    public long getNextValueSwitchDuration(ValueContext context) {
        return Long.MAX_VALUE;
    }
}
