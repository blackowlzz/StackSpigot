package dev.stackspigot.async.pathsearch;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.entity.EntityType;

import com.google.common.collect.Lists;
import dev.stackspigot.async.pathsearch.cache.SearchCacheEntry;
import dev.stackspigot.async.pathsearch.cache.SearchCacheEntryEntity;
import dev.stackspigot.async.pathsearch.cache.SearchCacheEntryPosition;
import dev.stackspigot.config.StackSpigotConfig;

import net.minecraft.server.BlockPosition;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityInsentient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Navigation;
import net.minecraft.server.PathEntity;
import net.minecraft.server.World;

/*
 *  A replacement for normal entity navigation that performs path searching async
 *  
 *  This is way faster than sync navigation (can handle thousands of entities with full AI), but has a few disadvantages.
 *  Entities' AI can be delayed for up to 2 ticks (very unlikely, but possible), so the cached target path might become inaccurate 
 *  after this time period, but most servers do not need to have perfectly accurate entity navigation. I believe the enhanced
 *  performance is worth it.
 *  
 *  This system performs an async calculation without using it when a entity should perform and use a sync calculation. We start an async
 *  calculation task when a path search is requested, then we return the result of the earlier calculation when a path search is requested again.
 *  This means that the entity does not do anything in terms of targeting for the first tick. If the calculations were not completed within
 *  2 ticks, the server will perform the path search on the main thread.
 *  
 */
public class AsyncNavigation extends Navigation {

	//StackSpigot-Code
	// Only the latest result of each kind is kept. The previous lists returned the oldest result of the entity,
	// whatever its target was, so once it was 3 ticks old every request fell back to a search on the main thread
	// until the lists were cleared, and the lists were copied on every added result.
	private volatile SearchCacheEntryEntity latestEntitySearch;
	private volatile SearchCacheEntryPosition latestPositionSearch;
	private int searchIssuedTick;
	//End-of-StackSpigot-Code
	
	public final AtomicBoolean isSearching = new AtomicBoolean(false);
	
	private int ticksSinceCleanup = 0;
	
	private static final List<EntityType> offloadedEntities = Lists.newArrayList();
	private static int minimumDistanceForOffloadingSquared = 0;

	public AsyncNavigation(EntityInsentient var1, World var2) {
		super(var1, var2);
	}
	
	public static void addOffloadedEntities(List<EntityType> entities) {
		offloadedEntities.addAll(entities);
	}
	
	private void issueSearch(Entity targetEntity) {
		this.searchIssuedTick = MinecraftServer.currentTick; // StackSpigot
		SearchHandler.getInstance().issueSearch(targetEntity, this);
	}
	
	private void issueSearch(int x, int y, int z) {
		this.searchIssuedTick = MinecraftServer.currentTick; // StackSpigot
		SearchHandler.getInstance().issueSearch(x, y, z, this);
	}

	//StackSpigot-Code
	// A search in progress that is not done within 2 ticks is replaced by a search on the main thread when the
	// accuracy is ensured
	private boolean searchTooSlow() {
		return StackSpigotConfig.ensurePathSearchAccuracy && this.isSearching.get()
				&& MinecraftServer.currentTick - this.searchIssuedTick >= 2;
	}
	//End-of-StackSpigot-Code
	
	@Override
	public PathEntity a(Entity targetEntity) {
		
		boolean isTooClose = this.b.h(targetEntity) < minimumDistanceForOffloadingSquared;
		boolean alreadySearching = this.isSearching.get();
		
		if ((!offLoadedSearches(this.getEntity().getBukkitEntity().getType()) || isTooClose) && !alreadySearching) {
			return super.a(targetEntity);
		}

		//StackSpigot-Code
		SearchCacheEntryEntity latest = this.latestEntitySearch;
		PathEntity previousPath = latest != null && latest.getTarget() == targetEntity ? latest.getPath() : null;
		if (previousPath != null && latest.isAccurate()) {
			return previousPath;
		}

		if (!this.isSearching.get()) {
			this.issueSearch(targetEntity);
		} else if (this.searchTooSlow()) {
			return super.a(targetEntity);
		}

		// Keep following the previous path to the same target until the new one is ready
		return previousPath;
		//End-of-StackSpigot-Code
	}
	
	@Override
	public PathEntity a(int x, int y, int z) {
		
		boolean isTooClose = this.b.distanceSquared(x, y, z) < minimumDistanceForOffloadingSquared;
		boolean alreadySearching = this.isSearching.get();
		
		if ((!offLoadedSearches(this.getEntity().getBukkitEntity().getType()) || isTooClose) && !alreadySearching) {
			return super.a(new BlockPosition(x, y, z));
		}

		//StackSpigot-Code
		SearchCacheEntryPosition latest = this.latestPositionSearch;
		PathEntity previousPath = latest != null && latest.getX() == x && latest.getY() == y && latest.getZ() == z
				? latest.getPath() : null;
		if (previousPath != null && latest.isAccurate()) {
			return previousPath;
		}

		if (!this.isSearching.get()) {
			this.issueSearch(x, y, z);
		} else if (this.searchTooSlow()) {
			return super.a(new BlockPosition(x, y, z));
		}

		return previousPath;
		//End-of-StackSpigot-Code
	}
	
	@Override
	public PathEntity a(BlockPosition blockposition) {
		return a(blockposition.getX(), blockposition.getY(), blockposition.getZ());
	}
	
	public void addEntry(SearchCacheEntry cacheEntry) {
		//StackSpigot-Code
		if (cacheEntry instanceof SearchCacheEntryEntity) {
			this.latestEntitySearch = (SearchCacheEntryEntity) cacheEntry;
		} else {
			this.latestPositionSearch = (SearchCacheEntryPosition) cacheEntry;
		}
		//End-of-StackSpigot-Code
	}
	
	@Override
	public void cleanUpExpiredSearches() {
		this.ticksSinceCleanup++;
		if (this.ticksSinceCleanup == 150) {
			this.ticksSinceCleanup = 0;
			
			//StackSpigot-Code
			this.latestEntitySearch = null;
			this.latestPositionSearch = null;
			//End-of-StackSpigot-Code
		}
	}

	private static boolean offLoadedSearches(EntityType type) {
		if (StackSpigotConfig.asyncPathSearches) {
			return offloadedEntities.contains(type);
		} else {
			return false;
		}
	}

	public static void setMinimumDistanceForOffloading(int distanceToAsync) {
		minimumDistanceForOffloadingSquared = distanceToAsync;
	}	
}
