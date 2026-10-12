package com.turboarmor.client;

import com.turboarmor.TurboArmor;
import com.turboarmor.TurboEnergy;
import com.turboarmor.TurboState;
import com.turboarmor.net.TurboNet;
import com.turboarmor.net.TurboPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
    private static final double THRUST = 0.045;           // empuje horizontal por tick
    private static final double MAX_HORIZONTAL = 0.55;    // velocidad horizontal máxima
    private static int sendCooldown;
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
        if (!on) { sendCooldown = 0; return; }

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

        if (sendCooldown-- <= 0) {
            TurboNet.CHANNEL.send(new TurboPacket(true), PacketDistributor.SERVER.noArg());
            sendCooldown = 3;
        }
    }
}
