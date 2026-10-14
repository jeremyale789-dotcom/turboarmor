package com.turboarmor.client;

import com.turboarmor.TurboArmor;
import com.turboarmor.TurboArmorItem;
import com.turboarmor.TurboChestplateItem;
import com.turboarmor.TurboEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Interfaz de la armadura (según el boceto):
 *  - Arriba a la izquierda: icono de cada pieza Turbo puesta + su barra de durabilidad.
 *  - A la derecha: barra vertical alta del turbo (máx. 10, consumo 0.1/s) con un círculo debajo que marca el valor.
 * Solo aparece con el CASCO puesto; la barra del turbo solo con la PECHERA además.
 */
@Mod.EventBusSubscriber(modid = TurboArmor.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TurboHud {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Se dibuja al final de cada fotograma, encima del juego. */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        boolean helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof TurboArmorItem;
        boolean hud = helmet && mc.screen == null && !mc.options.hideGui;
        boolean intro = TurboIntro.active();
        if (!hud && !intro) return;

        GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        g.pose().translate(0.0f, 0.0f, -2000.0f);     // el plano de recorte de la interfaz empieza en z = -1000
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        if (hud) draw(g, mc, w, h);
        if (intro) TurboIntro.render(g, w, h);
        g.flush();
    }

    // ---------------------------------------------------------------- utilidades de dibujo
    private static int argb(int a, int rgb) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }

    /** Marco azul con brillo, como los paneles de la referencia. */
    static void frame(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0x662AA8FF);   // resplandor
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF28A0FF);   // borde
        g.fill(x0, y0, x1, y1, 0xFF061A3A);                   // fondo
    }

    static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private static void smallText(GuiGraphics g, Font font, String s, float x, float y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, s, 0, 0, color, true);
        g.pose().popPose();
    }

    // ---------------------------------------------------------------- interfaz
    private static void draw(GuiGraphics g, Minecraft mc, int w, int h) {
        var p = mc.player;
        Font font = mc.font;

        // --- izquierda: icono + barra de durabilidad de cada pieza puesta
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack st = p.getItemBySlot(SLOTS[i]);
            if (!(st.getItem() instanceof TurboArmorItem)) continue;
            int x = 8, y = 8 + i * 22;
            int max = Math.max(1, st.getMaxDamage());
            int left = max - st.getDamageValue();
            float ratio = Math.max(0f, Math.min(1f, left / (float) max));

            g.renderItem(st, x, y);
            int bx = x + 22, by = y + 4, bw = 72, bh = 8;
            frame(g, bx, by, bx + bw, by + bh);
            int fw = Math.round((bw - 2) * ratio);
            int c1 = ratio > 0.5f ? 0xFF6FF0FF : (ratio > 0.25f ? 0xFFFFD25A : 0xFFFF6A5A);
            int c2 = ratio > 0.5f ? 0xFF1FA8E8 : (ratio > 0.25f ? 0xFFD08A10 : 0xFFC02A1A);
            if (fw > 0) g.fillGradient(bx + 1, by + 1, bx + 1 + fw, by + bh - 1, c1, c2);
            smallText(g, font, Integer.toString(left), bx + bw + 5, by + 0.5f, 0.75f, 0xFFCFE8FF);
        }

        // --- derecha: barra vertical del turbo (solo con la pechera)
        if (!(p.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof TurboChestplateItem)) return;
        float energy = TurboEnergy.clientValue / 1000f;
        boolean low = energy <= 2.5f, active = TurboClient.active;

        int bw = 12;
        int x0 = w - 24, x1 = x0 + bw;
        int top = Math.round(h * 0.18f), bottom = Math.round(h * 0.74f);
        frame(g, x0, top, x1, bottom);

        int innerH = bottom - top - 2;
        int fh = Math.round(innerH * Math.max(0f, Math.min(1f, energy / 10f)));
        int c1 = low ? 0xFFFF9A5A : (active ? 0xFFD8FFFF : 0xFF6FF0FF);
        int c2 = low ? 0xFFD03A1A : (active ? 0xFF4FD0FF : 0xFF1FA8E8);
        if (fh > 0) g.fillGradient(x0 + 1, bottom - 1 - fh, x1 - 1, bottom - 1, c1, c2);
        for (int i = 1; i < 10; i++) {                                    // marcas: 1 por unidad de energía
            int ty = bottom - 1 - Math.round(innerH * i / 10f);
            g.fill(x0 + 1, ty, x1 - 1, ty + 1, 0xAA000820);
        }
        if (active) g.fill(x0 - 3, top - 3, x1 + 3, top - 2, 0xFFFFE066);  // destello superior mientras el turbo está activo

        // círculo debajo con el valor (como en el boceto)
        int cx = x0 + bw / 2, cy = bottom + 18, r = 12;
        disc(g, cx, cy, r + 2, 0x662AA8FF);
        disc(g, cx, cy, r + 1, 0xFF28A0FF);
        disc(g, cx, cy, r, 0xFF061A3A);
        String txt = String.format("%.1f", energy);
        smallText(g, font, txt, cx - font.width(txt) * 0.75f / 2f, cy - 3.5f, 0.75f, low ? 0xFFFF8A7A : 0xFFDDF6FF);
        g.fill(x0 - 8, cy - 1, x0 - 5, cy + 1, 0xFF28A0FF);               // rayita a la izquierda del círculo
    }
}
