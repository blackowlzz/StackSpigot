package net.techcable.tacospigot;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

//StackSpigot-Code
public class CompatHacksTest {

	@BeforeClass
	public static void setUpServer() {
		if (Bukkit.getServer() == null) {
			Server server = Mockito.mock(Server.class);
			Mockito.when(server.getPluginManager()).thenReturn(Mockito.mock(PluginManager.class));
			Mockito.when(server.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
			Bukkit.setServer(server);
		}
	}

	@Test
	public void lookupIsCachedAndRefreshed() throws InterruptedException {
		PluginManager manager = Bukkit.getPluginManager();
		Mockito.when(manager.isPluginEnabled("ProtocolSupport")).thenReturn(false);
		Thread.sleep(1100);
		Assert.assertFalse(CompatHacks.hasProtocolSupport());

		Mockito.when(manager.isPluginEnabled("ProtocolSupport")).thenReturn(true);
		Mockito.clearInvocations(manager);
		for (int i = 0; i < 1000; i++) {
			Assert.assertFalse(CompatHacks.hasProtocolSupport());
		}
		Mockito.verify(manager, Mockito.atMost(1)).isPluginEnabled("ProtocolSupport");

		Thread.sleep(1100);
		Assert.assertTrue(CompatHacks.hasProtocolSupport());
	}
}
//End-of-StackSpigot-Code
