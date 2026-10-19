package com.endruqq.imperium.client.render;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.entity.UnitType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

public class SoldierRenderer extends HumanoidMobRenderer<SoldierEntity, PlayerModel<SoldierEntity>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[UnitType.values().length];

    static {
        for (UnitType t : UnitType.values()) {
            TEXTURES[t.ordinal()] = new ResourceLocation(Imperium.MODID, "textures/entity/soldier/" + t.id() + ".png");
        }
    }

    public SoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SurrenderModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        addLayer(new NationTabardLayer<>(this, "tabard", SoldierEntity::getNationId));
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(SoldierEntity e) {
        return TEXTURES[e.getUnitType().ordinal()];
    }

    @Override
    public void render(SoldierEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buf, int light) {
        PlayerModel<SoldierEntity> m = getModel();
        m.rightArmPose = pose(e, e.getMainHandItem(), InteractionHand.MAIN_HAND);
        m.leftArmPose = pose(e, e.getOffhandItem(), InteractionHand.OFF_HAND);
        super.render(e, yaw, pt, pose, buf, light);
    }

    private static HumanoidModel.ArmPose pose(SoldierEntity e, ItemStack stack, InteractionHand hand) {
        if (stack.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
        if (e.isUsingItem() && e.getUsedItemHand() == hand && stack.getItem() instanceof BowItem) return HumanoidModel.ArmPose.BOW_AND_ARROW;
        if (stack.getItem() instanceof ShieldItem && e.isUsingItem() && e.getUsedItemHand() == hand) return HumanoidModel.ArmPose.BLOCK;
        return HumanoidModel.ArmPose.ITEM;
    }
}
