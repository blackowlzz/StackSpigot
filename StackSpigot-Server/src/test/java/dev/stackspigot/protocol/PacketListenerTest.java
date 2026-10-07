package dev.stackspigot.protocol;

import net.minecraft.server.Packet;
import net.minecraft.server.PlayerConnection;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

//StackSpigot-Code
public class PacketListenerTest {

	private static final class ReceivedOnly implements PacketListener {
		@Override
		public boolean onReceivedPacket(PlayerConnection connection, Packet<?> packet) {
			return false;
		}
	}

	private static final class SentOverride implements PacketListener {
		@Override
		public boolean onSentPacket(PlayerConnection connection, Packet<?> packet) {
			return false;
		}
	}

	@Test
	public void receivedOnlyListenerDoesNotOverrideSent() {
		Assert.assertFalse(PacketListener.overridesSentPacket(new ReceivedOnly()));
		Assert.assertFalse(PacketListener.overridesSentPacket(new PacketListener() {
		}));
	}

	@Test
	public void sentOverrideIsDetected() {
		Assert.assertTrue(PacketListener.overridesSentPacket(new SentOverride()));
		Assert.assertTrue(PacketListener.overridesSentPacket(new PacketListener() {
			@Override
			public boolean onSentPacket(PlayerConnection connection, Packet<?> packet) {
				return true;
			}
		}));
	}

	@Test
	public void anyOverridesLooksAtEveryListener() {
		Assert.assertFalse(PacketListener.anyOverridesSentPacket(Collections.<PacketListener>emptyList()));
		Assert.assertFalse(PacketListener.anyOverridesSentPacket(Arrays.<PacketListener>asList(new ReceivedOnly())));
		Assert.assertTrue(PacketListener.anyOverridesSentPacket(
				Arrays.<PacketListener>asList(new ReceivedOnly(), new SentOverride())));
	}
}
//End-of-StackSpigot-Code
