package com.endruqq.imperium.registry;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.block.ConstructionSiteBlock;
import com.endruqq.imperium.block.ProvinceCoreBlock;
import com.endruqq.imperium.block.SteelFurnaceBlock;
import com.endruqq.imperium.block.TreasuryBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Imperium.MODID);

    public static final RegistryObject<Block> PROVINCE_CORE = BLOCKS.register("province_core",
            () -> new ProvinceCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
                    .strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.METAL)
                    .lightLevel(s -> 12).pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Block> CONSTRUCTION_SITE = BLOCKS.register("construction_site",
            () -> new ConstructionSiteBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(4.0F, 6.0F).noLootTable().sound(SoundType.WOOD)
                    .lightLevel(s -> s.getValue(ConstructionSiteBlock.ACTIVE) ? 9 : 4).pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Block> TREASURY = BLOCKS.register("treasury",
            () -> new TreasuryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
                    .strength(5.0F, 1200.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final RegistryObject<Block> STEEL_FURNACE = BLOCKS.register("steel_furnace",
            () -> new SteelFurnaceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(s -> s.getValue(SteelFurnaceBlock.LIT) ? 13 : 0)));

    public static final RegistryObject<Block> STEEL_BLOCK = BLOCKS.register("steel_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(6.0F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));

    private ModBlocks() {}
}
