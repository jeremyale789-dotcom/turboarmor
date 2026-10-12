package com.turboarmor.net;

import com.turboarmor.TurboEnergy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Paquete servidor -> cliente: energía actual del turbo (milésimas). */
public record TurboEnergyPacket(int energy) {
    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(energy); }

    public static TurboEnergyPacket decode(FriendlyByteBuf buf) { return new TurboEnergyPacket(buf.readVarInt()); }

    public static void handle(TurboEnergyPacket msg, CustomPayloadEvent.Context ctx) {
        TurboEnergy.clientValue = msg.energy();
        ctx.setPacketHandled(true);
    }
}
