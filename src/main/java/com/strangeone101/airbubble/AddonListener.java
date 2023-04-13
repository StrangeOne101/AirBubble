package com.strangeone101.airbubble;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.event.PlayerSwingEvent;
import com.projectkorra.projectkorra.waterbending.WaterManipulation;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class AddonListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (AirBubble.isAir(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockFlowTo(final BlockFromToEvent event) {
        final Block toblock = event.getToBlock();
        final Block fromblock = event.getBlock();

        if (ElementalAbility.isWater(fromblock)) {
            event.setCancelled(AirBubble.isAir(toblock));
            if (!event.isCancelled()) {
                event.setCancelled(!WaterManipulation.canFlowFromTo(fromblock, toblock));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerSneak(final PlayerToggleSneakEvent event) {
        final Player player = event.getPlayer();
        final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        final CoreAbility ability = bPlayer.getBoundAbility();

        if (ability != null && AirBubble.class.equals(ability.getClass())) {
            new AirBubble(player, true);
        }

    }

    @EventHandler
    public void onPlayerInteract(PlayerSwingEvent event) {
        final Player player = event.getPlayer();
        final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        final CoreAbility ability = bPlayer.getBoundAbility();

        if (ability != null && AirBubble.class.equals(ability.getClass())) {
            new AirBubble(player, false);
        }
    }
}
