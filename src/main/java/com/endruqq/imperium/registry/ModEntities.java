package com.endruqq.imperium.registry;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.entity.CitizenEntity;
import com.endruqq.imperium.entity.SoldierEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Imperium.MODID);

    public static final RegistryObject<EntityType<CitizenEntity>> CITIZEN = ENTITIES.register("citizen",
            () -> EntityType.Builder.<CitizenEntity>of(CitizenEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build(Imperium.MODID + ":citizen"));

    public static final RegistryObject<EntityType<SoldierEntity>> SOLDIER = ENTITIES.register("soldier",
            () -> EntityType.Builder.<SoldierEntity>of(SoldierEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(12).build(Imperium.MODID + ":soldier"));

    private ModEntities() {}
}
