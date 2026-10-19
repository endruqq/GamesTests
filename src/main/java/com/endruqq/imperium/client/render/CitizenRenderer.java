package com.endruqq.imperium.client.render;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.entity.CitizenEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CitizenRenderer extends HumanoidMobRenderer<CitizenEntity, PlayerModel<CitizenEntity>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[CitizenEntity.SKINS * 2];

    static {
        for (int i = 0; i < CitizenEntity.SKINS; i++) {
            TEXTURES[i] = new ResourceLocation(Imperium.MODID, "textures/entity/citizen/man_" + i + ".png");
            TEXTURES[CitizenEntity.SKINS + i] = new ResourceLocation(Imperium.MODID, "textures/entity/citizen/woman_" + i + ".png");
        }
    }

    public CitizenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new NationTabardLayer<>(this, "sash", CitizenEntity::getNationId));
    }

    @Override
    public ResourceLocation getTextureLocation(CitizenEntity e) {
        int skin = Math.floorMod(e.getSkin(), CitizenEntity.SKINS);
        return TEXTURES[(e.isFemale() ? CitizenEntity.SKINS : 0) + skin];
    }
}
