package com.endruqq.imperium.client.render;

import com.endruqq.imperium.entity.SoldierEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** Soldier model that raises both hands when the soldier has surrendered. */
public class SurrenderModel extends PlayerModel<SoldierEntity> {
    public SurrenderModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(SoldierEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (!e.isSurrendered()) return;
        float wave = (float) Math.sin(ageInTicks * 0.15F) * 0.05F;
        rightArm.xRot = (float) Math.PI + 0.2F + wave;
        leftArm.xRot = (float) Math.PI + 0.2F - wave;
        rightArm.yRot = 0;
        leftArm.yRot = 0;
        rightArm.zRot = 0.35F;
        leftArm.zRot = -0.35F;
        head.xRot = 0.35F;
        hat.copyFrom(head);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
    }
}
