package com.turboarmor;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Recarga en la mesa de crafteo: Pechera Turbo + Combustible Turbo (en cualquier casilla) = la misma pechera con carga.
 * El combustible (y por tanto el frasco vacío) se consume y se pierde. El de hierro da +5.0; el de diamante recarga por completo.
 */
public class TurboRechargeRecipe extends CustomRecipe {
    public TurboRechargeRecipe(CraftingBookCategory category) { super(category); }

    private static boolean isFuel(ItemStack s) {
        return s.is(TurboArmor.FUEL.get()) || s.is(TurboArmor.FUEL_DIAMOND.get());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int chest = 0, fuel = 0, other = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof TurboChestplateItem) chest++;
            else if (isFuel(s)) fuel++;
            else other++;
        }
        return chest == 1 && fuel == 1 && other == 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack chest = ItemStack.EMPTY, fuel = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.getItem() instanceof TurboChestplateItem) chest = s;
            else if (isFuel(s)) fuel = s;
        }
        ItemStack out = chest.copy();
        out.setCount(1);
        int add = fuel.is(TurboArmor.FUEL_DIAMOND.get()) ? TurboEnergy.MAX : TurboEnergy.FUEL_ENERGY;
        TurboEnergy.setPending(out, TurboEnergy.getPending(out) + add);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public RecipeSerializer<?> getSerializer() { return TurboArmor.RECHARGE_SERIALIZER.get(); }
}
