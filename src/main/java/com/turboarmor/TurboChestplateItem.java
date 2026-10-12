package com.turboarmor;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Pechera con la habilidad del miniturbo (la lógica está en TurboClient / TurboEvents). */
public class TurboChestplateItem extends TurboArmorItem {
    public TurboChestplateItem(Holder<ArmorMaterial> material, Properties props) {
        super(material, ArmorItem.Type.CHESTPLATE, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.turboarmor.turbo").withStyle(net.minecraft.ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.turboarmor.turbo2").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.turboarmor.energy").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
