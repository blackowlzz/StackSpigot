package dev.stackspigot.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.PacketDataSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

//StackSpigot-Code
public class PacketDataSerializerTest {

	@BeforeClass
	public static void setUpServer() {
		if (Bukkit.getServer() == null) {
			Server server = org.mockito.Mockito.mock(Server.class);
			org.mockito.Mockito.when(server.getPluginManager()).thenReturn(org.mockito.Mockito.mock(PluginManager.class));
			org.mockito.Mockito.when(server.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
			Bukkit.setServer(server);
		}
	}

	private static void referenceVarInt(ByteBuf out, int i) {
		while ((i & -128) != 0) {
			out.writeByte(i & 127 | 128);
			i >>>= 7;
		}
		out.writeByte(i);
	}

	private static byte[] bytes(ByteBuf buf) {
		byte[] b = new byte[buf.readableBytes()];
		buf.getBytes(buf.readerIndex(), b);
		return b;
	}

	@Test
	public void varIntMatchesReference() {
		int[] values = { 0, 1, 127, 128, 255, 16383, 16384, 2097151, 2097152, 268435455, 268435456, Integer.MAX_VALUE,
				-1, -128, Integer.MIN_VALUE };
		for (int v : values) {
			ByteBuf expected = Unpooled.buffer();
			referenceVarInt(expected, v);
			PacketDataSerializer actual = new PacketDataSerializer(Unpooled.buffer());
			actual.b(v);
			Assert.assertArrayEquals("value " + v, bytes(expected), bytes(actual));
			Assert.assertEquals(v, actual.e());
		}
	}

	@Test
	public void stringMatchesReference() {
		String[] values = { "", "a", "StackSpigot", "caffè", "日本語のテキスト", "emoji 😀 pair", "x".repeat(300) };
		for (String s : values) {
			byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
			ByteBuf expected = Unpooled.buffer();
			referenceVarInt(expected, utf8.length);
			expected.writeBytes(utf8);
			PacketDataSerializer actual = new PacketDataSerializer(Unpooled.buffer());
			actual.a(s);
			Assert.assertArrayEquals(bytes(expected), bytes(actual));
			Assert.assertEquals(s, actual.c(32767));
		}
	}

	@Test(expected = io.netty.handler.codec.EncoderException.class)
	public void tooLongStringIsRejected() {
		new PacketDataSerializer(Unpooled.buffer()).a("x".repeat(32768));
	}
}
//End-of-StackSpigot-Code
