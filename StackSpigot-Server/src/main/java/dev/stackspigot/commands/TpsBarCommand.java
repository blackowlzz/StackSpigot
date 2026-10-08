package dev.stackspigot.commands;

import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import dev.stackspigot.config.StackSpigotConfig;
import net.minecraft.server.ChatComponentText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PacketPlayOutChat;

//StackSpigot-Code
// Shows the TPS, the MSPT and the ping in the action bar of the players that turned it on. Version 1.8 clients have
// no boss bar, so the action bar is used.
public class TpsBarCommand extends Command {

	private static final Set<UUID> BARS = ConcurrentHashMap.newKeySet();

	public TpsBarCommand(String name) {
		super(name);
		this.description = "Shows the TPS, the MSPT and your ping in the action bar";
		this.usageMessage = "/tpsbar";
		this.setPermission("stackspigot.command.tpsbar");
	}

	@Override
	public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		if (!testPermission(sender)) {
			return true;
		}
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Only players can use the TPS bar.");
			return true;
		}

		Player player = (Player) sender;
		if (BARS.remove(player.getUniqueId())) {
			// An empty action bar clears the last text right away
			send(player, "");
			player.sendMessage(ChatColor.GREEN + "TPS bar disabled.");
		} else {
			BARS.add(player.getUniqueId());
			update(player, format(), averageTps(), MinecraftServer.getServer().getAverageMspt());
			player.sendMessage(ChatColor.GREEN + "TPS bar enabled.");
		}
		return true;
	}

	// Called at the end of every tick on the main thread
	public static void tick(int ticks) {
		if (BARS.isEmpty() || ticks % StackSpigotConfig.tpsBarInterval != 0) {
			return;
		}
		final String format = format();
		final double tps = averageTps();
		final double mspt = MinecraftServer.getServer().getAverageMspt();
		for (Iterator<UUID> iterator = BARS.iterator(); iterator.hasNext();) {
			Player player = Bukkit.getPlayer(iterator.next());
			if (player == null) {
				iterator.remove();
			} else {
				update(player, format, tps, mspt);
			}
		}
	}

	private static String format() {
		return ChatColor.translateAlternateColorCodes('&', StackSpigotConfig.tpsBarFormat);
	}

	private static double averageTps() {
		return Math.min(20.0D, MinecraftServer.getServer().recentTps[0]);
	}

	private static void update(Player player, String format, double tps, double mspt) {
		final int ping = ((CraftPlayer) player).getPing();
		send(player, format
				.replace("%tps%", color(tps >= 19.0D ? 0 : tps >= 15.0D ? 1 : 2) + String.format(Locale.ROOT, "%.1f", tps))
				.replace("%mspt%", color(mspt < 40.0D ? 0 : mspt < 50.0D ? 1 : 2) + String.format(Locale.ROOT, "%.1f", mspt))
				.replace("%ping%", color(ping < 100 ? 0 : ping < 200 ? 1 : 2) + ping + "ms"));
	}

	private static String color(int level) {
		return (level == 0 ? ChatColor.GREEN : level == 1 ? ChatColor.YELLOW : ChatColor.RED).toString();
	}

	private static void send(Player player, String message) {
		((CraftPlayer) player).getHandle().playerConnection
				.sendPacket(new PacketPlayOutChat(new ChatComponentText(message), (byte) 2));
	}
}
//End-of-StackSpigot-Code
