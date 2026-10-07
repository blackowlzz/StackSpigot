package org.spigotmc;

import java.util.List;

import co.aikar.timings.SpigotTimings;
import net.minecraft.server.AxisAlignedBB;
import net.minecraft.server.Chunk;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityAmbient;
import net.minecraft.server.EntityAnimal;
import net.minecraft.server.EntityArrow;
import net.minecraft.server.EntityComplexPart;
import net.minecraft.server.EntityCreature;
import net.minecraft.server.EntityCreeper;
import net.minecraft.server.EntityEnderCrystal;
import net.minecraft.server.EntityEnderDragon;
import net.minecraft.server.EntityFallingBlock;
import net.minecraft.server.EntityFireball;
import net.minecraft.server.EntityFireworks;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityMonster;
import net.minecraft.server.EntityProjectile;
import net.minecraft.server.EntitySheep;
import net.minecraft.server.EntitySlime;
import net.minecraft.server.EntityTNTPrimed;
import net.minecraft.server.EntityVillager;
import net.minecraft.server.EntityWeather;
import net.minecraft.server.EntityWither;
import net.minecraft.server.MathHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.World;

public class ActivationRange {

	static AxisAlignedBB maxBB = AxisAlignedBB.a(0, 0, 0, 0, 0, 0);
	static AxisAlignedBB miscBB = AxisAlignedBB.a(0, 0, 0, 0, 0, 0);
	static AxisAlignedBB animalBB = AxisAlignedBB.a(0, 0, 0, 0, 0, 0);
	static AxisAlignedBB monsterBB = AxisAlignedBB.a(0, 0, 0, 0, 0, 0);

	/**
	 * Initializes an entities type on construction to specify what group this
	 * entity is in for activation ranges.
	 *
	 * @param entity
	 * @return group id
	 */
	public static byte initializeEntityActivationType(Entity entity) {
		if (entity instanceof EntityMonster || entity instanceof EntitySlime) {
			return 1; // Monster
		} else if (entity instanceof EntityCreature || entity instanceof EntityAmbient) {
			return 2; // Animal
		} else {
			return 3; // Misc
		}
	}

	/**
	 * These entities are excluded from Activation range checks.
	 *
	 * @param entity
	 * @param config
	 * @return boolean If it should always tick.
	 */
	public static boolean initializeEntityActivationState(Entity entity, SpigotWorldConfig config) {
		if ((entity.activationType == 3 && config.miscActivationRange == 0)
				|| (entity.activationType == 2 && config.animalActivationRange == 0)
				|| (entity.activationType == 1 && config.monsterActivationRange == 0) || entity instanceof EntityHuman
				|| entity instanceof EntityProjectile || entity instanceof EntityEnderDragon
				|| entity instanceof EntityComplexPart || entity instanceof EntityWither
				|| entity instanceof EntityFireball || entity instanceof EntityWeather
				|| entity instanceof EntityTNTPrimed || entity instanceof EntityFallingBlock // PaperSpigot - Always
																								// tick falling blocks
				|| entity instanceof EntityEnderCrystal || entity instanceof EntityFireworks) {
			return true;
		}

		return false;
	}

