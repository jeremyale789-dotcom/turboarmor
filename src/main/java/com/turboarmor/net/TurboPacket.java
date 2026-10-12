package com.turboarmor.net;

import com.turboarmor.TurboEnergy;
import com.turboarmor.TurboState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Paquete cliente -> servidor: "estoy usando el miniturbo". */
public record TurboPacket(boolean active) {
    public void encode(FriendlyByteBuf buf) { buf.writeBoolean(active); }

    public static TurboPacket decode(FriendlyByteBuf buf) { return new TurboPacket(buf.readBoolean()); }

    public static void handle(TurboPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer p = ctx.getSender();
        if (p != null && msg.active() && TurboState.wearsChestplate(p) && TurboEnergy.get(p) > 0) TurboState.mark(p);
        ctx.setPacketHandled(true);
    }
}
