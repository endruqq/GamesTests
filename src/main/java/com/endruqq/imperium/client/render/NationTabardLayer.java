package com.endruqq.imperium.client.render;

import com.endruqq.imperium.Imperium;
import com.endruqq.imperium.client.ClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.function.ToIntFunction;

/** A tabard/sash in the colour of the unit's nation. */
public class NationTabardLayer<T extends Mob> extends RenderLayer<T, PlayerModel<T>> {
    private final ResourceLocation texture;
    private final ToIntFunction<T> nationOf;

    public NationTabardLayer(RenderLayerParent<T, PlayerModel<T>> parent, String texture, ToIntFunction<T> nationOf) {
        super(parent);
        this.texture = new ResourceLocation(Imperium.MODID, "textures/entity/" + texture + ".png");
        this.nationOf = nationOf;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        int nation = nationOf.applyAsInt(entity);
        if (nation < 0) return;
        float[] c = ClientState.rgbOf(nation);
        renderColoredCutoutModel(getParentModel(), texture, pose, buffer, light, entity, c[0], c[1], c[2]);
    }
}
