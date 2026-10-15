package com.turboarmor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.turboarmor.TurboArmor;
import com.turboarmor.TurboArmorItem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
 * Secuencia al ponerte el casco:
 *   1) ARRANQUE (1.3 s): la pantalla parpadea como un visor que se enciende, una línea de escaneo la recorre, aparecen las
 *      esquinas del visor y el texto "TURBO SYSTEM ONLINE"; termina con un destello.
 *   2) FILTRO AZUL permanente (mientras lleves el casco) + estática azul y paneles con los memes (5.2 s).
 * Con CTRL + la tecla de TurboKeys se activa/desactiva todo (filtro, arranque, memes y panel de estado). Funciona con el
 * casco puesto o con el casco en la mano secundaria (así se puede preparar antes de ponérselo).
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

    private static final long POWER_MS = 1300;                     // arranque del casco
    private static final long MEME_MS = 5200;                      // estática + memes
    private static final long TOTAL = POWER_MS + MEME_MS;
    private static final int PANELS = 22;

    private record Panel(int meme, float fx, float fy, int width, long spawn, long life) {}

    private static long start = -1;
    private static boolean flashSound;
    private static Boolean wasWearing = null;
    private static final List<Panel> PANEL_LIST = new ArrayList<>();

    private TurboIntro() {}

    private static boolean isHelmet(net.minecraft.world.item.ItemStack s) {
        return s.getItem() instanceof TurboArmorItem a && a.getEquipmentSlot() == EquipmentSlot.HEAD;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { wasWearing = null; start = -1; return; }

        boolean worn = mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof TurboArmorItem;
        boolean offhand = isHelmet(mc.player.getItemBySlot(EquipmentSlot.OFFHAND));

        // CTRL + tecla: activar/desactivar la interfaz (con el casco puesto o en la mano secundaria)
        while (TurboKeys.TOGGLE.consumeClick()) {
            if (Screen.hasControlDown() && (worn || offhand)) {
                boolean on = TurboConfig.toggle();
                if (!on) start = -1;
                mc.player.displayClientMessage(Component.translatable(on ? "hud.turboarmor.ui_on" : "hud.turboarmor.ui_off"), true);
                mc.player.playSound(on ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, 0.5f, 1.5f);
                if (on && worn) begin(mc);          // al volver a activarla con el casco puesto: arranque + filtro + memes otra vez
            }
        }

        // al ponerte el casco empieza la secuencia (no al entrar al mundo ya con él puesto)
        if (wasWearing != null && !wasWearing && worn) begin(mc);
        wasWearing = worn;

        // sonido del destello al terminar el arranque
        if (start >= 0 && !flashSound && Util.getMillis() - start >= POWER_MS - 150) {
            flashSound = true;
            mc.player.playSound(SoundEvents.BEACON_ACTIVATE, 0.6f, 1.7f);
        }
    }

    private static void begin(Minecraft mc) {
        if (!TurboConfig.uiEnabled) return;
        start = Util.getMillis();
        flashSound = false;
        PANEL_LIST.clear();
        Random r = new Random();
        for (int i = 0; i < PANELS; i++) {
            PANEL_LIST.add(new Panel(i % MEME.length, r.nextFloat(), r.nextFloat(), 130 + r.nextInt(90),
                    250L + i * 120L + r.nextInt(60), 1700L + r.nextInt(700)));
        }
        mc.player.playSound(SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.4f);
    }

    public static boolean active() { return start >= 0 && Util.getMillis() - start < TOTAL; }

    /** 0 mientras arranca el casco; luego aparece el filtro azul poco a poco. Sin secuencia en curso: 1. */
    private static float filterAlpha() {
        if (start < 0) return 1f;
        long t = Util.getMillis() - start;
        if (t < POWER_MS) return 0f;
        return Math.min(1f, (t - POWER_MS) / 400f);
    }

    // ------------------------------------------------------------------ filtro azul permanente
    public static void drawFilter(GuiGraphics g, int w, int h) {
        float a = filterAlpha();
        if (a <= 0f) return;
        g.fill(0, 0, w, h, (((int) (a * 0x30)) << 24) | 0x0A50B0);                  // tinte azul
        int vb = (int) (a * 0x60), band = h / 7, side = w / 9;
        g.fillGradient(0, 0, w, band, (vb << 24) | 0x0A3C8C, 0x000A3C8C);            // viñeta arriba
        g.fillGradient(0, h - band, w, h, 0x000A3C8C, (vb << 24) | 0x0A3C8C);        // viñeta abajo
        for (int i = 0; i < side; i += 2) {                                          // viñeta a los lados
            int al = (int) (vb * (1f - i / (float) side));
            int c = (al << 24) | 0x0A3C8C;
            g.fill(i, 0, i + 2, h, c);
            g.fill(w - i - 2, 0, w - i, h, c);
        }
    }

    // ------------------------------------------------------------------ secuencia
    public static void render(GuiGraphics g, int w, int h) {
        long t = Util.getMillis() - start;
        if (t < POWER_MS) { drawPowerOn(g, w, h, t); return; }
        drawMemes(g, w, h, t - POWER_MS);
    }

    private static int col(float a, int rgb) { return (Math.max(0, Math.min(255, (int) (a * 255))) << 24) | (rgb & 0xFFFFFF); }

    private static void drawPowerOn(GuiGraphics g, int w, int h, long t) {
        Font font = Minecraft.getInstance().font;

        // 1) parpadeos de pantalla encendiéndose
        int blackA = t < 350 ? ((t / 70) % 2 == 0 ? 170 : 80) : (int) (80 * Math.max(0f, 1f - (t - 350) / 300f));
        g.fill(0, 0, w, h, blackA << 24);

        // 2) línea de escaneo que recorre la pantalla de arriba hacia abajo
        if (t >= 350 && t < 1000) {
            int y = (int) ((t - 350) / 650f * h);
            g.fill(0, 0, w, y, 0x220A50B0);
            g.fillGradient(0, Math.max(0, y - 18), w, y, 0x000A9CFF, 0x889FE8FF);
            g.fill(0, y, w, y + 2, 0xFFB8F2FF);
        } else if (t >= 1000) {
            g.fill(0, 0, w, h, 0x220A50B0);
        }

        // 3) esquinas del visor
        float ba = Math.min(1f, t / 400f);
        int m = 14, len = 30, c = col(ba, 0x28A0FF);
        g.fill(m, m, m + len, m + 2, c); g.fill(m, m, m + 2, m + len, c);
        g.fill(w - m - len, m, w - m, m + 2, c); g.fill(w - m - 2, m, w - m, m + len, c);
        g.fill(m, h - m - 2, m + len, h - m, c); g.fill(m, h - m - len, m + 2, h - m, c);
        g.fill(w - m - len, h - m - 2, w - m, h - m, c); g.fill(w - m - 2, h - m - len, w - m, h - m, c);

        // 4) texto que se escribe solo
        if (t >= 650) {
            String l1 = "TURBO SYSTEM", l2 = t >= 1000 ? "ONLINE" : "";
            int n = (int) Math.min(l1.length(), (t - 650) / 28);
            String shown = l1.substring(0, n) + (((t / 120) % 2 == 0 && l2.isEmpty()) ? "_" : "");
            drawCentered(g, font, shown, w / 2f, h * 0.30f, 1.7f, 0xFF9FE8FF);
            if (!l2.isEmpty()) drawCentered(g, font, l2, w / 2f, h * 0.30f + 18f, 1.2f, 0xFF4CE05A);
        }

        // 5) destello final
        if (t >= 1150) {
            float fa = Math.max(0f, 1f - (t - 1150) / 150f) * 0.6f;
            g.fill(0, 0, w, h, col(fa, 0xBFEFFF));
        }
    }

    private static void drawCentered(GuiGraphics g, Font font, String s, float cx, float y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(cx - font.width(s) * scale / 2f, y, 0f);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, s, 0, 0, color, true);
        g.pose().popPose();
    }

    private static void drawMemes(GuiGraphics g, int w, int h, long t) {
        float noiseA = t < 400 ? t / 400f * 0.55f : (t > MEME_MS - 900 ? Math.max(0f, (MEME_MS - t) / 900f) * 0.55f : 0.55f);

        RenderSystem.enableBlend();
        long step = t / 45;
        g.setColor(1f, 1f, 1f, noiseA);
        g.blit(STATIC, 0, 0, (step * 37) % 128, (step * 91) % 128, w, h, 128, 128);
        g.setColor(1f, 1f, 1f, 1f);
        g.fill(0, 0, w, h, (((int) (noiseA * 0.45f * 255)) << 24) | 0x0A3C8C);

        for (Panel p : PANEL_LIST) {
            long lt = t - p.spawn();
            if (lt < 0 || lt > p.life()) continue;
            float in = Math.min(1f, lt / 150f);
            float out = lt > p.life() - 200 ? Math.max(0f, (p.life() - lt) / 200f) : 1f;
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

    /** Rectángulo con las esquinas cortadas (como los marcos de la referencia). */
    private static void octagon(GuiGraphics g, int x0, int y0, int x1, int y1, int cut, int color) {
        g.fill(x0, y0 + cut, x1, y1 - cut, color);
        for (int k = 0; k < cut; k++) {
            int inset = cut - k;
            g.fill(x0 + inset, y0 + k, x1 - inset, y0 + k + 1, color);
            g.fill(x0 + inset, y1 - 1 - k, x1 - inset, y1 - k, color);
        }
    }

    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h, float a, int meme) {
        octagon(g, x - 8, y - 8, x + w + 8, y + h + 8, 9, col(a * 0.45f, 0x2AA8FF));
        octagon(g, x - 5, y - 5, x + w + 5, y + h + 5, 7, col(a, 0x28A0FF));
        octagon(g, x - 3, y - 3, x + w + 3, y + h + 3, 5, col(a, 0x0A3C9C));
        g.setColor(1f, 1f, 1f, a);
        g.blit(MEME[meme], x, y, w, h, 0f, 0f, 256, MEME_H[meme], 256, MEME_H[meme]);
        g.setColor(1f, 1f, 1f, 1f);
        g.fill(x + 4, y + h + 5, x + 4 + Math.max(6, w / 6), y + h + 6, col(a, 0xFFFFFF));
    }
}