	/**
	 * Find what entities are in range of the players in the world and set active if
	 * in range.
	 *
	 * @param world
	 */
	public static void activateEntities(World world) {
		SpigotTimings.entityActivationCheckTimer.startTiming();
		//StackSpigot-Code
		final int miscActivationRange = world.spigotConfig.miscActivationRange;
		final int animalActivationRange = world.spigotConfig.animalActivationRange;
		final int monsterActivationRange = world.spigotConfig.monsterActivationRange;

		int maxRange = Math.max(monsterActivationRange, animalActivationRange);
		maxRange = Math.max(maxRange, miscActivationRange);
		maxRange = Math.min((world.spigotConfig.viewDistance << 4) - 8, maxRange);

		final List<Entity> players = (List<Entity>) (List) world.players;
		final int count = players.size();
		if (count == 0) {
			SpigotTimings.entityActivationCheckTimer.stopTiming();
			return;
		}
		ensureCapacity(count);

		groupIndex.clear();
		int groups = 0;
		for (int p = 0; p < count; p++) {
			Entity player = players.get(p);
			player.activatedTick = MinecraftServer.currentTick;
			AxisAlignedBB max = player.getBoundingBox().grow(maxRange, 256, maxRange);
			miscBoxes[p] = player.getBoundingBox().grow(miscActivationRange, 256, miscActivationRange);
			animalBoxes[p] = player.getBoundingBox().grow(animalActivationRange, 256, animalActivationRange);
			monsterBoxes[p] = player.getBoundingBox().grow(monsterActivationRange, 256, monsterActivationRange);

			int i = MathHelper.floor(max.a / 16.0D);
			int j = MathHelper.floor(max.d / 16.0D);
			int k = MathHelper.floor(max.c / 16.0D);
			int l = MathHelper.floor(max.f / 16.0D);

			long key;
			int dx = j - i;
			int dz = l - k;
			if (i >= -MAX_PACKED && i < MAX_PACKED && k >= -MAX_PACKED && k < MAX_PACKED && dx >= 0 && dx < 256
					&& dz >= 0 && dz < 256) {
				key = (i & 0x3FFFFFL) | ((k & 0x3FFFFFL) << 22) | ((long) dx << 44) | ((long) dz << 52);
			} else {
				key = Long.MIN_VALUE | p;
			}
			int group = groupIndex.get(key);
			if (group < 0) {
				group = groups++;
				groupIndex.put(key, group);
				groupMinX[group] = i;
				groupMaxX[group] = j;
				groupMinZ[group] = k;
				groupMaxZ[group] = l;
				groupSize[group] = 0;
			}
			playerGroup[p] = group;
			groupSize[group]++;
		}

		int offset = 0;
		for (int g = 0; g < groups; g++) {
			groupStart[g] = offset;
			offset += groupSize[g];
			groupSize[g] = 0;
		}
		for (int p = 0; p < count; p++) {
			int g = playerGroup[p];
			members[groupStart[g] + groupSize[g]++] = p;
		}

		for (int g = 0; g < groups; g++) {
			final int from = groupStart[g];
			final int to = from + groupSize[g];
			for (int i1 = groupMinX[g]; i1 <= groupMaxX[g]; ++i1) {
				for (int j1 = groupMinZ[g]; j1 <= groupMaxZ[g]; ++j1) {
					Chunk chunk = world.getChunkIfLoaded(i1, j1);
					if (chunk != null) {
						activateChunkEntities(chunk, from, to);
					}
				}
			}
		}
		for (int p = 0; p < count; p++) {
			miscBoxes[p] = null;
			animalBoxes[p] = null;
			monsterBoxes[p] = null;
		}
		//End-of-StackSpigot-Code
		SpigotTimings.entityActivationCheckTimer.stopTiming();
	}

	//StackSpigot-Code
	private static final int MAX_PACKED = 1 << 21;
	private static final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap groupIndex = newGroupIndex();
	private static AxisAlignedBB[] miscBoxes = new AxisAlignedBB[0];
	private static AxisAlignedBB[] animalBoxes = new AxisAlignedBB[0];
	private static AxisAlignedBB[] monsterBoxes = new AxisAlignedBB[0];
	private static int[] playerGroup = new int[0];
	private static int[] members = new int[0];
	private static int[] groupMinX = new int[0];
	private static int[] groupMaxX = new int[0];
	private static int[] groupMinZ = new int[0];
	private static int[] groupMaxZ = new int[0];
	private static int[] groupStart = new int[0];
	private static int[] groupSize = new int[0];

	private static it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap newGroupIndex() {
		it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap map = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
		map.defaultReturnValue(-1);
		return map;
	}

	private static void ensureCapacity(int count) {
		if (playerGroup.length >= count) {
			return;
		}
		int size = Math.max(count, playerGroup.length * 2);
		miscBoxes = new AxisAlignedBB[size];
		animalBoxes = new AxisAlignedBB[size];
		monsterBoxes = new AxisAlignedBB[size];
		playerGroup = new int[size];
		members = new int[size];
		groupMinX = new int[size];
		groupMaxX = new int[size];
		groupMinZ = new int[size];
		groupMaxZ = new int[size];
		groupStart = new int[size];
		groupSize = new int[size];
	}
	//End-of-StackSpigot-Code

