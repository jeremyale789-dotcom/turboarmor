package com.turboarmor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Combustible Turbo (hierro: +5.0 / diamante: recarga completa). Se usa en la mesa de crafteo junto a la pechera. */
public class TurboFuelItem extends Item {
    private final boolean full;

    public TurboFuelItem(Properties props, boolean full) { super(props); this.full = full; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.add(Component.translatable(full ? "tooltip.turboarmor.fuel_full" : "tooltip.turboarmor.fuel").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.turboarmor.fuel2").withStyle(ChatFormatting.GRAY));
    }
}
