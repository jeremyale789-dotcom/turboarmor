package com.turboarmor.client;

import com.turboarmor.TurboArmorItem;
import com.turboarmor.TurboChestplateItem;
import com.turboarmor.TurboEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Interfaz de la armadura:
 *  - Solo aparece si llevas el CASCO Turbo.
 *  - Muestra la durabilidad que le queda a cada pieza Turbo que llevas puesta.
 *  - La barra del turbo (máx. 10, consumo 0.1/s) solo aparece si además llevas la PECHERA Turbo.
 */
public class TurboHud implements IGuiOverlay {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int X = 6, Y = 6, W = 150;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;
        if (!(p.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof TurboArmorItem)) return;   // sin casco: sin interfaz

        Font font = mc.font;

        int rows = 0;
        for (EquipmentSlot s : SLOTS) if (p.getItemBySlot(s).getItem() instanceof TurboArmorItem) rows++;
        boolean hasChest = p.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof TurboChestplateItem;
        int h = 16 + rows * 20 + (hasChest ? 28 : 0) + 4;

        // panel
        g.fill(X - 1, Y - 1, X + W + 1, Y + h + 1, 0xFF05102A);
        g.fillGradient(X, Y, X + W, Y + h, 0xD8142A55, 0xD806102A);
        g.fill(X, Y, X + W, Y + 1, 0xFF4FB4FF);                    // línea superior
        g.fill(X, Y, X + 2, Y + h, 0xFFFFD23C);                    // acento amarillo
        g.fill(X + W - 14, Y + 3, X + W - 4, Y + 5, 0xFFFF7A18);   // detalle naranja (como los ojos)
        g.drawString(font, Component.translatable("hud.turboarmor.title"), X + 8, Y + 4, 0xFF8FE9FF, true);

        // durabilidad de cada pieza
        int y = Y + 16;
        for (EquipmentSlot s : SLOTS) {
            ItemStack st = p.getItemBySlot(s);
            if (!(st.getItem() instanceof TurboArmorItem)) continue;
            int max = Math.max(1, st.getMaxDamage());
            int left = max - st.getDamageValue();
            float ratio = Math.max(0f, Math.min(1f, left / (float) max));
            g.renderItem(st, X + 6, y);
            int bx = X + 28, bw = 62;
            g.fill(bx - 1, y + 4, bx + bw + 1, y + 12, 0xFF000010);
            g.fill(bx, y + 5, bx + bw, y + 11, 0xFF1A2038);
            int color = ratio > 0.5f ? 0xFF4CE05A : (ratio > 0.25f ? 0xFFFFC83C : 0xFFFF4040);
            g.fill(bx, y + 5, bx + Math.round(bw * ratio), y + 11, color);
            g.fill(bx, y + 5, bx + Math.round(bw * ratio), y + 7, 0x55FFFFFF);       // brillo
            g.drawString(font, left + "/" + max, bx + bw + 5, y + 4, 0xFFDDE8FF, true);
            y += 20;
        }

        // barra del turbo (solo con la pechera)
        if (hasChest) {
            float energy = TurboEnergy.clientValue / 1000f;
            boolean low = energy <= 2.5f;
            boolean active = TurboClient.active;
            g.drawString(font, Component.translatable("hud.turboarmor.turbo"), X + 8, y + 2, active ? 0xFFFFE066 : 0xFF8FE9FF, true);
            if (active) g.drawString(font, Component.translatable("hud.turboarmor.active"), X + 52, y + 2, 0xFFFF9A2E, true);
            String txt = String.format("%.1f/10", energy);
            g.drawString(font, txt, X + W - 6 - font.width(txt), y + 2, low ? 0xFFFF6A5A : 0xFFDDE8FF, true);

            int sx = X + 8, sy = y + 13, sw = 11, gap = 1;                           // 10 segmentos = 10 unidades
            for (int i = 0; i < 10; i++) {
                int x0 = sx + i * (sw + gap);
                g.fill(x0, sy, x0 + sw, sy + 7, 0xFF0C1630);
                float f = Math.max(0f, Math.min(1f, energy - i));
                if (f > 0f) {
                    int top = low ? 0xFFFF8A4A : 0xFF6FF0FF, bottom = low ? 0xFFD03A1A : 0xFF1FA8E8;
                    g.fillGradient(x0, sy, x0 + Math.max(1, Math.round(sw * f)), sy + 7, top, bottom);
                }
            }
        }
    }
}
