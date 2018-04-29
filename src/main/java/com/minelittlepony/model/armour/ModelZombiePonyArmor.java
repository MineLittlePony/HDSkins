package com.minelittlepony.model.armour;

import com.minelittlepony.render.AbstractPonyRenderer;

public class ModelZombiePonyArmor extends ModelPonyArmor {

    // Copied from ModelZombiePony
    @Override
    protected void rotateRightArm(float move, float tick) {
        if (rightArmPose != ArmPose.EMPTY) return;

        if (islookAngleRight(move)) {
            rotateArmHolding(bipedRightArm, 1, swingProgress, tick);
        } else {
            rotateArmHolding(bipedLeftArm, -1, swingProgress, tick);
        }
    }

    @Override
    protected void rotateLeftArm(float move, float tick) {
        // Zombies are unidexterous.
    }

    @Override
    protected void fixSpecialRotationPoints(float move) {
        if (rightArmPose != ArmPose.EMPTY) return;

        if (islookAngleRight(move)) {
            AbstractPonyRenderer.shiftRotationPoint(bipedRightArm, 0.5F, 1.5F, 3);
        } else {
            AbstractPonyRenderer.shiftRotationPoint(bipedLeftArm, -0.5F, 1.5F, 3);
        }
    }
}
