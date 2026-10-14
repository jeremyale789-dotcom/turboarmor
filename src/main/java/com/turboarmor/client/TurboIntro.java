package com.turboarmor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.turboarmor.TurboArmor;
import com.turboarmor.TurboArmorItem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Intro al ponerte el casco: la pantalla se llena de estática azul y aparecen muchos paneles con marco azul brillante
 * (como en la imagen de referencia) mostrando los memes; luego todo desaparece. Dura unos 5 segundos.
 */
@Mod.EventBusSubscriber(modid = TurboArmor.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TurboIntro {
    private static final ResourceLocation[] MEME = {
            ResourceLocation.fromNamespaceAndPath(TurboArmor.ID, "textures/gui/meme_1.png"),
            ResourceLocation.fromNamespaceAndPath(TurboArmor.ID, "textures/gui/meme_2.png"),
            ResourceLocation.fromNamespaceAndPath(TurboArmor.ID, "textures/gui/meme_3.png"),
            ResourceLocation.fromNamespaceAndPath(TurboArmor.ID, "textures/gui/meme_4.png")};
    private static final int[] MEME_H = {220, 256, 253, 220};      // alto de cada imagen (ancho = 256)
    private static final ResourceLocation STATIC = ResourceLocation.fromNamespaceAndPath(TurboArmor.ID, "textures/gui/static.png");

    private static final long TOTAL = 5200;                        // duración total (ms)
    private static final int PANELS = 22;

    private record Panel(int meme, float fx, float fy, int width, long spawn, long life) {}

    private static long start = -1;
    private static Boolean wasWearing = null;
    private static final List<Panel> PANEL_LIST = new ArrayList<>();

    private TurboIntro() {}

    /** Detecta el momento exacto en que te pones el casco (no se dispara al entrar al mundo ya con él puesto). */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { wasWearing = null; start = -1; return; }
        boolean now = mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof TurboArmorItem;
        if (wasWearing != null && !wasWearing && now) begin(mc);
        wasWearing = now;
    }

    private static void begin(Minecraft mc) {
        start = Util.getMillis();
        PANEL_LIST.clear();
        Random r = new Random();
        for (int i = 0; i < PANELS; i++) {
            PANEL_LIST.add(new Panel(i % MEME.length, r.nextFloat(), r.nextFloat(), 130 + r.nextInt(90),
                    250L + i * 120L + r.nextInt(60), 1700L + r.nextInt(700)));
        }
        mc.player.playSound(SoundEvents.BEACON_ACTIVATE, 0.6f, 1.7f);
    }

    public static boolean active() { return start >= 0 && Util.getMillis() - start < TOTAL; }

    public static void render(GuiGraphics g, int w, int h) {
        long t = Util.getMillis() - start;
        float noiseA = t < 400 ? t / 400f * 0.55f : (t > TOTAL - 900 ? Math.max(0f, (TOTAL - t) / 900f) * 0.55f : 0.55f);

        // estática azul + tinte (la textura se repite; el desplazamiento cambia para que "tiemble")
        RenderSystem.enableBlend();
        long step = t / 45;
        g.setColor(1f, 1f, 1f, noiseA);
        g.blit(STATIC, 0, 0, (step * 37) % 128, (step * 91) % 128, w, h, 128, 128);
        g.setColor(1f, 1f, 1f, 1f);
        g.fill(0, 0, w, h, (((int) (noiseA * 0.45f * 255)) << 24) | 0x0A3C8C);

        for (Panel p : PANEL_LIST) {
            long lt = t - p.spawn();
            if (lt < 0 || lt > p.life()) continue;
            float in = Math.min(1f, lt / 150f);                               // aparece
            float out = lt > p.life() - 200 ? Math.max(0f, (p.life() - lt) / 200f) : 1f;   // desaparece
            float a = Math.min(in, out);
            float sc = (0.7f + 0.3f * in) * (0.6f + 0.4f * out);
            int pw = Math.max(8, (int) (p.width() * sc));
            int ph = Math.max(8, (int) (pw * MEME_H[p.meme()] / 256f));
            int x = (int) (p.fx() * Math.max(1, w - pw)), y = (int) (p.fy() * Math.max(1, h - ph));
            drawPanel(g, x, y, pw, ph, a, p.meme());
        }
        RenderSystem.enableBlend();
        g.setColor(1f, 1f, 1f, 1f);
    }

    private static int col(float a, int rgb) { return (Math.max(0, Math.min(255, (int) (a * 255))) << 24) | (rgb & 0xFFFFFF); }

    /** Rectángulo con las esquinas cortadas (forma de octágono, como los marcos de la referencia). */
    private static void octagon(GuiGraphics g, int x0, int y0, int x1, int y1, int cut, int color) {
        g.fill(x0, y0 + cut, x1, y1 - cut, color);
        for (int k = 0; k < cut; k++) {
            int inset = cut - k;
            g.fill(x0 + inset, y0 + k, x1 - inset, y0 + k + 1, color);
            g.fill(x0 + inset, y1 - 1 - k, x1 - inset, y1 - k, color);
        }
    }

    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h, float a, int meme) {
        octagon(g, x - 8, y - 8, x + w + 8, y + h + 8, 9, col(a * 0.45f, 0x2AA8FF));    // resplandor
        octagon(g, x - 5, y - 5, x + w + 5, y + h + 5, 7, col(a, 0x28A0FF));            // borde brillante
        octagon(g, x - 3, y - 3, x + w + 3, y + h + 3, 5, col(a, 0x0A3C9C));            // borde interior
        g.setColor(1f, 1f, 1f, a);
        g.blit(MEME[meme], x, y, w, h, 0f, 0f, 256, MEME_H[meme], 256, MEME_H[meme]);
        g.setColor(1f, 1f, 1f, 1f);
        g.fill(x + 4, y + h + 5, x + 4 + Math.max(6, w / 6), y + h + 6, col(a, 0xFFFFFF));   // rayita de "título" bajo el marco
    }
}
