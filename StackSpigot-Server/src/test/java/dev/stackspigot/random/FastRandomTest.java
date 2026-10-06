package dev.stackspigot.random;

import org.junit.Assert;
import org.junit.Test;

//StackSpigot-Code
public class FastRandomTest {

	@Test
	public void zeroSeedDoesNotGetStuck() {
		FastRandom random = new FastRandom(0L);
		boolean nonZero = false;
		for (int i = 0; i < 16; i++) {
			nonZero |= random.nextInt() != 0;
		}
		Assert.assertTrue(nonZero);

		random.setSeed(0L);
		nonZero = false;
		for (int i = 0; i < 16; i++) {
			nonZero |= random.nextInt() != 0;
		}
		Assert.assertTrue(nonZero);
	}

	@Test
	public void sameSeedGivesSameSequence() {
		FastRandom a = new FastRandom(1234L);
		FastRandom b = new FastRandom(1234L);
		for (int i = 0; i < 32; i++) {
			Assert.assertEquals(a.nextInt(), b.nextInt());
		}
	}
}
//End-of-StackSpigot-Code
