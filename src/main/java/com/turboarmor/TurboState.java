package com.turboarmor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Estado (en el servidor) de los jugadores que tienen el miniturbo activo. El cliente lo refresca cada pocos ticks. */
public final class TurboState {
    private static final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();
    /** Margen (ticks) que el servidor sigue considerando activo el turbo tras la última señal del cliente. */
    private static final int GRACE_TICKS = 8;

    private TurboState() {}

    public static boolean wearsChestplate(Player p) {
        return p.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof TurboChestplateItem;
    }

    public static void mark(ServerPlayer p) {
        ACTIVE_UNTIL.put(p.getUUID(), p.level().getGameTime() + GRACE_TICKS);
    }

    public static boolean isActive(Player p) {
        Long until = ACTIVE_UNTIL.get(p.getUUID());
        if (until == null) return false;
        if (until < p.level().getGameTime()) { ACTIVE_UNTIL.remove(p.getUUID()); return false; }
        return wearsChestplate(p);
    }

    public static void clear(UUID id) { ACTIVE_UNTIL.remove(id); }
}
