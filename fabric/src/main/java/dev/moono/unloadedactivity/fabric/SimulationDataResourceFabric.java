package dev.moono.unloadedactivity.fabric;

#if MC_VER >= MC_1_21_10
@Deprecated
public class SimulationDataResourceFabric {}
#else
import dev.moono.unloadedactivity.datapack.SimulationDataResource;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

public class SimulationDataResourceFabric extends SimulationDataResource implements IdentifiableResourceReloadListener {
    public SimulationDataResourceFabric(boolean isBlock) {
        super(isBlock);
    }

    @Override
    public ResourceLocation getFabricId() {
        return this.isBlocks ? BLOCKS_ID : TAGS_ID;
    }
}
#endif