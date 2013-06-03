/*
 * This file is part of
 * ExtraHardMode Server Plugin for Minecraft
 *
 * Copyright (C) 2012 Ryan Hamshire
 * Copyright (C) 2013 Diemex
 *
 * ExtraHardMode is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ExtraHardMode is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with ExtraHardMode.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.extrahardmode.features.monsters;

import com.extrahardmode.ExtraHardMode;
import com.extrahardmode.config.RootConfig;
import com.extrahardmode.config.RootNode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;


/**
 * Changes to Enderman including:
 *
 * Teleportation of the Player towards the Enderman ,
 */
public class Bobs implements Listener
{
    private ExtraHardMode plugin = null;
    private RootConfig CFG = null;

    public Bobs(ExtraHardMode plugin)
    {
        this.plugin = plugin;
        CFG = plugin.getModuleForClass(RootConfig.class);
    }

    /**
     * when an entity (not a player) teleports...
     *
     * @param event - Event that occurred.
     */
    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event)
    {
        Entity entity = event.getEntity();
        World world = entity.getWorld();

        final boolean improvedEndermanTeleportation = CFG.getBoolean(RootNode.IMPROVED_ENDERMAN_TELEPORTATION, world.getName());

        if (entity instanceof Enderman && improvedEndermanTeleportation && world.getEnvironment().equals(World.Environment.NORMAL))
        {
            Enderman enderman = (Enderman) entity;

            // ignore endermen which aren't fighting players
            if (enderman.getTarget() == null || !(enderman.getTarget() instanceof Player))
                return;

            // ignore endermen which are taking damage from the environment (to
            // avoid rapid teleportation due to rain or suffocation)
            if (enderman.getLastDamageCause() != null && enderman.getLastDamageCause().getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK)
                return;

            // ignore endermen which are in caves (standing on stone)
            if (enderman.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.STONE)
                return;

            Player player = (Player) enderman.getTarget();

            // ignore when player is in a different world from the enderman
            if (!player.getWorld().equals(enderman.getWorld()))
                return;

            // half the time, teleport the player instead
            if (plugin.random(50))
            {
                event.setCancelled(true);
                int distanceSquared = (int) player.getLocation().distanceSquared(enderman.getLocation());

                // play sound at old location
                world.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                Block destinationBlock;

                // if the player is far away
                if (distanceSquared > 75)
                {
                    // have the enderman swap places with the player
                    destinationBlock = enderman.getLocation().getBlock();
                    enderman.teleport(player.getLocation());
                }

                // otherwise if the player is close
                else
                {
                    // teleport the player to the enderman's destination
                    destinationBlock = event.getTo().getBlock();
                }

                while (destinationBlock.getType() != Material.AIR || destinationBlock.getRelative(BlockFace.UP).getType() != Material.AIR)
                {
                    destinationBlock = destinationBlock.getRelative(BlockFace.UP);
                }

                player.teleport(destinationBlock.getLocation(), PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
                //TODO EhmEndermanTeleportEvent

                // play sound at new location
                world.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0F, 1.0F);
            }
        }
    }
}
