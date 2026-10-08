package net.minecraft.server;

import java.util.Random;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

//StackSpigot-Code
public class ContainerSlotItemTest {

	@BeforeClass
	public static void bootstrap() {
		DispenserRegistry.c();
	}

	private static ItemStack stack() {
		return Mockito.mock(ItemStack.class);
	}

	private static void fill(PlayerInventory inventory, InventoryCrafting crafting, Random random) {
		for (int i = 0; i < inventory.items.length; i++) {
			inventory.items[i] = random.nextBoolean() ? stack() : null;
		}
		for (int i = 0; i < inventory.armor.length; i++) {
			inventory.armor[i] = random.nextBoolean() ? stack() : null;
		}
		for (int i = 0; i < crafting.getContents().length; i++) {
			crafting.getContents()[i] = random.nextBoolean() ? stack() : null;
		}
	}

	private static void assertSameItems(Container container) {
		for (int i = 0; i < container.c.size(); i++) {
			Assert.assertSame("slot " + i, container.c.get(i).getItem(), container.slotItem(i));
		}
	}

	@Test
	public void playerContainerReadsTheSameItems() {
		PlayerInventory inventory = new PlayerInventory(null);
		ContainerPlayer container = new ContainerPlayer(inventory, true, null);
		Random random = new Random(1);

		for (int round = 0; round < 20; round++) {
			fill(inventory, container.craftInventory, random);
			container.resultInventory.setItem(0, random.nextBoolean() ? stack() : null);
			assertSameItems(container);
		}

		// Loading a player replaces the arrays of the inventory
		inventory.items = new ItemStack[36];
		inventory.armor = new ItemStack[4];
		fill(inventory, container.craftInventory, random);
		assertSameItems(container);
	}

	@Test
	public void slotsWithTheirOwnGetItemAreUsed() {
		final ItemStack fixed = stack();
		PlayerInventory inventory = new PlayerInventory(null);
		Container container = new Container() {
			{
				this.a(new Slot(inventory, 0, 0, 0));
				this.a(new Slot(inventory, 1, 0, 0) {
					@Override
					public ItemStack getItem() {
						return fixed;
					}
				});
			}

			@Override
			public boolean a(EntityHuman entityhuman) {
				return true;
			}

			@Override
			public org.bukkit.inventory.InventoryView getBukkitView() {
				return null;
			}
		};
		inventory.items[0] = stack();
		inventory.items[1] = stack();
		assertSameItems(container);
		Assert.assertSame(fixed, container.slotItem(1));

		// A slot added later is picked up
		container.a(new Slot(inventory, 37, 0, 0));
		inventory.armor[1] = stack();
		assertSameItems(container);
	}
}
//End-of-StackSpigot-Code
