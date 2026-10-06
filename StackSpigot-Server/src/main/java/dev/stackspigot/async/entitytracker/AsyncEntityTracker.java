package dev.stackspigot.async.entitytracker;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.stackspigot.async.AsyncUtil;
import dev.stackspigot.async.ResettableLatch;
import dev.stackspigot.config.StackSpigotConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.rastrian.dev.utils.IndexedLinkedHashSet;
import net.minecraft.server.*;

public class AsyncEntityTracker extends EntityTracker {
	
	private static final ExecutorService trackingThreadExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("StackSpigot Entity Tracker Thread").build());
	private final WorldServer worldServer;	
	
	public AsyncEntityTracker(WorldServer worldserver) {
		super(worldserver);
		this.worldServer = worldserver;
	}
	
	@Override
	public void updatePlayers() {
		final IndexedLinkedHashSet<EntityTrackerEntry> entries = (IndexedLinkedHashSet<EntityTrackerEntry>) c;
		final int size = entries.size();
		// Read the config once so every worker uses the same stride, even if it is reloaded mid-tick
		final int threads = Math.min(Math.max(1, StackSpigotConfig.trackingThreads), Math.max(1, size));

		if (size > 0) {
			final ResettableLatch latch = worldServer.ticker.getLatch();
			latch.reset(threads);

			for (int offset = 0; offset < threads; offset++) {
				final int finalOffset = offset;

				AsyncUtil.run(() -> {
					try {
						for (int index = finalOffset; index < size; index += threads) {
							try {
								entries.get(index).update();
							} catch (Throwable t) {
								t.printStackTrace();
							}
						}
					} finally {
						latch.decrement();
					}
				}, trackingThreadExecutor);

			}
			try {
				latch.waitTillZero();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		for (EntityPlayer player : MinecraftServer.getServer().getPlayerList().players) {
			// A player that is still logging in does not have a connection yet
			PlayerConnection connection = player.playerConnection;
			if (connection != null) {
				connection.sendQueuedPackets();
			}
		}
	}

	public static ExecutorService getExecutor() {
		return trackingThreadExecutor;
	}
}
