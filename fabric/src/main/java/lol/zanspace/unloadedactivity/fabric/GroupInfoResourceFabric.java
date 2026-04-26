package lol.zanspace.unloadedactivity.fabric;

#if MC_VER >= MC_1_21_10
@Deprecated
public class GroupInfoResourceFabric {}
#else
import lol.zanspace.unloadedactivity.datapack.GroupInfoResource;
import lol.zanspace.unloadedactivity.datapack.SimulationDataResource;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

public class GroupInfoResourceFabric extends GroupInfoResource implements IdentifiableResourceReloadListener {
    public GroupInfoResourceFabric() {
        super();
    }

    @Override
    public ResourceLocation getFabricId() {
        return GROUPS_ID;
    }
}
#endif