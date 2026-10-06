// From
// https://github.com/Argarian-Network/NachoSpigot/tree/async-kb-hit
package dev.stackspigot.async.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.server.Packet;

//StackSpigot-Code
public final class Spigot404Write {

    private Spigot404Write() {
    }

    public static void writeThenFlush(Channel channel, Packet<?> value, GenericFutureListener<? extends Future<? super Void>>[] listener) {
        if (channel == null || !channel.isActive()) {
            return;
        }

        if (listener == null) {
            channel.writeAndFlush(value, channel.voidPromise());
            return;
        }

        ChannelFuture future = channel.writeAndFlush(value);
        future.addListeners(listener);
        future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}
//End-of-StackSpigot-Code
