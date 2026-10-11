package com.turboarmor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Lado servidor del miniturbo: anula el daño de caída mientras está activo y muestra llamas azules. */
@Mod.EventBusSubscriber(modid = TurboArmor.ID)
public class TurboEvents {

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (e.getEntity() instanceof Player p && TurboState.isActive(p)) {
            e.setDistance(0f);
            e.setCanceled(true);          // caída de pluma: sin daño
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide) return;
        Player p = e.player;
        if (!TurboState.isActive(p)) return;
        p.fallDistance = 0f;
        if (p.level() instanceof ServerLevel sl) {
            if (p.tickCount % 2 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 0.05, p.getZ(), 3, 0.15, 0.0, 0.15, 0.02);
            }
            if (p.tickCount % 6 == 0) {
                sl.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.3f, 1.6f);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        TurboState.clear(e.getEntity().getUUID());
    }
}
