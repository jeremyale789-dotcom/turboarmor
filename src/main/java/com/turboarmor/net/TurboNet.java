package com.turboarmor.net;

import com.turboarmor.TurboArmor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

/** Canal de red propio (turboarmor:main): no choca con el de ningún otro mod. */
public final class TurboNet {
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(TurboArmor.ID, "main"))
            .networkProtocolVersion(1)
            .optional()
            .simpleChannel();

    private TurboNet() {}

    public static void init() {
        CHANNEL.messageBuilder(TurboPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TurboPacket::encode)
                .decoder(TurboPacket::decode)
                .consumerMainThread(TurboPacket::handle)
                .add();
        CHANNEL.messageBuilder(TurboEnergyPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TurboEnergyPacket::encode)
                .decoder(TurboEnergyPacket::decode)
                .consumerMainThread(TurboEnergyPacket::handle)
                .add();
    }
}
