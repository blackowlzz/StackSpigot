package dev.stackspigot;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.command.SimpleCommandMap;

import com.google.common.collect.Sets;
import dev.stackspigot.async.pathsearch.SearchHandler;
import dev.stackspigot.async.thread.CombatThread;
import dev.stackspigot.commands.KnockbackCommand;
import dev.stackspigot.commands.MobAICommand;
import dev.stackspigot.commands.PingCommand;
import dev.stackspigot.commands.SetMaxSlotCommand;
import dev.stackspigot.commands.SpawnMobCommand;
import dev.stackspigot.config.StackSpigotConfig;
import dev.stackspigot.protocol.MovementListener;
import dev.stackspigot.protocol.PacketListener;

import net.minecraft.server.MinecraftServer;
import xyz.sculas.nacho.anticrash.AntiCrash;
import xyz.sculas.nacho.async.AsyncExplosions;

public class StackSpigot {

	public static final Logger LOGGER = LogManager.getLogger();
	private static final Logger DEBUG_LOGGER = LogManager.getLogger();
	private static StackSpigot INSTANCE;
	
	private CombatThread knockbackThread;
	
	private final Set<PacketListener> packetListeners = Sets.newConcurrentHashSet();
	private final Set<MovementListener> movementListeners = Sets.newConcurrentHashSet();

	private StackSpigot() {
		initCmds();
		
		// We do not want to initialize this again after a reload
		if (StackSpigotConfig.asyncPathSearches && SearchHandler.getInstance() == null) {
			new SearchHandler();
		}
		
		if (StackSpigotConfig.asyncKnockback) {
			knockbackThread = new CombatThread("Knockback Thread");
		}
		if (StackSpigotConfig.asyncTnt) {
			AsyncExplosions.initExecutor(StackSpigotConfig.fixedPoolSize);
		}
		if (StackSpigotConfig.enableAntiCrash) {
			registerPacketListener(new AntiCrash());
		}
	}

	public void reload() {
		StackSpigot.init();
	}

	private void initCmds() {
		
		SimpleCommandMap commandMap = MinecraftServer.getServer().server.getCommandMap();
		
		if (StackSpigotConfig.mobAiCmd) {
			MobAICommand mobAiCommand = new MobAICommand("mobai");
			commandMap.register(mobAiCommand.getName(), "", mobAiCommand);
		}
		
		if (StackSpigotConfig.pingCmd) {
			PingCommand pingCommand = new PingCommand("ping");
			commandMap.register(pingCommand.getName(), "", pingCommand);
		}
		
		// NachoSpigot commands
		if (StackSpigotConfig.setMaxSlotCommand) {
			SetMaxSlotCommand setMaxSlotCommand = new SetMaxSlotCommand("sms"); // [Nacho-0021] Add setMaxPlayers within Bukkit.getServer() and SetMaxSlot Command
			commandMap.register(setMaxSlotCommand.getName(), "ns", setMaxSlotCommand);
		}

		if (StackSpigotConfig.spawnMobCommand) {
			SpawnMobCommand spawnMobCommand = new SpawnMobCommand("spawnmob");
			commandMap.register(spawnMobCommand.getName(), "ns", spawnMobCommand);
		}

		if (StackSpigotConfig.knockbackCommand) {
			KnockbackCommand knockbackCommand = new KnockbackCommand("kb");
			commandMap.register(knockbackCommand.getName(), "ns", knockbackCommand);
		}
	}

	public static void init() {
		if (INSTANCE == null) {
			INSTANCE = new StackSpigot();
		}
	}

	public CombatThread getKnockbackThread() {
		return knockbackThread;
	}
    
	public static void debug(String msg) {
		if (StackSpigotConfig.debugMode)
			DEBUG_LOGGER.info(msg);
	}
	
	public void registerPacketListener(PacketListener packetListener) {
		this.packetListeners.add(packetListener);
	}

	public void unregisterPacketListener(PacketListener packetListener) {
		this.packetListeners.remove(packetListener);
	}

	public Set<PacketListener> getPacketListeners() {
		return this.packetListeners;
	}

	public void registerMovementListener(MovementListener movementListener) {
		this.movementListeners.add(movementListener);
	}

	public void unregisterMovementListener(MovementListener movementListener) {
		this.movementListeners.remove(movementListener);
	}

	public Set<MovementListener> getMovementListeners() {
		return this.movementListeners;
	}
	
	public static StackSpigot getInstance() {
		return INSTANCE;
	}
}
