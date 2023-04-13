package com.strangeone101.airbubble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;

import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.TempBlock;

public class AirBubble extends AirAbility implements AddonAbility {

	@Attribute("Click" + Attribute.DURATION)
	private long clickDuration;
	@Attribute(Attribute.RADIUS)
	private double maxRadius;
	@Attribute(Attribute.SPEED)
	private double speed;
	private boolean requireAir = true;

	private boolean isShift;
	private double radius;
	private boolean removing = false; // Is true when the radius is shrinking.
	private final Map<Block, TempBlock> tempBlocks = new HashMap<>();
	private Location location;
	private long lastActivation; // When the last click happened.

	public AirBubble(final Player player, final boolean isShift) {
		super(player);

		this.setFields();

		if (CoreAbility.hasAbility(player, this.getClass())) {
			final AirBubble bubble = CoreAbility.getAbility(player, this.getClass());

			if (bubble.location.getWorld().equals(player.getWorld())) {
				if (bubble.location.distanceSquared(player.getLocation()) < this.maxRadius * this.maxRadius) {
					if (bubble.removing) {
						bubble.removing = false;
					}

					bubble.location = player.getLocation();
					bubble.isShift = isShift;
					bubble.lastActivation = System.currentTimeMillis();
					return;
				}
			}
			bubble.removing = true;
		} else if (this.requireAir && !(!player.getEyeLocation().getBlock().getType().isSolid() && !player.getEyeLocation().getBlock().isLiquid())) {
			return;
		}

		if (!this.bPlayer.canBend(this)) {
			return;
		}

		this.radius = 0;
		this.isShift = isShift;
		this.location = player.getLocation();
		this.lastActivation = System.currentTimeMillis();

		this.start();
	}

	public void setFields() {
		this.clickDuration = ConfigManager.defaultConfig.get().getLong("ExtraAbilities.StrangeOne101.AirBubble.ClickDuration");
		this.maxRadius = applyModifiers(ConfigManager.defaultConfig.get().getDouble("ExtraAbilities.StrangeOne101.AirBubble.Radius"));
		this.speed = ConfigManager.defaultConfig.get().getDouble("ExtraAbilities.StrangeOne101.AirBubble.Speed");
	}

	@Override
	public String getName() {
		return "AirBubble";
	}

	@Override
	public void progress() {
		if (!this.bPlayer.canBend(this) || (this.isShift && !this.player.isSneaking()) || !this.location.getWorld().equals(this.player.getWorld())) {
			this.removing = true;
		}

		if (System.currentTimeMillis() - this.lastActivation > this.clickDuration && !this.isShift) {
			this.removing = true;
		}

		if (this.removing) {
			this.radius -= this.speed;

			if (this.radius <= 0.1) {
				this.radius = 0.1;
				this.remove();
			}
		} else {
			this.radius += this.speed;

			if (this.radius > this.maxRadius) {
				this.radius = this.maxRadius;
			}
		}

		final List<Block> list = new ArrayList<Block>();

		if (this.radius < this.maxRadius || !this.location.getBlock().equals(this.player.getLocation().getBlock())) {

			for (double x = -this.radius; x < this.radius; x += 0.5) {
				for (double y = -this.radius; y < this.radius; y += 0.5) {
					for (double z = -this.radius; z < this.radius; z += 0.5) {
						if (x * x + y * y + z * z <= this.radius * this.radius) {
							final Block b = this.location.add(x, y, z).getBlock();

							if (!this.tempBlocks.containsKey(b)) {
								if (isWater(b)) {
									BlockData state = b.getBlockData().clone();
									if (state instanceof Waterlogged) {
										final Waterlogged logged = (Waterlogged) state;
										logged.setWaterlogged(false);
									} else if (isWater(b.getType())) {
										state = Material.AIR.createBlockData();
									}
									this.tempBlocks.put(b, new TempBlock(b, state, this));
								}
							}
							list.add(b); // Store it to say that it should be there.
							this.location.subtract(x, y, z);
						}
					}
				}
			}

			// Remove all blocks that shouldn't be there.
			final Set<Block> set = new HashSet<Block>(this.tempBlocks.keySet());
			list.forEach(set::remove);

			for (Block b : set) {
				tempBlocks.get(b).revertBlock();
				tempBlocks.remove(b);
			}
		}

		this.location = this.player.getLocation();
	}

	@Override
	public Location getLocation() {
		return this.location;
	}

	@Override
	public long getCooldown() {
		return 0;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public void remove() {
		super.remove();

		this.tempBlocks.values().forEach(TempBlock::revertBlock);

		this.tempBlocks.clear();
	}

	/**
	 * Returns whether the block provided is one of the air blocks used by
	 * AirBubble
	 *
	 * @param block The block being tested
	 * @return True if it's in use
	 */
	public static boolean isAir(final Block block) {
		for (final AirBubble bubble : CoreAbility.getAbilities(AirBubble.class)) {
			if (bubble.tempBlocks.containsKey(block)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void load() {
		ConfigManager.languageConfig.get().addDefault("Abilities.Air.AirBubble.Description", "AirBubble allows the bender to bring air pockets into water. This allows them to breath underwater while they hold it!");
		ConfigManager.languageConfig.get().addDefault("Abilities.Air.AirBubble.Instructions", "Hold sneak when above water to push the water back and create an air bubble. Alternatively, you can click to create a bubble for a short amount of time.");

		ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.StrangeOne101.AirBubble.Radius", 3.0);
		ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.StrangeOne101.AirBubble.Speed", 0.5);
		ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.StrangeOne101.AirBubble.ClickDuration", 2000L);

		ConfigManager.defaultConfig.save();
		ConfigManager.languageConfig.save();

		Bukkit.getPluginManager().registerEvents(new AddonListener(), ProjectKorra.plugin);
		ProjectKorra.log.info("AirBubble " + getVersion() + " by StrangeOne101 enabled!");
	}

	@Override
	public void stop() {

	}

	@Override
	public String getAuthor() {
		return "StrangeOne101";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}
