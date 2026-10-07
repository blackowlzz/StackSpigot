# StackSpigot

##### StackSpigot is a performance-focused Minecraft 1.8.8 server software with improved PvP mechanics, forked from **[WindSpigot](https://github.com/Wind-Development/WindSpigot)**, itself a fork of **[NachoSpigot](https://github.com/Argarian-Network/NachoSpigot/tree/async-entity-tracker)**.

**StackSpigot is built and tested on Java 27. The Java 11 to 25 range supported by WindSpigot is expected to still apply.**

## Building
StackSpigot is built with [Maven](https://maven.apache.org/):
```
mvn clean package
```
The server jar is produced at `StackSpigot-Server/target/StackSpigot.jar` and the API jar at `StackSpigot-API/target/StackSpigot-API.jar`.

## Configuration
The main configuration file is `stackspigot.yml`, generated on first start. Use `--stackspigot-settings <file>` to point to a different file.

Knockback can be configured in-game with the `/kb` command.

## What StackSpigot changes
Changes made on top of WindSpigot:

**Project**
- Renamed to StackSpigot: modules, jars, the `dev.stackspigot` package, `stackspigot.yml`, `stackspigot.command.*` permissions, server brand and version.
- Removed the statistics client. WindSpigot sent the server count and player count to its own statistics server by default; StackSpigot makes no such connection and the `settings.statistics` option no longer exists.
- Default knockback profile renamed to `stack`.

**Compatibility**
- Plugins built for WindSpigot keep working without recompiling: the `com.windpvp.windspigot` classes (`WindSpigot`, `PacketListener`, `MovementListener`, `PlayerIllegalBehaviourEvent`), `getWindSpigotConfig()` and `MinecraftServer.getWindSpigot()` are kept as deprecated shims.
- The old `windspigot.command.*` permissions grant the new `stackspigot.command.*` ones.
- An existing `windspigot.yml` is copied to `stackspigot.yml` on first start.
- Plugins for Spigot, PaperSpigot, TacoSpigot and NachoSpigot 1.8.8 work as before. Plugins for newer Minecraft versions do not.

**Fixes**
- Async knockback could write packets to the wrong player. The packet queue was shared by all connections; each packet is now written only to its own channel.
- The entity tracker threw a `NullPointerException` for players that were still logging in.
- `IndexedLinkedHashSet` could report a size larger than its list, which caused hidden `IndexOutOfBoundsException` errors in the tracker threads. `removeAll` and `retainAll` now report whether the set changed.
- `FastRandom` stayed at zero forever when seeded with 0.

**Performance**
- The combat packet thread no longer drains its queue in quadratic time, no longer spins on sub-millisecond waits, and survives a failing packet write.
- The entity tracker reads the thread count once per tick, so a config reload cannot desynchronize its workers, and it does not schedule workers when nothing is tracked.
- The world ticker reuses its flush list instead of allocating one per world per tick.
- Packet writes without listeners use Netty's void promise, which removes one promise and listener allocation per packet and cut sampled allocations by about a third in a 600 player test.
- The protocol of a packet is looked up through a per class cache instead of a `HashMap` on every send.
- Packet and movement listeners are kept in copy-on-write sets, so sending a packet no longer iterates a concurrent hash set.
- Players with no queued packets are skipped when the entity tracker flushes, and the queue is lock-free.
- Entity activation range skips empty entity slices and no longer allocates an iterator per slice.
- `BlockPhysicsEvent` is only built and dispatched when a plugin listens to it, which removes an event and a block wrapper allocation from every block update on servers without such a plugin.
- VarInts are written with a fast path for the one and two byte case, and strings are written without an intermediate byte array. Both produce the same bytes as before.
- The per thread JDK NIO buffer cache is capped at 256 KiB (`jdk.nio.maxCachedBufferSize`, only if not already set), which stops long lived direct buffers from piling up native memory.
- Entity data watchers are initialized with `valueOf` instead of the deprecated boxing constructors.
- The packets queued by the entity tracker are written with one event loop task and one flush per connection instead of one task and one flush per packet. With a few hundred moving players this stops millions of pending write tasks from piling up in the Netty event loops (about 830 MB of live heap and 1.2 s of GC pauses in a 200 bot test, against about 90 MB and 40 ms) and lowered server CPU by about a third. Each connection keeps its own queue and channel, and the plugin packet listeners and the async knockback handling run exactly as before.
- The ProtocolSupport check that every packet serializer made against the plugin manager (a synchronized lookup that built new strings each time) is cached and refreshed once per second. In an 11 run comparison at 600 bots this lowered the memory allocated from about 8.8 GB to about 5.4 GB, the peak resident memory by about 11 to 17%, and the server CPU from about 142% to about 125% of a core. The median tick reported by `/tps` was about 0.5 ms higher in every one of those runs (about 3.4 ms against 2.9 ms), and the cause was not found.
- Entity activation visits every chunk once per group of players that share the same chunk range, instead of once per player. The set of activated entities is the same (a test compares it with the previous algorithm on 300 random worlds). With 600 players standing at the same spot the median tick measured inside the server went from 3.16 ms to 2.40 ms in 3 runs of 800 ticks. Players that are far apart from each other gain little.
- The packets queued by the entity tracker are taken from the queue and written by the Netty event loop of each connection, so the main thread no longer touches them. This only applies while no packet listener overrides `onSentPacket` (the built in anti crash listener does not); otherwise the previous path is used. With 600 players the tick measured inside the server went from a median of 2.18 ms to 1.94 ms, from 7.26 ms to 3.24 ms at the 90th percentile and from 18.39 ms to 7.43 ms at the 99th, in 4 runs of about 800 ticks each, with the same CPU use (about 106% of a core in both).

**Dependencies**
- Updated Maven plugins, Netty, commons-lang3, fastutil, log4j, snakeyaml, the SQLite and MySQL drivers and others. Mockito is test only and is no longer bundled in the server jar. Guava and Gson are kept in sync with Minecraft and were not changed.

Code written for StackSpigot is marked with `//StackSpigot-Code` and `//End-of-StackSpigot-Code` comments, see [NOTICE.md](NOTICE.md).

## Benchmarks
StackSpigot against the WindSpigot it was forked from (commit 283c604) and against the latest WindSpigot (commit 088e94b), 3 alternating runs each, 600 players online, default configuration, no plugins other than a small test plugin, on the same machine (Java 27, 8 GB heap, Java Flight Recorder enabled on all of them). The tick times are measured inside the server (`ServerTickEndEvent`) over the 40 seconds after all players joined, about 800 ticks per run.

| | WindSpigot 283c604 | WindSpigot 088e94b | StackSpigot |
|---|---|---|---|
| Median tick | 4.60 ms | 3.28 ms | 1.84 ms |
| 90th percentile tick | 14.39 ms | 8.25 ms | 2.98 ms |
| 99th percentile tick | 36.65 ms | 18.26 ms | 5.57 ms |
| Server CPU | 200% of a core | 150% of a core | 111% of a core |
| Memory allocated | 20.6 GB | 20.9 GB | 7.2 GB |
| Peak resident memory | 5786 MB | 5800 MB | 4546 MB |
| Total GC pause | 71 ms | 64 ms | 42 ms |

The median tick of every StackSpigot run (1.78, 1.81 and 1.92 ms) was lower than the median tick of every run of both WindSpigot versions. The players are bots that walk around, not a real server with plugins, so treat the numbers as indicative. Ramping up to 1000 bots, both StackSpigot and the latest WindSpigot kept the server above 19 TPS (worst tick 28 ms against 33 ms). In a 40 bot combat test the CPU use was the same as before the latest changes. With `settings.async.knockback` enabled, WindSpigot 283c604 delivered only about a tenth of the knockback packets to the players that were hit (about 100 against about 800 on StackSpigot) because of the shared packet queue that StackSpigot fixes.

## FAQ

#### What combat mechanics are improved on?
Potion speed and hit delay are configurable, and knockback is fully configurable through profiles.

#### What does StackSpigot do to improve overall performance?
Heavy work is moved off the main server thread (entity tracking, path searching, explosions, ...) and the server load is split up.

#### What other modifications does StackSpigot have?
See the patches list below.

## License
StackSpigot is released under the GNU General Public License version 3, see [LICENSE](LICENSE). Copyright and the requirement to keep the `//StackSpigot-Code` and `//End-of-StackSpigot-Code` markers are described in [NOTICE.md](NOTICE.md).

StackSpigot is built on the work of many projects and developers, credited in the patches list below.

## Patches
**All credit goes to the people that made these patches.**<br>
*Give credit where credit is due!*

The tags below (`WindSpigot-xxxx`, `Nacho-xxxx`, ...) identify the project each patch was inherited from.
```
[StackSpigot-0001] Remove statistics telemetry
[StackSpigot-0002] Fix async knockback packets written to the wrong channel
[StackSpigot-0003] Optimize the async combat packet thread
[StackSpigot-0004] Fix async entity tracker edge cases
[StackSpigot-0005] Reduce per-tick allocations in WorldTicker
[StackSpigot-0006] Fix FastRandom zero seed
[StackSpigot-0007] Update dependencies and build plugins
[StackSpigot-0008] Use the void promise for packet writes without listeners
[StackSpigot-0009] Cache the protocol lookup per packet class
[StackSpigot-0010] Use copy-on-write sets for packet and movement listeners
[StackSpigot-0011] Skip empty queued packet sends and use a lock-free queue
[StackSpigot-0012] Skip empty entity slices in ActivationRange
[StackSpigot-0013] Add WindSpigot plugin compatibility layer
[StackSpigot-0014] Only dispatch BlockPhysicsEvent when a plugin listens to it
[StackSpigot-0015] Write the queued entity tracker packets with one task and one flush per connection
[StackSpigot-0016] Cache the ProtocolSupport lookup of the packet serializer
[StackSpigot-0017] Group players with the same chunk range in the entity activation check
[StackSpigot-0018] Let the Netty event loop drain the packets queued by the entity tracker

[WindSpigot-0001] Thread affinity
[WindSpigot-0002] WindSpigot config
[WindSpigot-0003] Mob AI toggle command
[WindSpigot-0004] Parallel world ticking
[WindSpigot-0006] Remove FastMath
[WindSpigot-0007] Player ping command
[WindSpigot-0008] Make NachoSpigot's async TNT configurable
[WindSpigot-0009] Configurable entity hit delay
[WindSpigot-0010] Configurable potion speeds
[WindSpigot-0011] Make console display of player ips toggleable
[WindSpigot-0013] More configuration for knockback
[WindSpigot-0014] Async entity path searching
[WindSpigot-0015] Configurable explosion animations and sound
[WindSpigot-0016] Configurable weather changes
[WindSpigot-0017] Configurable fishing rod speed multiplier

[PandaSpigot-0115] Break up and make tab spam limits configurable
[PandaSpigot-0105] Fix SPIGOT-2622: Only one player receiving the server shutdown message
[PandaSpigot-0060] Backport SPIGOT-5428: Better handling of some ItemMeta
[PandaSpigot-0130] Backport Fix SPIGOT-3348: Skin Cache Bug
[PandaSpigot-0118] Fix SPIGOT-509: Negative or zero quantity items made when middle click dragging in creative
[PandaSpigot-0112] Fix MC-120567: Bed portal crash
[PandaSpigot-0107] Fix GH-276: Item durability desync when some events are cancelled
[PandaSpigot-0031] Add missing InventoryView.getSlotType API
[PandaSpigot-0104] Backport modern tick loop system
[PandaSpigot-0018] Set cap on JDK per-thread native byte buffer cache (by Aikar)
[PandaSpigot-0034] Optimize VarInt reading and writing (writing only, technique from Velocity)
[PandaSpigot-0035] Various micro-optimizations for PacketDataSerializer (string writing)
[PandaSpigot-0043] Only process BlockPhysicsEvent if a plugin has a listener (by Aikar, originally from PaperSpigot)
[PandaSpigot-0133] use valueOf in DataWatcher (by MasterDash5)

[Spigot-0097] Remove DataWatcher Locking by spottedleaf
[Spigot-0138] Branchless NibbleArray by md5
[Spigot-2380] Hitting in the air will always load the chunk at 0,0 by md_5

[Paper-0021] Implement Paper VersionChecker
[Paper-0033] Optimize explosions
[Paper-0044] Use UserCache for player heads
[Paper-0072] Fix Furnace cook time bug when lagging by Aikar
[Paper-0076] Optimized Light Level Comparisons by Aikar
[Paper-0083] Waving banner workaround by Gabscap
[Paper-0068] Use a Shared Random for Entities by Aikar
[Paper-0085] Add handshake event to allow plugins to handle client handshaking logic themselves
[Paper-0093] Don't save empty scoreboard teams to scoreboard.dat by Aikar
[Paper-0097] Faster redstone torch rapid clock removal by Martin Panzer
[Paper-0100] Avoid blocking on Network Manager creation by Aikar
[Paper-0102] Update log4j
[Paper-0103] Add setting for proxy online mode status
[Paper-0112] Reduce IO ops opening a new region file by Antony Riley
[Paper-0122] Don't let fishinghooks use portals by Zach Brown
[Paper-0125] Optimize World.isLoaded(BlockPosition)Z by Aikar
[Paper-0125] Improve Maps (in item frames) performance and bug fixes by Aikar
[Paper-0141] Do not let armorstands drown
[Paper-0144] Improve Minecraft Hopper Performance by Aikar
[Paper-0152] Disable ticking of snow blocks by killme
[Paper-0164] [MC-117075] TE Unload Lag Spike by mezz
[Paper-0168] Cache user authenticator threads by vemacs
[Paper-0207] Shame on you Mojang moves chunk loading off https thread by Aikar
[Paper-0249] Improve BlockPosition inlining by Techcable
[Paper-0254] Don't blindly send unlit chunks when lighting updates are allowed by Shane Freeder
[Paper-0266] [MC-99321] Dont check for blocked double chest for hoppers
[Paper-0302] Don't load chunks for villager door checks by Aikar
[Paper-0313] Optimize World Time Updates by Aikar
[Paper-0321] Server Tick Events
[Paper-0342] Always process chunk removal in removeEntity by Aikar 2018
[Paper-0344] [MC-111480] Start Entity ID's at 1
[Paper-0346] [MC-135506] Experience should save as Integers
[Paper-0347] don't go below 0 for pickupDelay, breaks picking up items by Aikar
[Paper-0350] use a Queue for Queueing Commands by Aikar
[Paper-0352] Optimize BlockPosition helper methods by Spottedleaf
[Paper-0353] Send nearby packets from world player list not server list by Mystiflow
[Paper-0389] performance improvement for Chunk.getEntities by wea_ondara
[Paper-0539] Optimize NetworkManager Exception Handling by Andrew Steinborn
[Paper-0451] Reduce memory footprint of NBTTagCompound by spottedleaf
[Paper-0797] Use Velocity compression and cipher natives
[Paper-????] Cleanup allocated favicon ByteBuf by Shane Freeder
[Paper-????] MC-80966 - Always send chunk sections

<--> by Heath
[Nacho-0001] Remove stream usage when counting entities
[Nacho-0002] Check if the fuel is coal first before checking others
[Nacho-0003] Disable Snooper
[Nacho-0004] Do not repeatily allocate EnumDirection
[Nacho-0005] Do not reallocate enums via values
[Nacho-0006] Use Caffeine instead of Guava for player heads
[Nacho-0007] Add timings for packets
[Nacho-0008] Upgrade Netty version to 4.1.50 and support java 14
[Nacho-0009] Remove an extra file io call within world credit bob7l
[Nacho-0010] Use jchambers' FAST UUID methods
[Nacho-0011] Optimize weather update loops
[Nacho-0012] Don't load chunks for physics
[Nacho-0013] Use less resources for collisions
[Nacho-0014] stop timings crashing the server but still print the error
[Nacho-0015] Remove the usage of BlockPosition from getCubes
[Nacho-0016] faster getHighestBlockYAt function
[Nacho-0017] tiny winy optimization for async lighting
[Nacho-0018] more tiny winy optimization to lighting
[Nacho-0019] Avoid lock every packet send
[Nacho-0020] Packet Listener Api
[Nacho-0021] Add setMaxPlayers within Bukkit.getServer() and SetMaxSlot Command
[Nacho-0022] Stop raytracing loading chunks
[Nacho-0023] Optimize EntityTracker for the chunk updater
[Nacho-0024] Do not create new BlockPosition when loading chunk
[Nacho-0025] Disable random tickSpeed being modified (Every call it had to convert String into int via a string key which is costly)
[Nacho-0026] Optimize packet Split by Velocity
[Nacho-0027] Netty IP_TOS 0x18
[Nacho-0028] only fire InventoryCloseEvent if inventory is open
[Nacho-0029] add leash api
[Nacho-0030] add a ChunkPreLoadEvent
[Nacho-0031] remove unused vars
[Nacho-0033] Faster Operator search method
[Nacho-0048] Don't allocate empty int arrays for particles
[Nacho-0049] Option to disable Enchantment table ticking

<--> by Rastrian
[Nacho-????] Async knockback and hit detection packets
[Nacho-????] Multithreaded entity tracking
[Nacho-????] Ticking fixes, tile optimization, and optional fast math
[Nacho-????] Many more config options

<--> by Sculas
[Nacho-0034] Remove Java 8 message from TacoSpigot which made it so you couldn't run Java 8 or higher
[Nacho-0035] Made it so you can switch the brand name in nacho.yml
[Nacho-0036] Add toggles for commands "reload", "version" and "plugins"
[Nacho-0037] Add toggle for "Faster Operator"
[Nacho-0040] Change deprecated Netty parameter in ResourceLeakDetector
[Nacho-0041] Fix block placement
[Nacho-0042] Remove Spigot Watchdog
[Nacho-0043] Fix Citizens
[Nacho-0044] Async obfuscation
[Nacho-0045] Add Player#jump and Player#sendActionBar
[Nacho-0046] Little anti-malware
[Nacho-0047] Little anti-crash
[Nacho-0050] Custom knockback
[Nacho-0051] Rework ServerConnection and MinecraftPipeline (credits to Minestom)

[Yatopia-0030] Don't save Fireworks and Arrows by tr7zw (Arrows and firework Entities, eg stuck arrows in the ground)
[Yatopia-0047] Smarter statistics ticking
[Yatopia-0050] Smol entity optimisation

[IonSpigot-0003] Explosion Improvements
[IonSpigot-0006] Fix Chunk Loading
[IonSpigot-0013] Implement PandaWire
[IonSpigot-0014] Faster Chunk Entity List
[IonSpigot-0020] Faster EntityTracker Collections
[IonSpigot-0026] Lag Compensated Potions
[IonSpigot-0035] Optimise Entity Collisions
[IonSpigot-0037] Fast Cannon Entity Tracker

[InsanePaper-269] Cache Chunk Coordinations
[InsanePaper-390] Heavily optimize Tuinity controlled flush patch

[Akarin-0001] Avoid double I/O operation on load player file by tsao chi
[Akarin-0010] Save Json list asynchronously

[Tuinity-????] Skip updating entity tracker without players
[Tuinity-0017] Allow controlled flushing for network manager by Spottedleaf
[Tuinity-0018] Consolidate flush calls for entity tracker packets
[Tuinity-0052] Optimise non-flush packet sending

[SportPaper-0027] Fix head rotation packet spam
[SportPaper-0043] Get blocks in Chunk API
[SportPaper-0162] Fix PlayerInteractEvent not cancelling properly
[SportPaper-0197] Optimize head rotation patch
[SportPaper-0201] Cache block break animation packet
[SportPaper-0203] Fix Teleport Invisibility
[SportPaper-0204] Optimize toLegacyData removing unneeded sanity checks
[SportPaper-0260] Migrate from Trove to fastutil

[PaperBin-????] WorldServer#everyoneDeeplySleeping optimization

[KigPaper-0039] Fix Entity and Command Block memory leaks
[KigPaper-0128] Fix Entity and Command Block memory leaks
[KigPaper-0129] Fix more EnchantmentManager leaks
[KigPaper-0138] Fix some more memory leaks
[KigPaper-0161] Fix CraftingManager memory leak
[KigPaper-0167] Add setType without lighting update API
[KigPaper-0191] Don't calculate initial light if not requested

[FlamePaper-0032] Dont load chunks for chests
[FlamePaper-0033] Dont check occluding hoppers
[FlamePaper-0034] Hopper item lookup optimizations
[FlamePaper-0102] Fixed chunk memory leak
[FlamePaper-0104] Return last slot by default
[FlamePaper-0105] Fix memory leaks by Minetick
[FlamePaper-0106] Fix sending irrelevant block updates to the client
[FlamePaper-0110] Fix NullPointerException exploits for invalid logins
[FlamePaper-0113] Remove unused code from beacons
[FlamePaper-0115] Patch Book Exploits
[FlamePaper-0117] Pearl through blocks

[MineTick-0006] Fix Occasional Client Side Unloading of Chunk 0 0
[MineTick-0011] Optimize Idle Furnaces
[MineTick-0017] Fix Insane Nether Portal Lag

[Migot-0009] Prevent Creature Spawning in Unloaded Chunks

[Sugarcane-0022] Add YAML comments

[FalchusSpigot-????] Use ThreadLocalRandom
[FalchusSpigot-????] Use fastutil
[FalchusSpigot-????] Optimize config defaults
[FalchusSpigot-????] Update invalidly placed blocks (Credits: Quadflame/Refrains)
[FalchusSpigot-????] Fix SPIGOT-1401: dispenser, dropper, furnace placement
[FalchusSpigot-????] Fix SPIGOT-4647: Properly cancel VehicleDamageEvent
[FalchusSpigot-????] Fix MC-87 map scaling/cloning issues
[FalchusSpigot-????] Fix view distance lookup
[FalchusSpigot-????] Only send Dragon/Wither Death sounds to same world
[FalchusSpigot-????] Improve NetworkManager
[FalchusSpigot-????] Add FastNetworkManager (batched packet writes, the idea behind StackSpigot-0015)

[DashSpigot-0033] Fix SPIGOT-1746: Tile entities may not always tick
[DashSpigot-0011] Fix MC-94186: Dragon egg falling in lazy chunks

[Titanium-0189] Fix MC-124833
```
