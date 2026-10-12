package com.turboarmor.client;

import com.turboarmor.TurboArmor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TurboArmor.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TurboClientSetup {
    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(TurboArmorModel.OUTER, () -> TurboArmorModel.createLayer(false));
        e.registerLayerDefinition(TurboArmorModel.INNER, () -> TurboArmorModel.createLayer(true));
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(TurboArmor.ID, "turbo_hud"), new TurboHud());
    }
}
