// From
// https://github.com/Argarian-Network/NachoSpigot/tree/async-kb-hit
package dev.stackspigot.async.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.server.Packet;

public final class Spigot404Write {

    private Spigot404Write() {
    }

    /**
     * Writes and flushes a packet on the given channel. Netty already queues the
     * write on the channel's own event loop when called from another thread, so
     * the packet only ever reaches the channel it was addressed to.
     */
    public static void writeThenFlush(Channel channel, Packet<?> value, GenericFutureListener<? extends Future<? super Void>>[] listener) {
        // The player might leave right before the packet is sent
        if (channel == null || !channel.isActive()) {
            return;
        }

        ChannelFuture future = channel.writeAndFlush(value);
        if (listener != null) {
            future.addListeners(listener);
        }
        future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}
