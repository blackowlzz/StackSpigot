package org.spigotmc;

import net.minecraft.server.AxisAlignedBB;
import net.minecraft.server.Chunk;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.MathHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.World;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

//StackSpigot-Code
public class ActivationRangeTest {

	private static final int TICK = 100;

	private static void set(Object target, String name, Object value) throws Exception {
		Field field = findField(target.getClass(), name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static boolean hasField(Class<?> type, String name) {
		try {
			type.getDeclaredField(name);
			return true;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	private static Field findField(Class<?> type, String name) throws Exception {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			if (hasField(c, name)) {
				return c.getDeclaredField(name);
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static Entity entity(Class<? extends Entity> type, double x, double y, double z, int activationType,
			boolean defaultState) throws Exception {
		Entity entity = Mockito.mock(type);
		set(entity, "activationType", (byte) activationType);
		set(entity, "defaultActivationState", defaultState);
		AxisAlignedBB box = AxisAlignedBB.a(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
		Mockito.when(entity.getBoundingBox()).thenReturn(box);
		entity.activatedTick = Integer.MIN_VALUE;
		return entity;
	}

	private static final class Scenario {
		World world;
		List<Entity> all = new ArrayList<>();
	}

	private static Scenario scenario(long seed) throws Exception {
		Random random = new Random(seed);
		Scenario s = new Scenario();
		Map<Long, Chunk> chunks = new HashMap<>();
		for (int cx = -12; cx <= 12; cx++) {
			for (int cz = -12; cz <= 12; cz++) {
				if (random.nextDouble() < 0.85) {
					Chunk chunk = Mockito.mock(Chunk.class);
					List<Entity>[] slices = new List[16];
					for (int i = 0; i < 16; i++) {
						slices[i] = new ArrayList<>();
					}
					set(chunk, "entitySlices", slices);
					chunks.put(((long) cx << 32) | (cz & 0xFFFFFFFFL), chunk);
				}
			}
		}
		World world = Mockito.mock(World.class);
		Mockito.when(world.getChunkIfLoaded(Mockito.anyInt(), Mockito.anyInt())).thenAnswer(
				i -> chunks.get(((long) (int) i.getArgument(0) << 32) | ((int) i.getArgument(1) & 0xFFFFFFFFL)));
		SpigotWorldConfig config = Mockito.mock(SpigotWorldConfig.class);
		config.viewDistance = 2 + random.nextInt(9);
		config.miscActivationRange = random.nextInt(4) == 0 ? 0 : 8 + random.nextInt(24);
		config.animalActivationRange = 16 + random.nextInt(32);
		config.monsterActivationRange = 16 + random.nextInt(32);
		set(world, "spigotConfig", config);
		List<EntityHuman> players = new ArrayList<>();
		set(world, "players", players);
		s.world = world;

		int playerCount = random.nextInt(60);
		for (int p = 0; p < playerCount; p++) {
			double x;
			double z;
			if (random.nextDouble() < 0.7) {
				x = random.nextDouble() * 6 - 3;
				z = random.nextDouble() * 6 - 3;
			} else {
				x = random.nextDouble() * 240 - 120;
				z = random.nextDouble() * 240 - 120;
			}
			Entity player = entity(EntityHuman.class, x, 64, z, 3, true);
			players.add((EntityHuman) player);
			s.all.add(player);
			place(chunks, player, x, 64, z);
		}
		int mobCount = random.nextInt(300);
		for (int m = 0; m < mobCount; m++) {
			double x = random.nextDouble() * 280 - 140;
			double z = random.nextDouble() * 280 - 140;
			Entity mob = entity(Entity.class, x, 64, z, 1 + random.nextInt(3), random.nextInt(10) == 0);
			s.all.add(mob);
			place(chunks, mob, x, 64, z);
		}
		return s;
	}

	private static void place(Map<Long, Chunk> chunks, Entity entity, double x, double y, double z) throws Exception {
		int cx = MathHelper.floor(x / 16.0D);
		int cz = MathHelper.floor(z / 16.0D);
		Chunk chunk = chunks.get(((long) cx << 32) | (cz & 0xFFFFFFFFL));
		if (chunk != null) {
			chunk.entitySlices[MathHelper.clamp(MathHelper.floor(y / 16.0D), 0, 15)].add(entity);
		}
	}

	private static AxisAlignedBB legacyMisc;
	private static AxisAlignedBB legacyAnimal;
	private static AxisAlignedBB legacyMonster;

	private static void legacyActivate(World world) {
		final int miscActivationRange = world.spigotConfig.miscActivationRange;
		final int animalActivationRange = world.spigotConfig.animalActivationRange;
		final int monsterActivationRange = world.spigotConfig.monsterActivationRange;

		int maxRange = Math.max(monsterActivationRange, animalActivationRange);
		maxRange = Math.max(maxRange, miscActivationRange);
		maxRange = Math.min((world.spigotConfig.viewDistance << 4) - 8, maxRange);

		for (Entity player : (List<Entity>) (List) world.players) {
			player.activatedTick = MinecraftServer.currentTick;
			AxisAlignedBB maxBB = player.getBoundingBox().grow(maxRange, 256, maxRange);
			legacyMisc = player.getBoundingBox().grow(miscActivationRange, 256, miscActivationRange);
			legacyAnimal = player.getBoundingBox().grow(animalActivationRange, 256, animalActivationRange);
			legacyMonster = player.getBoundingBox().grow(monsterActivationRange, 256, monsterActivationRange);

			int i = MathHelper.floor(maxBB.a / 16.0D);
			int j = MathHelper.floor(maxBB.d / 16.0D);
			int k = MathHelper.floor(maxBB.c / 16.0D);
			int l = MathHelper.floor(maxBB.f / 16.0D);

			for (int i1 = i; i1 <= j; ++i1) {
				for (int j1 = k; j1 <= l; ++j1) {
					Chunk chunk = world.getChunkIfLoaded(i1, j1);
					if (chunk != null) {
						for (List<Entity> slice : chunk.entitySlices) {
							for (int index = 0; index < slice.size(); index++) {
								Entity entity = slice.get(index);
								if (MinecraftServer.currentTick > entity.activatedTick) {
									if (entity.defaultActivationState) {
										entity.activatedTick = MinecraftServer.currentTick;
										continue;
									}
									switch (entity.activationType) {
									case 1:
										if (legacyMonster.b(entity.getBoundingBox())) {
											entity.activatedTick = MinecraftServer.currentTick;
										}
										break;
									case 2:
										if (legacyAnimal.b(entity.getBoundingBox())) {
											entity.activatedTick = MinecraftServer.currentTick;
										}
										break;
									case 3:
									default:
										if (legacyMisc.b(entity.getBoundingBox())) {
											entity.activatedTick = MinecraftServer.currentTick;
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private static List<Boolean> activated(Scenario s) {
		List<Boolean> result = new ArrayList<>();
		for (Entity entity : s.all) {
			result.add(entity.activatedTick == MinecraftServer.currentTick);
		}
		return result;
	}

	@Test
	public void groupedActivationMatchesPerPlayerActivation() throws Exception {
		MinecraftServer.currentTick = TICK;
		int activatedTotal = 0;
		for (long seed = 0; seed < 300; seed++) {
			Scenario s = scenario(seed);
			legacyActivate(s.world);
			List<Boolean> expected = activated(s);
			for (Entity entity : s.all) {
				entity.activatedTick = Integer.MIN_VALUE;
			}
			ActivationRange.activateEntities(s.world);
			List<Boolean> actual = activated(s);
			Assert.assertEquals("seed " + seed, expected, actual);
			for (boolean b : expected) {
				if (b) {
					activatedTotal++;
				}
			}
		}
		Assert.assertTrue(activatedTotal > 1000);
	}

	@Test
	public void noPlayersActivatesNothing() throws Exception {
		MinecraftServer.currentTick = TICK;
		Scenario s = scenario(7);
		set(s.world, "players", new ArrayList<EntityHuman>());
		ActivationRange.activateEntities(s.world);
		for (Entity entity : s.all) {
			Assert.assertNotEquals(TICK, entity.activatedTick);
		}
	}
}
//End-of-StackSpigot-Code
