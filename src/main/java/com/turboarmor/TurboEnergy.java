package com.turboarmor;

import com.turboarmor.net.TurboEnergyPacket;
import com.turboarmor.net.TurboNet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.network.PacketDistributor;

/**
 * Energía del turbo, en milésimas (10000 = 10.0). El servidor es quien manda y la guarda con el jugador;
 * el cliente solo tiene una copia (para la barra y para saber si puede usar el turbo).
 */
public final class TurboEnergy {
    /** Almacenamiento máximo: 10.0 */
    public static final int MAX = 10_000;
    /** Consumo mientras el turbo está activo: 0.3 por segundo. */
    public static final int DRAIN_PER_SECOND = 300;
    /** Energía que da un Combustible Turbo: 5.0. (La ÚNICA forma de recargar es el combustible.) */
    public static final int FUEL_ENERGY = 5_000;

    private static final String KEY = "turboarmor_energy";
    private static final String PENDING_KEY = "turbo_pending_charge";

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

    /** Carga que la pechera trae de la mesa de crafteo; se pasa a la energía del jugador al ponérsela. */
    public static int getPending(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null || !cd.contains(PENDING_KEY)) return 0;
        return Mth.clamp(cd.copyTag().getInt(PENDING_KEY), 0, MAX);
    }

    public static void setPending(ItemStack stack, int value) {
        final int v = Mth.clamp(value, 0, MAX);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (v <= 0) tag.remove(PENDING_KEY); else tag.putInt(PENDING_KEY, v);
        });
    }
}
