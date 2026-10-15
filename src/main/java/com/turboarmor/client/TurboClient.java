package com.turboarmor.client;

import com.turboarmor.TurboArmor;
import com.turboarmor.TurboEnergy;
import com.turboarmor.TurboState;
import com.turboarmor.net.TurboNet;
import com.turboarmor.net.TurboPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Miniturbo (lado cliente): con la pechera puesta, EN EL AIRE, agáchate y mantén el salto:
 *  - la caída se frena (planeo lento) y recibes un pequeño empuje hacia donde miras,
 *  - avisa al servidor para que anule el daño de caída (caída de pluma) mientras dure.
 */
@Mod.EventBusSubscriber(modid = TurboArmor.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TurboClient {
    private static final double MAX_FALL_SPEED = -0.12;   // velocidad máxima de caída con el turbo
    private static final double THRUST = 0.10;            // empuje horizontal por tick (el doble que antes)
    private static final double MAX_HORIZONTAL = 1.15;    // velocidad horizontal máxima (el doble que antes)
    private static int sendCooldown;
    private static boolean wasOn;
    /** Para el HUD: ¿el turbo está activo ahora mismo? */
    public static volatile boolean active;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) { active = false; return; }
        if (mc.isPaused() || mc.screen != null) return;

        boolean on = TurboState.wearsChestplate(p) && TurboEnergy.clientValue > 0
                && mc.options.keyJump.isDown() && mc.options.keyShift.isDown()
                && !p.onGround() && !p.isInWater() && !p.isInLava() && !p.onClimbable()
                && !p.isFallFlying() && !p.getAbilities().flying && !p.isPassenger();
        active = on;
        if (!on) { sendCooldown = 0; wasOn = false; return; }

        Vec3 v = p.getDeltaMovement();
        double vy = Math.max(v.y, MAX_FALL_SPEED);

        Vec3 look = p.getLookAngle();
        double lh = Math.sqrt(look.x * look.x + look.z * look.z);
        double hx = v.x, hz = v.z;
        if (lh > 1.0E-4) { hx += look.x / lh * THRUST; hz += look.z / lh * THRUST; }
        double hs = Math.sqrt(hx * hx + hz * hz);
        if (hs > MAX_HORIZONTAL) { hx *= MAX_HORIZONTAL / hs; hz *= MAX_HORIZONTAL / hs; }

        p.setDeltaMovement(hx, vy, hz);
        p.fallDistance = 0f;

        jets(mc, p, !wasOn);
        wasOn = true;

        if (sendCooldown-- <= 0) {
            TurboNet.CHANNEL.send(new TurboPacket(true), PacketDistributor.SERVER.noArg());
            sendCooldown = 3;
        }
    }

    private static double rnd(RandomSource r, double a) { return (r.nextDouble() - 0.5) * 2.0 * a; }

    /**
     * Chorro de la turbina de la espalda. Se genera AQUÍ (en el cliente, con la posición real del jugador) y las partículas
     * heredan el movimiento del jugador, así salen siempre pegadas a la tobera y no se quedan atrás ni caen bajo el jugador.
     */
    private static void jets(Minecraft mc, LocalPlayer p, boolean ignition) {
        ClientLevel lv = mc.level;
        if (lv == null) return;
        RandomSource r = p.getRandom();
        double yaw = Math.toRadians(p.getYRot());
        double bx = Math.sin(yaw), bz = -Math.cos(yaw);                          // hacia atrás
        double nx = p.getX() + bx * 0.55, ny = p.getY() + p.getBbHeight() * 0.62, nz = p.getZ() + bz * 0.55;
        Vec3 v = p.getDeltaMovement();

        for (int i = 0; i < 2; i++) {                                            // núcleo azul
            lv.addParticle(ParticleTypes.SOUL_FIRE_FLAME, nx, ny, nz,
                    v.x + bx * 0.35 + rnd(r, 0.04), v.y + rnd(r, 0.03), v.z + bz * 0.35 + rnd(r, 0.04));
        }
        for (int i = 0; i < 2; i++) {                                            // llama exterior
            lv.addParticle(ParticleTypes.FLAME, nx, ny, nz,
                    v.x + bx * 0.22 + rnd(r, 0.06), v.y + rnd(r, 0.04), v.z + bz * 0.22 + rnd(r, 0.06));
        }
        lv.addParticle(ParticleTypes.END_ROD, nx, ny, nz, v.x + bx * 0.5, v.y + rnd(r, 0.02), v.z + bz * 0.5);   // destello
        if (p.tickCount % 2 == 0) {                                              // estela de humo (se queda atrás, flotando)
            lv.addParticle(ParticleTypes.CLOUD, nx, ny, nz, v.x * 0.15 + bx * 0.1, rnd(r, 0.02), v.z * 0.15 + bz * 0.1);
        }
        if (p.tickCount % 3 == 0) {                                              // chispas
            lv.addParticle(ParticleTypes.ELECTRIC_SPARK, nx, ny, nz, v.x * 0.5 + rnd(r, 0.3), v.y + rnd(r, 0.3), v.z * 0.5 + rnd(r, 0.3));
        }
        if (ignition) {                                                          // ráfaga de ignición
            for (int i = 0; i < 28; i++) {
                lv.addParticle(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.CLOUD, nx, ny, nz,
                        v.x * 0.3 + bx * 0.6 + rnd(r, 0.25), v.y * 0.3 + rnd(r, 0.25), v.z * 0.3 + bz * 0.6 + rnd(r, 0.25));
            }
        }
    }
}
