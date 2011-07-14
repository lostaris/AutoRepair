package com.lostaris.bukkit.ItemRepair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.iConomy.iConomy;

public class Repair extends AutoRepairSupport{
	public static final Logger log = Logger.getLogger("Minecraft");

	public Repair(AutoRepairPlugin instance, Player player, ItemStack tool) {
		super(instance, player, tool);
	}

	/**
	 * Method to check affordability of a repair
	 * @return true if it can be, false if it cant
	 */
	public boolean canAfford() {
		// free repair
		if (!AutoRepairPlugin.isRepairCosts()) {
			return true;
		}	
		// Just items
		else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("false") == 0) {
			if(isEnough(tool.getType().toString())) {
				return true;
			}			
		}
		// just iCon
		else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0) {
			if (getPlugin().getiConCosts().containsKey(tool.getType().toString())) {
				double balance = getHolding(player).balance();
				double cost = costICon(tool);
				if (cost <= balance) {
					return true;
				}
			}			
		} 
		// both items and iCon
		else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
			HashMap<String, ArrayList<ItemStack> > recipies = plugin.getRepairRecipies();
			ArrayList<ItemStack> req = recipies.get(tool.getType().toString());
			if (getPlugin().getiConCosts().containsKey(tool.getType().toString())
					&& plugin.getRepairRecipies().containsKey(tool.getType().toString())) {
				double balance = getHolding(player).balance();
				double cost = costICon(tool);
				ArrayList<ItemStack> newReq = partialReq(req, tool);
				if (cost <= balance && isEnoughItems(newReq)) {
					return true;
				}
			}			
		}
		//this player cannot afford this repair
		return false;
	}

	public boolean repair(ItemStack tool) {
		if (canAfford()) {			
			super.setTool(tool);
			HashMap<String, ArrayList<ItemStack> > recipies = plugin.getRepairRecipies();
			String itemName = Material.getMaterial(tool.getTypeId()).toString();
			ArrayList<ItemStack> req = recipies.get(itemName);			
			
			if (!AutoRepairPlugin.isRepairCosts()) {
				getPlayer().sendMessage("§3Repaired " + printItem(tool));
				return true;
			} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("false") == 0) {
				ArrayList<ItemStack> newReq = partialReq(req, tool);
				if (!zeroItems(newReq)) {
					player.sendMessage("§3Using " + printFormatReqs(newReq) + "§3 to repair "  + printItem(tool));
				} else {
					getPlayer().sendMessage("§3Repaired " + printItem(tool));
				}
				deduct(newReq);
				return true;
			} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0) {
				double cost = costICon(tool);
				getHolding(player).subtract(cost);
				if (cost > 0.00) {
					player.sendMessage("§3Using §7" + iConomy.format(cost) + "§3 to repair " + printItem(tool));
				} else {
					getPlayer().sendMessage("§3Repaired " + printItem(tool));
				}
				return true;
			} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
				double cost = costICon(tool);
				ArrayList<ItemStack> newReq = partialReq(req, tool);
				getHolding(player).subtract(cost);
				deduct(newReq);
				if (cost > 0.00 && zeroItems(newReq)) {
					player.sendMessage("§3Using §7" + iConomy.format(cost) + "§3 and");
					player.sendMessage("§3" + printFormatReqs(newReq) + "§3 to repair "  + printItem(tool));
				} else {
					getPlayer().sendMessage("§3Repaired " + printItem(tool));
				}			
				return true;
			}
		}
		return false;
	}

	/**
	 * iterate through and repair all item in a players inventory
	 */
	public boolean repairAll(){
		if (!AutoRepairPlugin.isAllowed(getPlayer(), "repair.all")) {
			getPlayer().sendMessage("§cYou dont have permission to do the repair all command.");
			return false;
		}

		player.sendMessage("§3Repairing all items");
		//Get player inventory
		PlayerInventory inven = getPlayer().getInventory();
		ItemStack[] inventoryItems = inven.getContents();
		for(int i = 0; i < 9; i++){
			if (inventoryItems[i] != null) {
				if (super.getPlugin().durability.containsKey(inventoryItems[i].getTypeId())) {
					super.setTool(inven.getItem(i));
					if (repair(inven.getItem(i))) {
						inven.setItem(i, repItem(tool));
					} else {
						player.sendMessage("§cYou cannot afford to repair all the items in your quickbar");
						return false;
					}
				}
			}
		}
		repairArmour();

		for(int i = 9; i < 36; i++) {
			if (inventoryItems[i] != null) {
				if (super.getPlugin().durability.containsKey(inventoryItems[i].getTypeId())) {
					super.setTool(inven.getItem(i));
					if (repair(inven.getItem(i))) {
						inven.setItem(i, repItem(tool));
					} else {
						player.sendMessage("§cYou cannot afford to repair all the items in your backpack");
						return false;
					}
				}
			}
		}

		return true;
	}


	/**
	 * Method to manually repair a players tool
	 * @param slot - inventory slot this tool is in
	 * @return
	 */
	public boolean manualRepair(int slot) {
		PlayerInventory inven = getPlayer().getInventory();
		super.tool = inven.getItem(slot);
		//player.sendMessage("repair.manual." + itemType(tool));
		if (!AutoRepairPlugin.isAllowed(getPlayer(), ("repair.manual." + itemType(tool)))) {
			getPlayer().sendMessage("§cYou dont have permission to repair §2" + itemType(tool));
			return false;
		}		
		
		if (!super.getPlugin().getRepairRecipies().containsKey(tool.getType().name())) {
			getPlayer().sendMessage("§cThis item cannot be repaired.");
			return true;
		}

		if (repair(tool)) {
			inven.setItem(slot, repItem(tool));
			return true;
		} else {
			ArrayList<ItemStack> req = plugin.getRepairRecipies().get(tool.getType().toString());
			ArrayList<ItemStack> newReq = partialReq(req, tool);
			if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("false") == 0) {
				justItemsWarn(tool.getType().toString(), newReq);
			} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0) {
				iConWarn(tool.getType().toString(), costICon(tool));
			} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
				bothWarn(tool.getType().toString(), costICon(tool), newReq);
			}
			return false;
		}
	}

	/** Method dealing with automatic repairing of tools
	 * @param tool the item to repair
	 * @param slot the inventory slot this tool is in
	 */
	public boolean autoRepairTool(int slot) {
		PlayerInventory inven = getPlayer().getInventory();
		super.tool = inven.getItem(slot);
		if (!AutoRepairPlugin.isAllowed(player, ("repair.auto." + itemType(tool))) || !AutoRepairPlugin.isAutoRepair()) { 
			return false;
		}		
		
		if (repair(tool)) {
			inven.setItem(slot, repItem(tool));
			return true;
		} else {
			ArrayList<ItemStack> req = plugin.getRepairRecipies().get(tool.getType().toString());
			if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("false") == 0) {
				if (!getLastWarning()) {
					if (AutoRepairPlugin.isAllowed(player, "warn")) {
						justItemsWarn(tool.getType().name(), req);					
					}
					setLastWarning(true);							
				}
			} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0) {
				if (!getLastWarning()) {
					if (AutoRepairPlugin.isAllowed(player, "warn")) {
						iConWarn(tool.getType().name(), costICon(tool));					
					}
					setLastWarning(true);							
				}
			} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
				if (!getLastWarning()) {
					if (AutoRepairPlugin.isAllowed(player, "warn")) {
						bothWarn(tool.getType().name(), costICon(tool), req);					
					}
					setLastWarning(true);							
				}
			}
			return false;
		}
	}

	/** 
	 * Method to repair all the worn armour of a player
	 */
	public void repairArmour() {
		if (!AutoRepairPlugin.isAllowed(player, ("repair.manual.armour"))) {
			player.sendMessage("§cYou dont have permission to repair §2armour");
			return;
		}
		
		PlayerInventory inven = player.getInventory();
		if(inven.getBoots().getTypeId() == 0 && inven.getChestplate().getTypeId() == 0 &&
				inven.getHelmet().getTypeId() == 0 && inven.getLeggings().getTypeId() == 0) {
			player.sendMessage("§cYou are not wearing any armour");
			return;
		}
		
		if (inven.getHelmet().getType() != Material.AIR) {
			super.tool = inven.getHelmet();
			if (repair(tool)) {
				inven.setHelmet(repItem(inven.getHelmet()));
			} else {
				player.sendMessage("§cYou cannot afford to repair your " + printItem(inven.getHelmet()));
			}
		}
		if (inven.getChestplate().getType() != Material.AIR) {
			super.tool = inven.getChestplate();
			if (repair(tool)) {
				inven.setChestplate(repItem(inven.getChestplate()));
			} else {
				player.sendMessage("§cYou cannot afford to repair your " + printItem(inven.getHelmet()));
			}
		}
		if (inven.getLeggings().getType() != Material.AIR) {
			super.tool = inven.getLeggings();
			if (repair(tool)) {
				inven.setLeggings(repItem(inven.getLeggings()));
			} else {
				player.sendMessage("§cYou cannot afford to repair your " + printItem(inven.getHelmet()));
			}
		}
		if (inven.getBoots().getType() != Material.AIR) {
			super.tool = inven.getBoots();
			if (repair(tool)) {
				inven.setBoots(repItem(inven.getBoots()));
			} else {
				player.sendMessage("§cYou cannot afford to repair your " + printItem(inven.getHelmet()));
			}
		}
	}

	/**
	 * Deducts the items needed from a player to do a repair
	 */
	public void deduct(ArrayList<ItemStack> req) {
		PlayerInventory inven = player.getInventory();
		for (int i =0; i < req.size(); i++) {
			ItemStack currItem = new ItemStack(req.get(i).getTypeId(), req.get(i).getAmount());
			int neededAmount = req.get(i).getAmount();
			int smallestSlot = findSmallest(currItem);
			if (smallestSlot != -1) {
				// until we have removed all the required materials
				while (neededAmount > 0) {									
					smallestSlot = findSmallest(currItem);
					ItemStack smallestItem = inven.getItem(smallestSlot);
					if (neededAmount < smallestItem.getAmount()) {
						// got enough in smallest stack deal and done
						ItemStack newSize = new ItemStack(currItem.getType(), smallestItem.getAmount() - neededAmount);
						inven.setItem(smallestSlot, newSize);
						neededAmount = 0;										
					} else {
						// need to remove from more than one stack, deal and continue
						neededAmount -= smallestItem.getAmount();
						inven.clear(smallestSlot);
					}
				}
			}
		}
	}
}
