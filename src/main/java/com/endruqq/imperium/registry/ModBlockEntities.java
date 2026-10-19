package com.endruqq.imperium.registry;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.block.SteelFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Imperium.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SteelFurnaceBlockEntity>> STEEL_FURNACE =
            BLOCK_ENTITIES.register("steel_furnace",
                    () -> BlockEntityType.Builder.of(SteelFurnaceBlockEntity::new, ModBlocks.STEEL_FURNACE.get()).build(null));

    private ModBlockEntities() {}
}
