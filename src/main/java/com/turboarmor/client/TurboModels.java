package com.turboarmor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;

/** Crea (una sola vez) los modelos 3D de la armadura y los entrega según la pieza. */
public final class TurboModels {
    private static TurboArmorModel outer, inner;

    private TurboModels() {}

    public static HumanoidModel<?> forSlot(EquipmentSlot slot) {
        TurboArmorModel m;
        if (slot == EquipmentSlot.LEGS) {
            if (inner == null) inner = new TurboArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(TurboArmorModel.INNER));
            m = inner;
        } else {
            if (outer == null) outer = new TurboArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(TurboArmorModel.OUTER));
            m = outer;
        }
        m.setVisibility(slot);
        return m;
    }
}
