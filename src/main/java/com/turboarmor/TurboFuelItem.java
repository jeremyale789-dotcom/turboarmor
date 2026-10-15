package com.turboarmor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Combustible Turbo: es la ÚNICA forma de recargar la energía de la pechera.
 * Mantén clic derecho (con la pechera Turbo puesta) para recargar +5.0 de energía; la cápsula vacía se devuelve.
 */
public class TurboFuelItem extends Item {
    public TurboFuelItem(Properties props) { super(props); }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!TurboState.wearsChestplate(player)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("msg.turboarmor.need_chest"), true);
            return InteractionResultHolder.fail(stack);
        }
        int energy = level.isClientSide ? TurboEnergy.clientValue
                : (player instanceof ServerPlayer sp ? TurboEnergy.get(sp) : TurboEnergy.MAX);
        if (energy >= TurboEnergy.MAX) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("msg.turboarmor.full"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp && TurboState.wearsChestplate(sp)) {
            TurboEnergy.set(sp, TurboEnergy.get(sp) + TurboEnergy.FUEL_ENERGY);
            level.playSound(null, sp.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.5f);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, sp.getX(), sp.getY() + 1.0, sp.getZ(), 24, 0.4, 0.6, 0.4, 0.3);
            }
            if (!sp.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack capsule = new ItemStack(TurboArmor.CAPSULE.get());   // se devuelve la cápsula vacía
                if (stack.isEmpty()) return capsule;
                if (!sp.getInventory().add(capsule)) sp.drop(capsule, false);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.turboarmor.fuel").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.turboarmor.fuel2").withStyle(ChatFormatting.GRAY));
    }
}
