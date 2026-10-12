package com.turboarmor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Lado servidor del miniturbo: anula la caída, gasta/recarga la energía y muestra llamas azules. */
@Mod.EventBusSubscriber(modid = TurboArmor.ID)
public class TurboEvents {
    private static final int STEP = 4;   // la energía se actualiza cada 4 ticks (0.2 s)

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
        if (!(e.player instanceof ServerPlayer p)) return;
        if (!TurboState.wearsChestplate(p)) return;

        boolean active = TurboState.isActive(p);
        if (active) {
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

        if (p.tickCount % STEP != 0) return;
        int energy = TurboEnergy.get(p);
        if (active) {
            energy -= TurboEnergy.DRAIN_PER_SECOND * STEP / 20;          // 0.1 por segundo
            TurboEnergy.set(p, energy);
            if (energy <= 0) TurboState.clear(p.getUUID());              // sin energía: el turbo se corta
        } else if (p.onGround() && energy < TurboEnergy.MAX) {
            TurboEnergy.set(p, energy + TurboEnergy.RECHARGE_PER_SECOND * STEP / 20);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) TurboEnergy.sync(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) TurboEnergy.sync(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        TurboState.clear(e.getEntity().getUUID());
    }
}
