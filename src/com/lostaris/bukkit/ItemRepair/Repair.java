package com.lostaris.bukkit.ItemRepair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.iConomy.iConomy;

public class Repair extends AutoRepairSupport{
	public static final Logger log = Logger.getLogger("Minecraft");

	public Repair(AutoRepairPlugin instance) {
		super(instance, getPlayer());
	}
	
	/**
	 * Method to check affordability of a repair
	 * @return true if it can be, false if it cant
	 */
	public boolean canAfford(ItemStack tool) {
		// Just items
		if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("false") == 0) {
			if(isEnough(tool.toString())) {
				return true;
			}
			
		// just iCon
		} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0) {
			if (AutoRepairPlugin.getiConCosts().containsKey(tool.toString())) {
				double balance = getHolding(player).balance();
				int cost = costICon(tool);
				if (cost <= balance) {
					return true;
				}
			}
		
		// both items and iCon
		} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
			HashMap<String, ArrayList<ItemStack> > recipies = AutoRepairPlugin.getRepairRecipies();
			ArrayList<ItemStack> req = recipies.get(tool.toString());
			if (AutoRepairPlugin.getiConCosts().containsKey(tool.toString())
					&& AutoRepairPlugin.getRepairRecipies().containsKey(tool.toString())) {
				double balance = getHolding(player).balance();
				int cost = costICon(tool);
				ArrayList<ItemStack> newReq = partialReq(req, tool);
				if (cost <= balance && isEnoughItems(newReq)) {
					return true;
				}
			}			
		}
		//this player cannot afford this repair
		return false;
	}

	/**
	 * iterate throught
	 */
	public boolean repairAll(){
		if (!AutoRepairPlugin.isAllowed(getPlayer(), "repairAll")) {
			getPlayer().sendMessage("�cYou dont have permission to do the repairAll command.");
			return false;
		}
		
		//Get player inventory
		PlayerInventory inventory = getPlayer().getInventory();
		ItemStack[] inventoryItems = inventory.getContents();
		
		for( int i = 0; i < 9; i++){
			repair(inventoryItems[i], i);
		}
		
		
		return true;
	}
	/**
	 * Method to manually repair a players tool
	 * @param tool - tool to repair
	 * @param slot - inventory slot this tool is in
	 * @return
	 */
	public boolean manualRepair(ItemStack tool, int slot) {
		if (!AutoRepairPlugin.isAllowed(getPlayer(), "repair")) {
			getPlayer().sendMessage("�cYou dont have permission to do the repair command.");
			return false;
		}

		PlayerInventory inven = getPlayer().getInventory();
		HashMap<String, ArrayList<ItemStack> > recipies = AutoRepairPlugin.getRepairRecipies();
		String itemName = Material.getMaterial(tool.getTypeId()).toString();
		ArrayList<ItemStack> req = recipies.get(itemName);
		String toolString = tool.getType().toString();
		double balance;

		if (!AutoRepairPlugin.isRepairCosts()) {
			getPlayer().sendMessage("�3Repaired " + itemName);
			inven.setItem(slot, repItem(tool));
			// icon cost only
		} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0){
			if (AutoRepairPlugin.getiConCosts().containsKey(toolString)) {
				balance = getHolding(player).balance();
				//int cost = AutoRepairPlugin.getiConCosts().get(itemName);
				int cost = costICon(tool);
				if (cost <= balance) {
					getHolding(player).subtract(cost);
					player.sendMessage("�3Using " + iConomy.format(cost) + " to repair " + itemName);
					inven.setItem(slot, repItem(tool));
				} else {
					iConWarn(itemName, cost);
				}
			} else {
				player.sendMessage("�cThis is not a tool");
			}
			//both icon and item cost
		} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
			if (AutoRepairPlugin.getiConCosts().containsKey(toolString)
					&& AutoRepairPlugin.getRepairRecipies().containsKey(toolString)) {
				balance = getHolding(player).balance();
				//int cost = AutoRepairPlugin.getiConCosts().get(itemName);
				int cost = costICon(tool);
				ArrayList<ItemStack> newReq = partialReq(req, tool);
				//if (cost <= balance && isEnoughItems(req)) {
				if (cost <= balance && isEnoughItems(newReq)) {
					getHolding(player).subtract(cost);
					deduct(newReq);
					player.sendMessage("�3Using " + iConomy.format(cost) + " and");
					player.sendMessage("�3" + printFormatReqs(newReq) + " to repair "  + itemName);
					inven.setItem(slot, repItem(tool));
				} else {
					bothWarn(itemName, cost, newReq);
				}
			} else {
				player.sendMessage("�cThis is not a tool");
			}
			// just item cost
		} else {
			if (AutoRepairPlugin.getRepairRecipies().containsKey(toolString)) {
				ArrayList<ItemStack> newReq = partialReq(req, tool);
				//if (isEnoughItems(req)) {
				if (isEnoughItems(newReq)) {
					deduct(newReq);
					player.sendMessage("�3Using " + printFormatReqs(newReq) + " to repair "  + itemName);
					inven.setItem(slot, repItem(tool));
				} else {
					justItemsWarn(itemName, newReq);
				}
			} else {
				player.sendMessage("�cThis is not a tool");
			}
		}
		return false;		
	}

	/** Method dealing with automatic repairing of tools
	 * @param tool the item to repair
	 * @param slot the inventory slot this tool is in
	 */
	public boolean autoRepairTool(ItemStack tool, int slot) {
		if (!AutoRepairPlugin.isAllowed(player, "auto") || !AutoRepairPlugin.isAutoRepair()) { 
			return false;
		}

		PlayerInventory inven = player.getInventory();
		HashMap<String, ArrayList<ItemStack> > recipies = AutoRepairPlugin.getRepairRecipies();
		String itemName = Material.getMaterial(tool.getTypeId()).toString();
		ArrayList<ItemStack> req = recipies.get(itemName);		
		double balance;
		//Account account;

		// free repairing
		if (!AutoRepairPlugin.isRepairCosts()) {
			player.sendMessage("�3Repaired " + itemName);
			inven.setItem(slot, repItem(tool));
			// icon cost only
		} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0){
			balance = getHolding(player).balance();
			int cost = AutoRepairPlugin.getiConCosts().get(itemName);
			
			// the user can afford to repair
			if (cost <= balance) {
				getHolding(player).subtract(cost);
				player.sendMessage("�3Using " + iConomy.format(cost) + " to repair " + itemName);
				inven.setItem(slot, repItem(tool));
			// the user cannot afford to repair
			} else {
				if (!getLastWarning()) {
					if (AutoRepairPlugin.isAllowed(player, "warn")) {
						iConWarn(itemName, cost);					
					}
					setLastWarning(true);							
				}
			}
			//both icon and item cost
		} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
			balance = getHolding(player).balance();
			int cost = AutoRepairPlugin.getiConCosts().get(itemName);
			// the user can afford to repair
			if (cost <= balance && isEnoughItems(req)) {
				getHolding(player).subtract(cost);
				deduct(req);
				player.sendMessage("�3Using " + iConomy.format(cost) + " and");
				player.sendMessage("�3" + printFormatReqs(req) + " to repair "  + itemName);
				inven.setItem(slot, repItem(tool));
			// the user cannot afford to repair
			} else {
				if (!getLastWarning()) {
					if (AutoRepairPlugin.isAllowed(player, "warn")) {
						bothWarn(itemName, cost, req);					
					}
					setLastWarning(true);							
				}

			}			
			// just item cost
		} else {
			// the user can afford to repair
			if (isEnoughItems(req)) {
				deduct(req);
				player.sendMessage("�3Using " + printFormatReqs(req) + " to repair " + itemName);
				inven.setItem(slot, repItem(tool));
			} else {
				// the user cannot afford to repair
				if (!getLastWarning()) {
					if (AutoRepairPlugin.isAllowed(player, "warn")) {
						justItemsWarn(itemName, req);					
					}
					setLastWarning(true);							
				}
			}
		}
		return false;
	}

	/** 
	 * Method to repair all the worn armour of a player
	 */
	public void repairArmour() {
		if (!AutoRepairPlugin.isAllowed(player, "repair")) {
			player.sendMessage("�cYou dont have permission to do the repair command.");
			return;
		}

		PlayerInventory inven = player.getInventory();
		ArrayList<ItemStack> req = repArmourAmount();
		int total =0;
		double balance;

		if (!AutoRepairPlugin.isRepairCosts()) {
			player.sendMessage("�3Repaired your armour");
			repArm();
			// icon cost only
		} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0){
			balance = getHolding(player).balance();

			for (ItemStack i : inven.getArmorContents()) {				
				if (AutoRepairPlugin.getiConCosts().containsKey(i.getType().toString())) {
					//total += AutoRepairPlugin.getiConCosts().get(i.getType().toString());
					total += costICon(i);
				}				
			}
			if (total <= balance) {
				getHolding(player).subtract(total); //iConomy.db.set_balance(player.getName(), balance - total);
				player.sendMessage("�3Using " + iConomy.format(total) + " to repair your armour");
				repArm();
			} else {
				player.sendMessage("�cYou are cannot afford to repair your armour");
				player.sendMessage("�cNeed: " + iConomy.format(total));
			}
			//both icon and item cost
		} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
			balance = getHolding(player).balance();
			
			for (ItemStack i : inven.getArmorContents()) {				
				if (AutoRepairPlugin.getiConCosts().containsKey(i.getType().toString())) {
					//total += AutoRepairPlugin.getiConCosts().get(i.getType().toString());
					total += costICon(i);
				}				
			}						
			if (total <= balance && isEnoughItems(req)) {
				getHolding(player).subtract(total); //iConomy.db.set_balance(player.getName(), balance - total);
				deduct(req);
				player.sendMessage("�3Using " + iConomy.format(total) + " and");
				player.sendMessage("�3" + printFormatReqs(req) + " to repair your armour");
				repArm();
			} else {
				player.sendMessage("�cYou are missing one or more items to repair your armour");
				player.sendMessage("�cNeed: " + printFormatReqs(req) + " and " + iConomy.format(total));
			}			
			// just item cost
		} else {
			if (isEnoughItems(req)) {
				deduct(req);
				player.sendMessage("�3Using " + printFormatReqs(req) + " to repair your armour");
				repArm();
			} else {
				player.sendMessage("�cYou are missing one or more items to repair your armour");
				player.sendMessage("�cNeed: " + printFormatReqs(req));
			}
		}
	}

	/* 
	 * Method to do the actual repairing of a players armour
	 */
	public void repArm () {
		PlayerInventory inven = player.getInventory();
		if(inven.getBoots().getTypeId() != 0 ) {inven.setBoots(repItem(inven.getBoots()));}
		if(inven.getChestplate().getTypeId() != 0 ) {inven.setChestplate(repItem(inven.getChestplate()));}
		if(inven.getHelmet().getTypeId() != 0 ) {inven.setHelmet(repItem(inven.getHelmet()));}
		if(inven.getLeggings().getTypeId() != 0 ) {inven.setLeggings(repItem(inven.getLeggings()));}
	}

	/*
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
