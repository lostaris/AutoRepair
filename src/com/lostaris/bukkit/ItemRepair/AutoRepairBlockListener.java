package com.lostaris.bukkit.ItemRepair;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Listens to block damage events and warns the player about almost broken tools
 * calls autoRepair on tools that are about to break
 * @author lostaris
 */
public class AutoRepairBlockListener extends BlockListener {
	private final AutoRepairPlugin plugin;
	private final int woodDurability = 59;
	private final int goldDurability = 32;
	private final int stoneDurability = 131;
	private final int ironDurability = 250;
	private final int diamondDurability = 1561;
	public AutoRepairSupport support;
	public Repair repair;

	public AutoRepairBlockListener(final AutoRepairPlugin plugin) {
		this.plugin = plugin;
		this.support = new AutoRepairSupport(plugin, null);
		this.repair = new Repair(plugin);
	}	

	//put all Block related code here
	public void onBlockDamage(BlockDamageEvent event) {
		Player player = event.getPlayer();
		//if the player is not allowed to auto repair finish
		if (!AutoRepairPlugin.isAllowed(player, "access")) {
			return;
		}
		// set the player we are doing this for
		this.support.setPlayer(player);
		ItemStack toolHand = player.getItemInHand();
		PlayerInventory inven = player.getInventory();
		int toolSlot = inven.getHeldItemSlot();
		Short dmg = toolHand.getDurability();
		// reseting the warn flags
		if (dmg ==1) {
			support.setWarning(false);
			support.setLastWarning(false);
		}

		/*  If a tool has less than 10 durability left warn the player if needed
		 *  else repair it
		 */

		if (toolHand.getType() == Material.WOOD_SPADE || toolHand.getType() == Material.WOOD_PICKAXE || 
				toolHand.getType() == Material.WOOD_AXE || toolHand.getType() == Material.WOOD_SWORD ||
				toolHand.getType() == Material.WOOD_HOE) {
			if (dmg > (woodDurability -3)) {
				repair.autoRepairTool(toolHand, toolSlot);
			} else if (dmg > (woodDurability -10)) {
				support.repairWarn(toolHand, toolSlot);
			}
		}
		if (toolHand.getType() == Material.GOLD_SPADE || toolHand.getType() == Material.GOLD_PICKAXE || 
				toolHand.getType() == Material.GOLD_AXE || toolHand.getType() == Material.GOLD_SWORD ||
				toolHand.getType() == Material.GOLD_HOE) {
			if (dmg > (goldDurability -3)) {					
				repair.autoRepairTool(toolHand, toolSlot);
			} else if (dmg > (goldDurability -10)) {
				support.repairWarn(toolHand, toolSlot);
			}
		}
		if (toolHand.getType() == Material.STONE_SPADE || toolHand.getType() == Material.STONE_PICKAXE || 
				toolHand.getType() == Material.STONE_AXE || toolHand.getType() == Material.STONE_SWORD ||
				toolHand.getType() == Material.STONE_HOE) {
			if (dmg > (stoneDurability -3)) {
				repair.autoRepairTool(toolHand, toolSlot);
			} else if (dmg > (stoneDurability -10)) {
				support.repairWarn(toolHand, toolSlot);
			}
		}
		if (toolHand.getType() == Material.IRON_SPADE || toolHand.getType() == Material.IRON_PICKAXE || 
				toolHand.getType() == Material.IRON_AXE || toolHand.getType() == Material.IRON_SWORD ||
				toolHand.getType() == Material.IRON_HOE) {
			if (dmg > (ironDurability -3)) {
				repair.autoRepairTool(toolHand, toolSlot);
			} else if (dmg > (ironDurability -10)) {
				support.repairWarn(toolHand, toolSlot);
			}
		}
		if (toolHand.getType() == Material.DIAMOND_SPADE || toolHand.getType() == Material.DIAMOND_PICKAXE || 
				toolHand.getType() == Material.DIAMOND_AXE || toolHand.getType() == Material.DIAMOND_SWORD ||
				toolHand.getType() == Material.DIAMOND_HOE) {
			if (dmg > (diamondDurability -3)) {
				repair.autoRepairTool(toolHand, toolSlot);
			} else if (dmg > (diamondDurability -10)) {
				support.repairWarn(toolHand, toolSlot);
			}
		}
	}



	public AutoRepairPlugin getPlugin() {
		return plugin;
	}
}
