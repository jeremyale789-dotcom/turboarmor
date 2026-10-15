package com.turboarmor.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.turboarmor.TurboArmor;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Tecla para mostrar/ocultar la interfaz del casco. Se usa junto con CTRL.
 * Por defecto es la tecla "+" del teclado latino (la que está junto a la P; en un teclado inglés es "]").
 * Se puede cambiar en Opciones > Controles > Turbo Armor.
 */
@Mod.EventBusSubscriber(modid = TurboArmor.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TurboKeys {
    public static final KeyMapping TOGGLE = new KeyMapping("key.turboarmor.toggle_ui",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, "key.categories.turboarmor");

    private TurboKeys() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent e) { e.register(TOGGLE); }
}
