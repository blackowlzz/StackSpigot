package dev.stackspigot.async.netty;

import org.junit.Assert;
import org.junit.Test;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.server.Packet;
import net.minecraft.server.PacketDataSerializer;
import net.minecraft.server.PacketListener;

//StackSpigot-Code
public class Spigot404WriteTest {

	private static Packet<PacketListener> newPacket() {
		return new Packet<PacketListener>() {
			@Override
			public void a(PacketDataSerializer serializer) {
			}

			@Override
			public void b(PacketDataSerializer serializer) {
			}

			@Override
			public void a(PacketListener listener) {
			}
		};
	}

	@Test
	public void packetsAreNotDeliveredToOtherChannels() {
		EmbeddedChannel first = new EmbeddedChannel();
		EmbeddedChannel second = new EmbeddedChannel();

		Packet<?> forFirst = newPacket();
		Packet<?> forSecond = newPacket();

		Spigot404Write.writeThenFlush(first, forFirst, null);
		Spigot404Write.writeThenFlush(second, forSecond, null);

		Assert.assertSame(forFirst, first.readOutbound());
		Assert.assertNull(first.readOutbound());
		Assert.assertSame(forSecond, second.readOutbound());
		Assert.assertNull(second.readOutbound());
	}

	@Test
	public void closedOrMissingChannelsAreIgnored() {
		EmbeddedChannel closed = new EmbeddedChannel();
		closed.close();

		Packet<?> packet = newPacket();
		Spigot404Write.writeThenFlush(closed, packet, null);
		Spigot404Write.writeThenFlush(null, packet, null);

		Assert.assertNull(closed.readOutbound());
	}
}
//End-of-StackSpigot-Code
