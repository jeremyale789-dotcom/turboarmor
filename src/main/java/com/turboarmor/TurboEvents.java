package com.turboarmor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Lado servidor del miniturbo: anula la caída, gasta/recarga la energía y muestra llamas azules. */
@Mod.EventBusSubscriber(modid = TurboArmor.ID)
public class TurboEvents {
    private static final int STEP = 4;   // la energía se actualiza cada 4 ticks (0.2 s)
    private static final Set<UUID> WAS_ACTIVE = ConcurrentHashMap.newKeySet();

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
        boolean ignition = false;
        if (active) ignition = WAS_ACTIVE.add(p.getUUID());
        else WAS_ACTIVE.remove(p.getUUID());
        if (active) {
            p.fallDistance = 0f;
            if (p.level() instanceof ServerLevel sl) jets(sl, p, ignition);
        }

        if (p.tickCount % STEP != 0) return;
        int energy = TurboEnergy.get(p);
        if (active) {
            energy -= TurboEnergy.DRAIN_PER_SECOND * STEP / 20;          // 0.3 por segundo
            TurboEnergy.set(p, energy);
            if (energy <= 0) TurboState.clear(p.getUUID());              // sin energía: el turbo se corta
        }
    }

    private static double rnd(RandomSource r, double a) { return (r.nextDouble() - 0.5) * 2.0 * a; }

    /** Envía una partícula con velocidad solo a los OTROS jugadores (el dueño ya la ve generada en su propio cliente). */
    private static void emit(ServerLevel sl, Player owner, net.minecraft.core.particles.ParticleOptions type,
                             double x, double y, double z, double vx, double vy, double vz) {
        for (ServerPlayer o : sl.players()) {
            if (o == owner || o.distanceToSqr(x, y, z) > 4096.0) continue;
            sl.sendParticles(o, type, false, x, y, z, 0, vx, vy, vz, 1.0);
        }
    }

    /** Chorro de la turbina de la espalda tal como lo ven los demás jugadores + sonidos para todos. */
    private static void jets(ServerLevel sl, Player p, boolean ignition) {
        double yaw = Math.toRadians(p.getYRot());
        double bx = Math.sin(yaw), bz = -Math.cos(yaw);
        double nx = p.getX() + bx * 0.55, ny = p.getY() + p.getBbHeight() * 0.62, nz = p.getZ() + bz * 0.55;
        RandomSource r = p.getRandom();

        for (int i = 0; i < 2; i++) {
            emit(sl, p, ParticleTypes.SOUL_FIRE_FLAME, nx, ny, nz, bx * 0.35 + rnd(r, 0.04), rnd(r, 0.03), bz * 0.35 + rnd(r, 0.04));
            emit(sl, p, ParticleTypes.FLAME, nx, ny, nz, bx * 0.22 + rnd(r, 0.06), rnd(r, 0.05), bz * 0.22 + rnd(r, 0.06));
        }
        emit(sl, p, ParticleTypes.END_ROD, nx, ny, nz, bx * 0.55, rnd(r, 0.02), bz * 0.55);
        if (p.tickCount % 2 == 0) emit(sl, p, ParticleTypes.CLOUD, nx, ny, nz, bx * 0.12 + rnd(r, 0.03), rnd(r, 0.02), bz * 0.12 + rnd(r, 0.03));
        if (p.tickCount % 3 == 0) emit(sl, p, ParticleTypes.ELECTRIC_SPARK, nx, ny, nz, rnd(r, 0.4), rnd(r, 0.4), rnd(r, 0.4));
        if (p.tickCount % 5 == 0) {
            sl.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.35f, 0.7f);
        }
        if (ignition) {
            for (int i = 0; i < 28; i++) {
                emit(sl, p, i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.CLOUD, nx, ny, nz,
                        bx * 0.6 + rnd(r, 0.25), rnd(r, 0.25), bz * 0.6 + rnd(r, 0.25));
            }
            sl.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.8f, 0.9f);
            sl.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f, 1.4f);
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
        WAS_ACTIVE.remove(e.getEntity().getUUID());
    }
}
