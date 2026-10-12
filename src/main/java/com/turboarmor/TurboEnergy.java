package com.turboarmor;

import com.turboarmor.net.TurboEnergyPacket;
import com.turboarmor.net.TurboNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.PacketDistributor;

/**
 * Energía del turbo, en milésimas (10000 = 10.0). El servidor es quien manda y la guarda con el jugador;
 * el cliente solo tiene una copia (para la barra y para saber si puede usar el turbo).
 */
public final class TurboEnergy {
    /** Almacenamiento máximo: 10.0 */
    public static final int MAX = 10_000;
    /** Consumo mientras el turbo está activo: 0.1 por segundo. */
    public static final int DRAIN_PER_SECOND = 100;
    /** Recarga en el suelo (con la pechera puesta): 0.25 por segundo. (Valor elegido por mí: cámbialo aquí.) */
    public static final int RECHARGE_PER_SECOND = 250;

    private static final String KEY = "turboarmor_energy";

    /** Copia en el cliente (la envía el servidor). */
    public static volatile int clientValue = MAX;

    private TurboEnergy() {}

    public static int get(ServerPlayer p) {
        CompoundTag t = p.getPersistentData();
        return t.contains(KEY) ? Mth.clamp(t.getInt(KEY), 0, MAX) : MAX;
    }

    public static void set(ServerPlayer p, int value) {
        int v = Mth.clamp(value, 0, MAX);
        if (p.getPersistentData().contains(KEY) && p.getPersistentData().getInt(KEY) == v) return;
        p.getPersistentData().putInt(KEY, v);
        sync(p, v);
    }

    public static void sync(ServerPlayer p) { sync(p, get(p)); }

    private static void sync(ServerPlayer p, int v) {
        TurboNet.CHANNEL.send(new TurboEnergyPacket(v), PacketDistributor.PLAYER.with(p));
    }
}
