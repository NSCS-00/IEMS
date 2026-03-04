package com.dlzstudio.iems.entities;

import com.dlzstudio.iems.IEMSMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * IEMS 实体注册表
 * 用于注册能源连接实体等
 */
public class IEMSEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(IEMSMod.MODID);

    // 能源连接实体（用于渲染激光）
    public static final Supplier<EntityType<EnergyConnectionEntity>> ENERGY_CONNECTION = ENTITIES.register(
        "energy_connection",
        () -> EntityType.Builder.<EnergyConnectionEntity>of(EnergyConnectionEntity::new, MobCategory.MISC)
            .sized(0.1f, 0.1f)
            .clientTrackingRange(64)
            .updateInterval(1)
            .setShouldReceiveVelocityUpdates(false)
            .build("energy_connection")
    );

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}
