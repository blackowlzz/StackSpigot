package dev.stackspigot.protocol;

import net.minecraft.server.Packet;
import net.minecraft.server.PlayerConnection;

public interface PacketListener {
	default boolean onReceivedPacket(PlayerConnection playerConnection, Packet<?> packet) {
		return true;
	}

	default boolean onSentPacket(PlayerConnection connection, Packet<?> packet) {
		return true;
	}

	//StackSpigot-Code
	static boolean overridesSentPacket(PacketListener listener) {
		return OverrideCache.SENT.get(listener.getClass());
	}

	static boolean anyOverridesSentPacket(Iterable<PacketListener> listeners) {
		for (PacketListener listener : listeners) {
			if (overridesSentPacket(listener)) {
				return true;
			}
		}
		return false;
	}

	final class OverrideCache {
		private static final ClassValue<Boolean> SENT = new ClassValue<Boolean>() {
			@Override
			protected Boolean computeValue(Class<?> type) {
				try {
					return type.getMethod("onSentPacket", PlayerConnection.class, Packet.class)
							.getDeclaringClass() != PacketListener.class;
				} catch (NoSuchMethodException e) {
					return Boolean.TRUE;
				}
			}
		};

		private OverrideCache() {
		}
	}
	//End-of-StackSpigot-Code
}
