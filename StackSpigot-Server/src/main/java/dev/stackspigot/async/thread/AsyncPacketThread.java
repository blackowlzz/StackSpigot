// From
// https://github.com/Argarian-Network/NachoSpigot/tree/async-kb-hit
package dev.stackspigot.async.thread;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

import dev.stackspigot.async.netty.Spigot404Write;
import dev.stackspigot.config.StackSpigotConfig;

import net.minecraft.server.NetworkManager;
import net.minecraft.server.Packet;

public abstract class AsyncPacketThread {
    private volatile boolean running = true;
	private static final long SEC_IN_NANO = 1000000000;
	private static final int TPS = Math.max(1, StackSpigotConfig.combatThreadTPS);
	private static final long TICK_TIME = SEC_IN_NANO / TPS;
	// If the thread falls further behind than this, drop the backlog instead of bursting to catch up
	private static final long MAX_BEHIND = TICK_TIME * 5L;
    private Thread thread;
    protected Queue<Runnable> packets = new ConcurrentLinkedQueue<Runnable>();

    public AsyncPacketThread(String s) {
        this.thread = new Thread(new Runnable() {

            @Override
            public void run() {
            	AsyncPacketThread.this.loop();
            }
        }, s);
        this.thread.setDaemon(true);
        this.thread.start();
    }
    

    // Loops scanning for new packets to send at a fixed rate
	public void loop() {

		long nextTick = System.nanoTime();

		while (this.running) {
			long wait = nextTick - System.nanoTime();

			if (wait > 0) {
				// Sleeps with nanosecond precision instead of spinning on sub-millisecond waits
				LockSupport.parkNanos(wait);
				continue;
			}

			// Handle packets
			this.run();

			nextTick += TICK_TIME;
			long now = System.nanoTime();
			if (now - nextTick > MAX_BEHIND) {
				nextTick = now;
			}
		}
	}

    public void shutdown() {
        this.running = false;
        LockSupport.unpark(this.thread);
    }

    public abstract void run();

    // Queue a packet
    public void addPacket(final Packet<?>  packet, final NetworkManager manager, final GenericFutureListener<? extends Future<? super Void>>[] agenericfuturelistener) {
        this.packets.add(new Runnable() {

            @Override
            public void run() {
                Spigot404Write.writeThenFlush(manager.channel, packet, agenericfuturelistener);
            }
        });
    }

    public Thread getThread() {
        return this.thread;
    }

    // Store packet data
    public static class RunnableItem {
        private Channel channel;
        private Packet<?>  packet;

        public RunnableItem(Channel m, Packet<?>  p) {
            this.channel = m;
            this.packet = p;
        }

        public Packet<?> getPacket() {
            return this.packet;
        }

        public Channel getChannel() {
            return this.channel;
        }
    }
} 