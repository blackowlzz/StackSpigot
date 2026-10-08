package dev.stackspigot.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.server.EnumProtocol;
import net.minecraft.server.EnumProtocolDirection;
import net.minecraft.server.NetworkManager;
import net.minecraft.server.Packet;
import net.minecraft.server.PacketDataSerializer;
import net.minecraft.server.PacketEncoder;
import net.minecraft.server.PacketPlayOutCustomPayload;
import net.minecraft.server.PacketPlayOutKeepAlive;
import net.minecraft.server.PacketPrepender;

//StackSpigot-Code
public class MergedWriteTest {

	@org.junit.BeforeClass
	public static void setUpServer() {
		if (org.bukkit.Bukkit.getServer() == null) {
			org.bukkit.Server server = org.mockito.Mockito.mock(org.bukkit.Server.class);
			org.mockito.Mockito.when(server.getPluginManager()).thenReturn(org.mockito.Mockito.mock(org.bukkit.plugin.PluginManager.class));
			org.mockito.Mockito.when(server.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
			org.bukkit.Bukkit.setServer(server);
		}
	}

	private static EmbeddedChannel newChannel() {
		EmbeddedChannel channel = new EmbeddedChannel();
		channel.pipeline().addLast("prepender", PacketPrepender.INSTANCE)
				.addLast("encoder", new PacketEncoder(EnumProtocolDirection.CLIENTBOUND));
		channel.attr(NetworkManager.ATTRIBUTE_PROTOCOL).set(EnumProtocol.PLAY);
		return channel;
	}

	private static int lastBuffers;

	private static byte[] drain(EmbeddedChannel channel) {
		ByteBuf all = Unpooled.buffer();
		Object message;
		lastBuffers = 0;
		while ((message = channel.readOutbound()) != null) {
			lastBuffers++;
			ByteBuf buf = (ByteBuf) message;
			all.writeBytes(buf);
			buf.release();
		}
		byte[] bytes = new byte[all.readableBytes()];
		all.readBytes(bytes);
		return bytes;
	}

	private static byte[] throughPipeline(List<Supplier<Packet<?>>> packets) {
		EmbeddedChannel channel = newChannel();
		for (Supplier<Packet<?>> packet : packets) {
			channel.write(packet.get());
		}
		channel.flush();
		return drain(channel);
	}

	private static byte[] throughBatch(List<Supplier<Packet<?>>> packets, EmbeddedChannel channel) {
		NetworkManager manager = new NetworkManager(EnumProtocolDirection.CLIENTBOUND);
		manager.channel = channel;
		List<Packet<?>> batch = new ArrayList<>();
		for (Supplier<Packet<?>> packet : packets) {
			batch.add(packet.get());
		}
		manager.handleBatch(batch);
		return drain(channel);
	}

	private static Supplier<Packet<?>> payload(int size) {
		return () -> {
			byte[] data = new byte[size];
			for (int i = 0; i < size; i++) {
				data[i] = (byte) (i * 31 + size);
			}
			return new PacketPlayOutCustomPayload("StackSpigot", new PacketDataSerializer(Unpooled.wrappedBuffer(data)));
		};
	}

	private static List<Supplier<Packet<?>>> mixedPackets() {
		List<Supplier<Packet<?>>> packets = new ArrayList<>();
		for (int i = 0; i < 2000; i++) {
			final int id = i;
			packets.add(() -> new PacketPlayOutKeepAlive(id));
			if (i % 50 == 0) {
				// Lengths that need one, two and three VarInt bytes
				packets.add(payload(100));
				packets.add(payload(115));
				packets.add(payload(116));
				packets.add(payload(5000));
				packets.add(payload(20000));
			}
		}
		return packets;
	}

	@Test
	public void mergedWriteMatchesPipeline() {
		List<Supplier<Packet<?>>> packets = mixedPackets();
		EmbeddedChannel channel = newChannel();
		byte[] merged = throughBatch(packets, channel);
		// The merged path writes a few large buffers instead of a length and a body buffer per packet
		Assert.assertTrue("merged writes: " + lastBuffers, lastBuffers < packets.size() / 10);
		Assert.assertArrayEquals(throughPipeline(packets), merged);
	}

	@Test
	public void unknownOutboundHandlerUsesPipeline() {
		List<Supplier<Packet<?>>> packets = mixedPackets();
		EmbeddedChannel channel = newChannel();
		// A handler added by a plugin that rewrites the encoded packets must still see every packet
		channel.pipeline().addBefore("prepender", "plugin_handler", new MessageToByteEncoder<ByteBuf>() {
			@Override
			protected void encode(io.netty.channel.ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
				out.writeByte(0x7F);
				out.writeBytes(msg);
			}
		});
		byte[] batched = throughBatch(packets, channel);
		Assert.assertEquals(packets.size() * 2, lastBuffers);

		EmbeddedChannel reference = newChannel();
		reference.pipeline().addBefore("prepender", "plugin_handler", new MessageToByteEncoder<ByteBuf>() {
			@Override
			protected void encode(io.netty.channel.ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
				out.writeByte(0x7F);
				out.writeBytes(msg);
			}
		});
		for (Supplier<Packet<?>> packet : packets) {
			reference.write(packet.get());
		}
		reference.flush();
		Assert.assertArrayEquals(drain(reference), batched);
	}
}
