package me.rastrian.dev.utils;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class IndexedLinkedHashSetTest {

	@Test
	public void sizeIsAlwaysAValidBoundForGet() {
		IndexedLinkedHashSet<String> set = new IndexedLinkedHashSet<>();
		set.add("a");
		set.add("b");
		set.add("a"); // duplicate

		Assert.assertEquals(2, set.size());
		for (int i = 0; i < set.size(); i++) {
			Assert.assertNotNull(set.get(i));
		}
		Assert.assertEquals("a", set.get(0));
		Assert.assertEquals("b", set.get(1));
	}

	@Test
	public void removeKeepsIndexesCompact() {
		IndexedLinkedHashSet<String> set = new IndexedLinkedHashSet<>();
		set.addAll(Arrays.asList("a", "b", "c"));

		Assert.assertTrue(set.remove("b"));
		Assert.assertFalse(set.remove("b"));
		Assert.assertEquals(2, set.size());
		Assert.assertEquals("c", set.get(1));
		Assert.assertFalse(set.contains("b"));
	}

	@Test
	public void bulkOperationsReportWhetherTheSetChanged() {
		IndexedLinkedHashSet<String> set = new IndexedLinkedHashSet<>();
		set.addAll(Arrays.asList("a", "b", "c"));

		Assert.assertFalse(set.removeAll(Collections.singleton("z")));
		Assert.assertEquals(3, set.size());

		Assert.assertTrue(set.removeAll(Collections.singleton("a")));
		Assert.assertEquals(2, set.size());
		Assert.assertEquals("b", set.get(0));

		Assert.assertFalse(set.retainAll(Arrays.asList("b", "c")));
		Assert.assertTrue(set.retainAll(Collections.singleton("c")));
		Assert.assertEquals(1, set.size());
		Assert.assertEquals("c", set.get(0));
	}
}