	//StackSpigot-Code
	private static void activateChunkEntities(Chunk chunk, int from, int to) {
		for (List<Entity> slice : chunk.entitySlices) {
			int size = slice.size();
			if (size == 0) {
				continue;
			}
			for (int index = 0; index < size; index++) {
				Entity entity = slice.get(index);
				if (MinecraftServer.currentTick > entity.activatedTick) {
					if (entity.defaultActivationState) {
						entity.activatedTick = MinecraftServer.currentTick;
						continue;
					}
					AxisAlignedBB[] boxes;
					switch (entity.activationType) {
					case 1:
						boxes = monsterBoxes;
						break;
					case 2:
						boxes = animalBoxes;
						break;
					case 3:
					default:
						boxes = miscBoxes;
					}
					AxisAlignedBB entityBox = entity.getBoundingBox();
					for (int m = from; m < to; m++) {
						if (boxes[members[m]].b(entityBox)) {
							entity.activatedTick = MinecraftServer.currentTick;
							break;
						}
					}
				}
			}
		}
	}
	//End-of-StackSpigot-Code

	/**
	 * If an entity is not in range, do some more checks to see if we should give it
	 * a shot.
	 *
	 * @param entity
	 * @return
	 */
	public static boolean checkEntityImmunities(Entity entity) {
		// quick checks.
		if (entity.inWater || entity.fireTicks > 0) {
			return true;
		}
		if (!(entity instanceof EntityArrow)) {
			if (!entity.onGround || entity.passenger != null || entity.vehicle != null) {
				return true;
			}
		} else if (!((EntityArrow) entity).inGround) {
			return true;
		}
		// special cases.
		if (entity instanceof EntityLiving) {
			EntityLiving living = (EntityLiving) entity;
			if ( /* TODO: Missed mapping? living.attackTicks > 0 || */ living.hurtTicks > 0
					|| living.effects.size() > 0) {
				return true;
			}
			if (entity instanceof EntityCreature && ((EntityCreature) entity).getGoalTarget() != null) {
				return true;
			}
			if (entity instanceof EntityVillager && ((EntityVillager) entity).cm() /* Getter for first boolean */ ) {
				return true;
			}
			if (entity instanceof EntityAnimal) {
				EntityAnimal animal = (EntityAnimal) entity;
				if (animal.isBaby() || animal.isInLove()) {
					return true;
				}
				if (entity instanceof EntitySheep && ((EntitySheep) entity).isSheared()) {
					return true;
				}
			}
			if (entity instanceof EntityCreeper && ((EntityCreeper) entity).cn()) { // isExplosive
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the entity is active for this tick.
	 *
	 * @param entity
	 * @return
	 */
	public static boolean checkIfActive(Entity entity) {
		SpigotTimings.checkIfActiveTimer.startTiming();
		// Never safe to skip fireworks or entities not yet added to chunk
		if (!entity.isAddedToChunk() || entity instanceof EntityFireworks || entity.loadChunks) { // PaperSpigot
			SpigotTimings.checkIfActiveTimer.stopTiming();
			return true;
		}

		boolean isActive = entity.activatedTick >= MinecraftServer.currentTick || entity.defaultActivationState;

		// Should this entity tick?
		if (!isActive) {
			if ((MinecraftServer.currentTick - entity.activatedTick - 1) % 20 == 0) {
				// Check immunities every 20 ticks.
				if (checkEntityImmunities(entity)) {
					// Triggered some sort of immunity, give 20 full ticks before we check again.
					entity.activatedTick = MinecraftServer.currentTick + 20;
				}
				isActive = true;
			}
			// Add a little performance juice to active entities. Skip 1/4 if not immune.
		} else if (!entity.defaultActivationState && entity.ticksLived % 4 == 0 && !checkEntityImmunities(entity)) {
			isActive = false;
		}
		int x = MathHelper.floor(entity.locX);
		int z = MathHelper.floor(entity.locZ);
		// Make sure not on edge of unloaded chunk
		Chunk chunk = entity.world.getChunkIfLoaded(x >> 4, z >> 4);
		if (isActive && !(chunk != null && chunk.areNeighborsLoaded(1))) {
			isActive = false;
		}
		SpigotTimings.checkIfActiveTimer.stopTiming();
		return isActive;
	}
}
