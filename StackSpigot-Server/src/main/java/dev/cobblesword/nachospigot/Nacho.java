package dev.cobblesword.nachospigot;

import java.util.Set;

import dev.stackspigot.StackSpigot;
import dev.stackspigot.protocol.MovementListener;
import dev.stackspigot.protocol.PacketListener;

@Deprecated
public class Nacho {

	private static Nacho INSTANCE;

	public Nacho() {
		INSTANCE = this;
	}

	public static Nacho get() {
		return INSTANCE == null ? new Nacho() : INSTANCE;
	}

	public void registerCommands() {

	}

	public void registerPacketListener(PacketListener packetListener) {
		StackSpigot.getInstance().registerPacketListener(packetListener);
	}

	public void unregisterPacketListener(PacketListener packetListener) {
		StackSpigot.getInstance().unregisterPacketListener(packetListener);
	}

	public Set<PacketListener> getPacketListeners() {
		return StackSpigot.getInstance().getPacketListeners();
	}

	public void registerMovementListener(MovementListener movementListener) {
		StackSpigot.getInstance().registerMovementListener(movementListener);
	}

	public void unregisterMovementListener(MovementListener movementListener) {
		StackSpigot.getInstance().unregisterMovementListener(movementListener);
	}

	public Set<MovementListener> getMovementListeners() {
		return StackSpigot.getInstance().getMovementListeners();
	}

}
