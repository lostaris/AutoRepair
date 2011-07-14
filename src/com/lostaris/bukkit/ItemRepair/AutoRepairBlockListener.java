package com.lostaris.bukkit.ItemRepair;

//import org.bukkit.Material;
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
	public AutoRepairSupport support;
	public Repair repair;

	public AutoRepairBlockListener(final AutoRepairPlugin plugin) {
		this.plugin = plugin;
		this.support = new AutoRepairSupport(plugin, null, null);
		this.repair = new Repair(plugin, null, null);
	}	

	//put all Block related code here
	public void onBlockDamage(BlockDamageEvent event) {
		Player player = event.getPlayer();
		//if the player is not allowed to auto repair finish
		if (!AutoRepairPlugin.isAllowed(player, "access")) {
			return;
		}
		// set the player we are doing this for
		support.setPlayer(player);
		repair.setPlayer(player);
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
		
		if ("tools".compareToIgnoreCase(support.itemType(toolHand)) ==0 ) {
			if (toolHand.getDurability() > (plugin.durability.get(toolHand.getTypeId()) -3)) {
				repair.autoRepairTool(toolSlot);
			} else if (toolHand.getDurability() > (plugin.durability.get(toolHand.getTypeId()) -10)) {
				support.repairWarn(toolHand, toolSlot);
			}
		}
	}



	public AutoRepairPlugin getPlugin() {
		return plugin;
	}
}
