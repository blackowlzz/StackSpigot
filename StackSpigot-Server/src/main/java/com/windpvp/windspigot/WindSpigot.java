package com.windpvp.windspigot;

import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.windpvp.windspigot.protocol.MovementListener;
import com.windpvp.windspigot.protocol.PacketListener;

import dev.stackspigot.StackSpigot;

//StackSpigot-Code
@Deprecated
public class WindSpigot {

	public static final Logger LOGGER = StackSpigot.LOGGER;
	private static final WindSpigot INSTANCE = new WindSpigot();

	private WindSpigot() {
	}

	public static WindSpigot getInstance() {
		return StackSpigot.getInstance() == null ? null : INSTANCE;
	}

	public static void init() {
		StackSpigot.init();
	}

	public static void debug(String msg) {
		StackSpigot.debug(msg);
	}

	public void reload() {
		StackSpigot.getInstance().reload();
	}

	public void registerPacketListener(PacketListener packetListener) {
		StackSpigot.getInstance().registerPacketListener(packetListener);
	}

	public void unregisterPacketListener(PacketListener packetListener) {
		StackSpigot.getInstance().unregisterPacketListener(packetListener);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Set<PacketListener> getPacketListeners() {
		return (Set) StackSpigot.getInstance().getPacketListeners();
	}

	public void registerMovementListener(MovementListener movementListener) {
		StackSpigot.getInstance().registerMovementListener(movementListener);
	}

	public void unregisterMovementListener(MovementListener movementListener) {
		StackSpigot.getInstance().unregisterMovementListener(movementListener);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Set<MovementListener> getMovementListeners() {
		return (Set) StackSpigot.getInstance().getMovementListeners();
	}
}
//End-of-StackSpigot-Code
