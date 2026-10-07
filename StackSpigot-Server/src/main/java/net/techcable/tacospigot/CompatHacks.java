package net.techcable.tacospigot;

import org.bukkit.Bukkit;

public class CompatHacks {
	private CompatHacks() {
	}

	//StackSpigot-Code
	private static final long RECHECK_NANOS = 1_000_000_000L;
	private static volatile boolean protocolSupport;
	private static volatile long nextCheck = System.nanoTime();

	public static boolean hasProtocolSupport() {
		long now = System.nanoTime();
		if (now - nextCheck >= 0) {
			protocolSupport = Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport");
			nextCheck = now + RECHECK_NANOS;
		}
		return protocolSupport;
	}
	//End-of-StackSpigot-Code
}
