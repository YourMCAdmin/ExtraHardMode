package me.ryanhamshire.ExtraHardMode.features.monsters;


import me.ryanhamshire.ExtraHardMode.ExtraHardMode;
import me.ryanhamshire.ExtraHardMode.config.RootConfig;
import me.ryanhamshire.ExtraHardMode.config.RootNode;
import me.ryanhamshire.ExtraHardMode.config.messages.MessageConfig;
import me.ryanhamshire.ExtraHardMode.config.messages.MessageNode;
import me.ryanhamshire.ExtraHardMode.module.DataStoreModule;
import me.ryanhamshire.ExtraHardMode.module.EntityModule;
import me.ryanhamshire.ExtraHardMode.service.PermissionNode;
import me.ryanhamshire.ExtraHardMode.task.DragonAttackPatternTask;
import me.ryanhamshire.ExtraHardMode.task.DragonAttackTask;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Glydia implements Listener
{
    ExtraHardMode plugin = null;
    RootConfig CFG = null;
    MessageConfig messages;
    EntityModule entityModule;
    DataStoreModule data;

    public Glydia(ExtraHardMode plugin)
    {
        this.plugin = plugin;
        CFG = plugin.getModuleForClass(RootConfig.class);
        messages = plugin.getModuleForClass(MessageConfig.class);
        entityModule = plugin.getModuleForClass(EntityModule.class);
        data = plugin.getModuleForClass(DataStoreModule.class);
    }

    /**
     * When a Block is broken in the End
     *
     * @param breakEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent breakEvent)
    {
        Block block = breakEvent.getBlock();
        World world = block.getWorld();
        Player player = breakEvent.getPlayer();

        final boolean endNoBuilding = CFG.getBoolean(RootNode.ENDER_DRAGON_NO_BUILDING, world.getName());
        final boolean playerBypass = player.hasPermission(PermissionNode.BYPASS.getNode()) || player.getGameMode() == GameMode.CREATIVE;

        // FEATURE: very limited building in the end
        // players are allowed to break only end stone, and only to create a stair
        // up to ground level
        if (endNoBuilding && world.getEnvironment() == World.Environment.THE_END &&! playerBypass)
        {
            if (block.getType() != Material.ENDER_STONE)
            {
                breakEvent.setCancelled(true);
                plugin.sendMessage(player, messages.getString(MessageNode.LIMITED_END_BUILDING));
                return;
            }
            else
            {
                int absoluteDistanceFromBlock = Math.abs(block.getX() - player.getLocation().getBlockX());
                int zdistance = Math.abs(block.getZ() - player.getLocation().getBlockZ());
                if (zdistance > absoluteDistanceFromBlock)
                {
                    absoluteDistanceFromBlock = zdistance;
                }

                if (block.getY() < player.getLocation().getBlockY() + absoluteDistanceFromBlock)
                {
                    breakEvent.setCancelled(true);
                    plugin.sendMessage(player, messages.getString(MessageNode.LIMITED_END_BUILDING));
                    return;
                }
            }
        }
    }

    /**
     * When a Block is placed
     *
     * @param placeEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent placeEvent)
    {
        Player player = placeEvent.getPlayer();
        Block block = placeEvent.getBlock();
        World world = block.getWorld();

        final boolean enderDragonNoBuilding = CFG.getBoolean(RootNode.ENDER_DRAGON_NO_BUILDING, world.getName());
        final boolean playerBypass = player.hasPermission(PermissionNode.BYPASS.getNode()) || player.getGameMode() == GameMode.CREATIVE;

        // FEATURE: very limited building in the end players are allowed to break only end stone, and only to create a stair up to ground level
        if (enderDragonNoBuilding && world.getEnvironment() == World.Environment.THE_END &&! playerBypass)
        {
            placeEvent.setCancelled(true);
            plugin.sendMessage(player, messages.getString(MessageNode.LIMITED_END_BUILDING));
            return;
        }
    }

    /**
     * When the Dragon dies
     *
     * @param event
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event)
    {
        LivingEntity entity = event.getEntity();
        World world = entity.getWorld();

        final boolean endNoBuilding = CFG.getBoolean(RootNode.ENDER_DRAGON_DROPS_VILLAGER_EGGS, world.getName());
        final boolean enderDragonDropsEggs = CFG.getBoolean(RootNode.ENDER_DRAGON_DROPS_EGG, world.getName());
        final boolean announcements = CFG.getBoolean(RootNode.ENDER_DRAGON_COMBAT_ANNOUNCEMENTS, world.getName());

        // FEATURE: ender dragon drops prizes on death
        if (entity instanceof EnderDragon)
        {
            if (endNoBuilding)
            {
                ItemStack itemStack = new ItemStack(Material.MONSTER_EGG, 2, (short) 120);
                world.dropItemNaturally(entity.getLocation().add(10, 0, 0), itemStack);
            }

            if (enderDragonDropsEggs)
            {
                world.dropItemNaturally(entity.getLocation().add(10, 0, 0), new ItemStack(Material.DRAGON_EGG));
            }

            if (announcements)
            {
                StringBuilder builder = new StringBuilder("The dragon has been defeated!  ( By: ");
                for (String player : data.getPlayers())
                {
                    builder.append(player).append(" ");
                }
                builder.append(")");

                plugin.getServer().broadcastMessage(builder.toString());
            }

            if (endNoBuilding)
            {
                for (String player : data.getPlayers())
                {
                    if (plugin.getServer().getPlayer(player) != null)
                    {
                        Player player1 = plugin.getServer().getPlayer(player);
                        plugin.sendMessage(player1, messages.getString(MessageNode.DRAGON_FOUNTAIN_TIP));
                    }
                }
            }

            data.getPlayers().clear();
        }
    }

    /**
     * When the Player dies while fighting the dragon
     *
     * @param event
     */
    @EventHandler
    public void onPlayerDeath (PlayerDeathEvent event)
    {
        Player player = event.getEntity();

        final boolean dragonAnnouncements = CFG.getBoolean(RootNode.ENDER_DRAGON_COMBAT_ANNOUNCEMENTS, event.getEntity().getWorld().getName());

        // announce the combat result
        data.getPlayers().remove(player.getName());
        if (dragonAnnouncements && !player.isDead())
        {
            plugin.getServer().broadcastMessage(player.getName() + " was killed while fighting the dragon!");
        }
    }

    /**
     * When the Player changes World while fighting the Dragon, remove him from the Players fighting the Dragon
     *
     * @param event
     */
    @EventHandler
    public void onPlayerTpOut (PlayerChangedWorldEvent event)
    {
        String playerName = event.getPlayer().getName();
        if (event.getFrom().getEnvironment() == World.Environment.THE_END && data.getPlayers().contains(playerName))
            data.getPlayers().remove(playerName);
    }

    /**
     * When a Player enters the End and the Portal is in the air, try to build a bridge
     */
    @EventHandler
    public void onPlayerEnter (PlayerChangedWorldEvent event)
    {
    }

    /**
     * When the Dragon is damaged
     *
     * @param event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event)
    {
        Entity entity = event.getEntity();
        World world = entity.getWorld();

        // is this an entity damaged by entity event?
        EntityDamageByEntityEvent damageByEntityEvent = null;
        if (event instanceof EntityDamageByEntityEvent)
        {
            damageByEntityEvent = (EntityDamageByEntityEvent) event;
        }

        final boolean dragonAdditionalAttacks =  CFG.getBoolean(RootNode.ENDER_DRAGON_ADDITIONAL_ATTACKS, world.getName());
        final boolean dragonAnnouncements = CFG.getBoolean(RootNode.ENDER_DRAGON_COMBAT_ANNOUNCEMENTS, world.getName());

        // FEATURE: the dragon has new attacks
        if (dragonAdditionalAttacks && damageByEntityEvent != null && entity.getType() == EntityType.ENDER_DRAGON)
        {
            Player damager = null;
            if (damageByEntityEvent.getDamager() instanceof Player)
            {
                damager = (Player) damageByEntityEvent.getDamager();
            }
            else if (damageByEntityEvent.getDamager() instanceof Projectile)
            {
                Projectile projectile = (Projectile) damageByEntityEvent.getDamager();
                if (projectile.getShooter() != null && projectile.getShooter() instanceof Player)
                {
                    damager = (Player) projectile.getShooter();
                }
            }

            if (damager != null)
            {
                if (!data.getPlayers().contains(damager.getName()))
                {
                    data.getPlayers().add(damager.getName());

                    DragonAttackPatternTask task = new DragonAttackPatternTask(plugin, (LivingEntity) entity, damager, data.getPlayers());
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, 1L);

                    if (dragonAnnouncements)
                    {
                        plugin.getServer().broadcastMessage(damager.getName() + " is challenging the dragon!");
                    }
                }

                for (int i = 0; i < 5; i++)
                {
                    DragonAttackTask task = new DragonAttackTask(plugin, entity, damager);
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, 20L * (plugin.getRandom().nextInt(15)));
                }

                Chunk chunk = damager.getLocation().getChunk();
                Entity[] entities = chunk.getEntities();
                for (Entity entity1 : entities)
                {
                    if (entity1.getType() == EntityType.ENDERMAN)
                    {
                        Enderman enderman = (Enderman) entity1;
                        enderman.setTarget(damager);
                    }
                }
            }
        }
    }

    /**
     * when a player changes from the End to another world, clean up if End empty
     *
     * @param event - Event that occurred.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerChangeWorld(PlayerChangedWorldEvent event)
    {
        World world = event.getFrom();

        final boolean respawnDragon = CFG.getBoolean(RootNode.RESPAWN_ENDER_DRAGON, world.getName());

        // FEATURE: respawn the ender dragon when the last player leaves the end
        if (respawnDragon && world.getEnvironment() == World.Environment.THE_END && world.getPlayers().size() == 0) //Once everyone has left
        {
            // look for an ender dragon
            EnderDragon enderDragon = null;
            for (Entity entity : world.getEntities())
            {
                if (enderDragon != null && entity instanceof EnderDragon)
                {  //If there is already a dragon for whatever reason, remove it
                    entity.remove();
                }
                if (entity instanceof EnderDragon)
                {
                    enderDragon = (EnderDragon) entity;
                }
                // clean up any summoned minions
                if (entity.getType().equals(EntityType.ZOMBIE) || entity.getType().equals(EntityType.BLAZE))
                {
                    entity.remove();
                }
            }

            // if he's there, full health
            if (enderDragon != null)
            {
                enderDragon.setHealth(enderDragon.getMaxHealth());
            }

            // otherwise, spawn one
            else
            {
                entityModule.spawn(new Location(world, 0, world.getMaxHeight() - 1, 0), EntityType.ENDER_DRAGON);
            }
        }
    }

    /**
     * when an item spawns
     *
     * @param event - Event that occurred.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent event)
    {
        // FEATURE: fountain effect from dragon fireball explosions sometimes causes fire to drop as an item. this is the fix for that.
        //Note: eeeeeeh Feature?!
        Item item = event.getEntity();
        World world = item.getWorld();

        //TODO add method to test if activated for world
        if (item.getItemStack().getType() == Material.FIRE)
        {
            event.setCancelled(true);
        }
    }

    /**
     * when an entity targets something (as in to attack it)...
     *
     * @param event - Event that occurred.
     */
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event)
    {
        Entity entity = event.getEntity();
        World world = entity.getWorld();

        //TODO method for activated in world
        // FEATURE: monsters don't target the ender dragon
        if (event.getTarget() != null && event.getTarget() instanceof EnderDragon)
        {
            event.setCancelled(true);
        }
    }

    /**
     *
     * @param event
     */
    @EventHandler
    public void onExplosion(EntityExplodeEvent event)
    {
        World world = event.getLocation().getWorld();
        Entity entity = event.getEntity();

        final boolean dragonAdditionalAttacks = CFG.getBoolean(RootNode.ENDER_DRAGON_ADDITIONAL_ATTACKS, world.getName());

        // FEATURE: ender dragon fireballs may summon minions and/or set fires
        if (dragonAdditionalAttacks && entity != null && entity.getType() == EntityType.FIREBALL)
        {
            Fireball fireball = (Fireball) entity;
            Entity spawnedMonster = null;
            if (fireball.getShooter() != null && fireball.getShooter().getType() == EntityType.ENDER_DRAGON)
            {
                int random = plugin.getRandom().nextInt(100);
                if (random < 40)
                {
                    spawnedMonster = entity.getWorld().spawnEntity(entity.getLocation(), EntityType.BLAZE);

                    for (int x1 = -2; x1 <= 2; x1++)
                    {
                        for (int z1 = -2; z1 <= 2; z1++)
                        {
                            for (int y1 = 2; y1 >= -2; y1--)
                            {
                                Block block = fireball.getLocation().add(x1, y1, z1).getBlock();
                                Material underType = block.getRelative(BlockFace.DOWN).getType();
                                if (block.getType() == Material.AIR && underType != Material.AIR && underType != Material.FIRE)
                                {
                                    block.setType(Material.FIRE);
                                }
                            }
                        }
                    }

                    Location location = fireball.getLocation().add(0, 1, 0);
                    for (int i = 0; i < 10; i++)
                    {
                        FallingBlock fire = world.spawnFallingBlock(location, Material.FIRE, (byte) 0);
                        Vector velocity = Vector.getRandom();
                        if (velocity.getY() < 0)
                        {
                            velocity.setY(velocity.getY() * -1);
                        }
                        if (plugin.getRandom().nextBoolean())
                        {
                            velocity.setZ(velocity.getZ() * -1);
                        }
                        if (plugin.getRandom().nextBoolean())
                        {
                            velocity.setX(velocity.getX() * -1);
                        }
                        fire.setVelocity(velocity);
                    }
                }
                else if (random < 70)
                {
                    for (int i = 0; i < 2; i++)
                    {
                        spawnedMonster = entity.getWorld().spawnEntity(entity.getLocation(), EntityType.ZOMBIE);
                        entityModule.markLootLess((LivingEntity) spawnedMonster);
                        Zombie zombie = (Zombie) spawnedMonster;
                        zombie.setVillager(true);
                    }
                }
                else
                {
                    spawnedMonster = entity.getWorld().spawnEntity(entity.getLocation(), EntityType.ENDERMAN);
                }
            }

            if (spawnedMonster != null)
            {
                entityModule.markLootLess((LivingEntity) spawnedMonster);
            }
        }
    }
}
