package com.lostaris.bukkit.ItemRepair;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import com.iConomy.*;
import com.iConomy.system.Account;
import com.iConomy.system.Holdings;

/**
 * Supplementary methods for AutoRepair
 * @author lostaris
 */
public class AutoRepairSupport {
	private final AutoRepairPlugin plugin;
	protected Player player;
	protected ItemStack tool;

	public AutoRepairSupport(AutoRepairPlugin instance, Player player, ItemStack tool) {
		this.plugin = instance;
		this.player = player;
		this.tool = tool;
	}
	// max durabilities for all tools 
	private final int woodDurability = 59;
	private final int goldDurability = 32;
	private final int stoneDurability = 131;
	private final int ironDurability = 250;
	private final int diamondDurability = 1561;
	private boolean warning = false;
	private boolean lastWarning = false;
	public static final Logger log = Logger.getLogger("Minecraft");


	/**
	 * Method to return to the player the required items and/or iConomy cash needed for a repair
	 * @param tool - tool to return the repair requirements for
	 */
	public void toolReq(ItemStack tool) {
		// if the player has permission to do this command
		if (AutoRepairPlugin.isAllowed(player, "info")) {
			String toolString = tool.getType().toString();
			//player.sendMessage(itemType(tool) + " " + printItem(tool));
			// if its not a tool stop here
			if (!AutoRepairPlugin.getRepairRecipies().containsKey(toolString)) {
				player.sendMessage("§6This item cannot be repaired.");
				return;
			}

			// just icon cost
			if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0) {
				if (getPlugin().getiConCosts().containsKey(toolString)) {
					player.sendMessage("§6For this " + printItem(tool) + " §6it costs:");
					player.sendMessage("§f" +  iConomy.format(costICon(tool))							
							+ "§6 to repair now");
					if (plugin.getRounding().compareToIgnoreCase("flat") != 0) {
						player.sendMessage("§f" + iConomy.format(getPlugin().getiConCosts().get(toolString))
								+ "§6 to repair the full durability");
					}
				}
				//both icon cost and item cost
			} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
				if (AutoRepairPlugin.getRepairRecipies().containsKey(toolString) &&
						getPlugin().getiConCosts().containsKey(toolString)) {

					if (plugin.getRounding().compareToIgnoreCase("flat") != 0) {

					}
					player.sendMessage("§6For this " + printItem(tool) + " §6to repair now you need: §f" +
							iConomy.format(costICon(tool)) + "§6 and");
					player.sendMessage("§6" + printFormatReqsCost(AutoRepairPlugin.getRepairRecipies().get(toolString), tool));
					if (plugin.getRounding().compareToIgnoreCase("flat") != 0) {
						player.sendMessage("§6To repair the full durability you need: §f" + iConomy.format(
								getPlugin().getiConCosts().get(toolString)));
						player.sendMessage("§6" + printFormatReqs(AutoRepairPlugin.getRepairRecipies().get(toolString)));
					}					
				}
				// just item cost
			} else if (AutoRepairPlugin.isRepairCosts()) {
				//tests to see if the config file has a repair reference to the item they wish to repair
				if (AutoRepairPlugin.getRepairRecipies().containsKey(toolString)) {
					player.sendMessage("§6For this " + printItem(tool) + " §6to repair now you need: ");					
					player.sendMessage("§6" + printFormatReqsCost(AutoRepairPlugin.getRepairRecipies().get(toolString), tool));
					if (plugin.getRounding().compareToIgnoreCase("flat") != 0) {
						player.sendMessage("§6To repair the full durability you need:");
						player.sendMessage("§6" + printFormatReqs(AutoRepairPlugin.getRepairRecipies().get(toolString)));
					}
					
				}
			} else {
				player.sendMessage("§3No materials needed to repair");
				//return true;
			}
		} else {
			player.sendMessage("§cYou dont have permission to do the ? or dmg commands.");
		}
	}

	public Account getAcount(Player player) {
		Account account = iConomy.getAccount(player.getName());
		return account;
	}

	public Holdings getHolding(Player player) {
		Holdings balance = iConomy.getAccount(player.getName()).getHoldings();
		return balance;
	}

	/**
	 * Method to warn a player their tool is close to breaking
	 * If they do not have the required items and/or cash to repair warns them,
	 * and lets them know they are missing required items and/or cash and prints what is needed
	 * @param tool - tool to warn the player about
	 * @param slot - slot this tool is in
	 */
	public void repairWarn(ItemStack tool, int slot) {
		// if the player has permission to do this command
		if (!AutoRepairPlugin.isAllowed(player, "warn")) { 
			return;
		}
		// if there is free repairing and it will auto repair we dont need to warn
		if (!AutoRepairPlugin.isRepairCosts() && AutoRepairPlugin.isAutoRepair()) {
			return;						
		}

		HashMap<String, ArrayList<ItemStack>> repairRecipies;
		// if they haven't already been warned
		if (!warning) {					
			warning = true;		
			try {				
				repairRecipies = AutoRepairPlugin.getRepairRecipies();
				String toolString = tool.getType().toString();
				//tests to see if the config file has a repair reference to the item they wish to repair
				if (repairRecipies.containsKey(toolString)) {
					// there is no repair costs and no auto repair
					if (!AutoRepairPlugin.isRepairCosts() && !AutoRepairPlugin.isAutoRepair()) {
						player.sendMessage("§6WARNING: " + printItem(tool) + " will break soon");
						/* if there is repair costs  and no auto repair */
					} else if (AutoRepairPlugin.isRepairCosts() && !AutoRepairPlugin.isAutoRepair()) {
						double balance;
						// just iCon
						if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0){
							Double cost = getPlugin().getiConCosts().get(toolString);
							balance = getHolding(player).balance();
							player.sendMessage("§6WARNING: " + printItem(tool) + " will break soon, no auto repairing");
							if (cost > balance) {
								iConWarn(toolString, cost);
							}
							// both iCon and item cost
						} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
							Double cost = getPlugin().getiConCosts().get(toolString);
							balance = getHolding(player).balance();
							ArrayList<ItemStack> reqItems = AutoRepairPlugin.getRepairRecipies().get(toolString);
							player.sendMessage("§6WARNING: " + printItem(tool) + " will break soon, no auto repairing");
							if (cost > balance || !isEnoughItems(reqItems)) {
								bothWarn(toolString, cost, reqItems);
							}
							// just item cost
						} else {
							ArrayList<ItemStack> reqItems = AutoRepairPlugin.getRepairRecipies().get(toolString);
							player.sendMessage("§6WARNING: " + printItem(tool) + " will break soon, no auto repairing");
							if (!isEnoughItems(reqItems)) {
								justItemsWarn(toolString, reqItems);
							}
						}
						/* there is auto repairing and repair costs */
					} else {
						double balance;
						// just iCon
						if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0){
							Double cost = getPlugin().getiConCosts().get(toolString);
							balance = getHolding(player).balance();
							if (cost > balance) {
								player.sendMessage("§6WARNING: " + printItem(tool) + " will break soon");
								iConWarn(toolString, cost);
							}
							// both iCon and item cost
						} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
							Double cost = getPlugin().getiConCosts().get(toolString);
							ArrayList<ItemStack> reqItems = AutoRepairPlugin.getRepairRecipies().get(toolString);
							balance = getHolding(player).balance();
							if (cost > balance || !isEnoughItems(reqItems)) {
								player.sendMessage("§6WARNING: " + printItem(tool) + " will break soon");
								bothWarn(toolString, cost, reqItems);
							}
							// just item cost
						} else {
							ArrayList<ItemStack> reqItems = AutoRepairPlugin.getRepairRecipies().get(toolString);
							if (!isEnoughItems(reqItems)) {								
								player.sendMessage("§6WARNING: " + printItem(tool) + " will break soon");
								justItemsWarn(toolString, reqItems);
							}
						}
					}
				} else {
					// item does not have a repair reference in config
					player.sendMessage("§6" +toolString + " not found in config file.");
				}
			} catch (Exception e) {
				log.info("Error in AutoRepair config.properties file syntax");
			}
		}
	}

	/**
	 * Method to return the total cost of repair a players worn armour
	 * @param query
	 * @return true if all is well, false if the command is miss typed
	 */
	public boolean repArmourInfo(String query) {
		// if there is repair costs
		if (AutoRepairPlugin.isRepairCosts()) {
			try {
				char getRecipe = query.charAt(0);
				// if the command is ? - the correct one
				if (getRecipe == '?') {
					double total =0;
					ArrayList<ItemStack> req = repArmourAmount();
					PlayerInventory inven = player.getInventory();
					// just iCon costs
					if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("true") == 0){
						for (ItemStack i : inven.getArmorContents()) {				
							if (getPlugin().getiConCosts().containsKey(i.getType().toString())) {
								total += costICon(i);
							}				
						}
						player.sendMessage("§6To repair all §2your armour §6you need: §f"
								+ iConomy.format(total));						
						//both icon and item cost
					} else if (AutoRepairPlugin.getiSICon().compareToIgnoreCase("both") == 0) {
						for (ItemStack i : inven.getArmorContents()) {				
							if (getPlugin().getiConCosts().containsKey(i.getType().toString())) {
								total += costICon(i);
							}
						}
						player.sendMessage("§6To repair all §2your armour §6you need: §f"
								+ iConomy.format(total));
						player.sendMessage("§6" + printFormatReqs(req));		
						// just item cost
					} else {
						player.sendMessage("§6To repair all §2your armour §6you need:");
						player.sendMessage("§6" + printFormatReqs(req));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		} else {
			player.sendMessage("§3No materials needed to repair");
		}
		return true;
	}

	/**
	 * Method to return the total item cost of repairing a players worn armour
	 * @return req - total item costs of repairing a players warn armour
	 */
	public ArrayList<ItemStack> repArmourAmount() {
		HashMap<String, ArrayList<ItemStack> > recipies = AutoRepairPlugin.getRepairRecipies();
		PlayerInventory inven = player.getInventory();
		ItemStack[] armour = inven.getArmorContents();
		// list of all the items needed to repair all warn armour
		HashMap<String, Integer> totalCost = new HashMap<String, Integer>();
		// for the players 4 armour slots
		for (int i=0; i<armour.length; i++) {
			String item = armour[i].getType().toString();
			if (recipies.containsKey(item)) {
				ArrayList<ItemStack> reqItems = recipies.get(item);
				// get this armour piece's costs
				for (int j =0; j<reqItems.size(); j++) {
					// if we already have this cost, add to it
					if(totalCost.containsKey(reqItems.get(j).getType().toString())) {
						int amount = totalCost.get(reqItems.get(j).getType().toString());
						totalCost.remove(reqItems.get(j).getType().toString());
						int newAmount = amount + reqItems.get(j).getAmount();
						totalCost.put(reqItems.get(j).getType().toString(), newAmount);
						// otherwise add it to the list
					} else {
						totalCost.put(reqItems.get(j).getType().toString(), reqItems.get(j).getAmount());
					}
				}
			}
		}
		// turn it back into a ItemStack array
		ArrayList<ItemStack> req = new ArrayList<ItemStack>();
		for (Object key: totalCost.keySet()) {
			int cost = (int) (totalCost.get(key) * armPercentDmg());
			req.add(new ItemStack(Material.getMaterial(key.toString()), cost));
		}
		return req;
	}

	// sets the durability of a item back to no damage
	public ItemStack repItem(ItemStack item) {
		item.setDurability((short) 0);
		return item;
	}

	//prints the durability left of the current tool to the player
	public void durabilityLeft(ItemStack tool) {
		if (AutoRepairPlugin.isAllowed(player, "info")) {
			int usesLeft = this.returnUsesLeft(tool);
			if (usesLeft != -1) {
				player.sendMessage("§3" + usesLeft + " blocks left untill this " + printItem(tool) + " §3breaks.");
			} else {
				player.sendMessage("§6This is not a tool.");
			}
		} else {
			player.sendMessage("§cYou dont have permission to do the ? or dmg commands.");
		}
	}

	/**
	 * Method to return the number of uses left in this tool
	 * @param tool - tool to return uses left for
	 * @return uses left of this tool
	 */
	public int returnUsesLeft(ItemStack tool) {
		int usesLeft = -1;
		if (tool.getType() == Material.WOOD_SPADE || tool.getType() == Material.WOOD_PICKAXE || 
				tool.getType() == Material.WOOD_AXE || tool.getType() == Material.WOOD_SWORD ||
				tool.getType() == Material.WOOD_HOE) {
			usesLeft = woodDurability - tool.getDurability();
		}
		if (tool.getType() == Material.GOLD_SPADE || tool.getType() == Material.GOLD_PICKAXE || 
				tool.getType() == Material.GOLD_AXE || tool.getType() == Material.GOLD_SWORD ||
				tool.getType() == Material.GOLD_HOE) {
			usesLeft = goldDurability - tool.getDurability();
		}
		if (tool.getType() == Material.STONE_SPADE || tool.getType() == Material.STONE_PICKAXE || 
				tool.getType() == Material.STONE_AXE || tool.getType() == Material.STONE_SWORD ||
				tool.getType() == Material.STONE_HOE) {
			usesLeft = stoneDurability - tool.getDurability();
		}
		if (tool.getType() == Material.IRON_SPADE || tool.getType() == Material.IRON_PICKAXE || 
				tool.getType() == Material.IRON_AXE || tool.getType() == Material.IRON_SWORD ||
				tool.getType() == Material.IRON_HOE) {
			usesLeft = ironDurability - tool.getDurability();

		}
		if (tool.getType() == Material.DIAMOND_SPADE || tool.getType() == Material.DIAMOND_PICKAXE || 
				tool.getType() == Material.DIAMOND_AXE || tool.getType() == Material.DIAMOND_SWORD ||
				tool.getType() == Material.DIAMOND_HOE) {
			usesLeft = diamondDurability - tool.getDurability();
		}
		return usesLeft;
	}

	/**
	 * Finds the smallest stack of an item in a players inventory
	 * @param item - item to look for
	 * @return slot the smallest stack is in
	 */
	public int findSmallest(ItemStack item) {
		PlayerInventory inven = player.getInventory();
		HashMap<Integer, ? extends ItemStack> items = inven.all(item.getTypeId());
		int slot = -1;
		int smallest = 64;
		for (Entry<Integer, ? extends ItemStack> entry : items.entrySet()) {
			if (entry.getValue().getAmount() <= smallest) {
				smallest = entry.getValue().getAmount();
				slot = entry.getKey();
			}
		}		
		return slot;
	}

	/**
	 * Method to return the total amount of an item a player has
	 * @param item - item to look for
	 * @return total number of this item the player has
	 */
	@SuppressWarnings("unchecked")
	public int getTotalItems(ItemStack item) {
		int total = 0;
		PlayerInventory inven = player.getInventory();
		HashMap<Integer, ? extends ItemStack> items = inven.all(item.getTypeId());
		//iterator for the hashmap
		Set<?> set = items.entrySet();
		Iterator<?> i = set.iterator();
		while(i.hasNext()){
			Map.Entry me = (Map.Entry)i.next();
			ItemStack item1 = (ItemStack) me.getValue();
			//if the player has doesn't not have enough of the item used to repair
			total += item1.getAmount();					
		}
		return total;
	}

	// checks to see if the player has enough of an item
	public boolean isEnough(String itemName) {
		ArrayList<ItemStack> reqItems = AutoRepairPlugin.getRepairRecipies().get(itemName);
		boolean enoughItemFlag = true;
		ArrayList<ItemStack> newReq = partialReq(reqItems, tool);
		for (int i =0; i < newReq.size(); i++) {
			ItemStack currItem = new ItemStack(newReq.get(i).getTypeId(), newReq.get(i).getAmount());

			int neededAmount = newReq.get(i).getAmount();
			int currTotal = getTotalItems(currItem);
			if (neededAmount > currTotal && neededAmount != 0) {
				enoughItemFlag = false;
			}
		}
		return enoughItemFlag;
	}

	public double armPercentDmg() {
		PlayerInventory inven = player.getInventory();
		ItemStack[] armour = inven.getArmorContents();
		double totalDmg = 0;
		double totalDurability = 0;
		for (ItemStack i : armour) {
			if (i.getType() != Material.AIR) {
				totalDmg += i.getDurability();
				totalDurability += plugin.durability.get(i.getTypeId());
			}			
		}
		double percent = (totalDmg / totalDurability);
		return percent;
	}

	public double percentDmg(ItemStack tool) {
		double percentage = 0.0;
		if (plugin.durability.containsKey(tool.getTypeId())) {
			double maxDur = plugin.durability.get(tool.getTypeId());
			double currentDur = tool.getDurability();
			percentage = (currentDur / maxDur);
		}
		return percentage;
	}

	public int costItem(ItemStack tool, ItemStack reqItem){
		int repCost = 0;
		double doubleCost = reqItem.getAmount() * percentDmg(tool);
		int cost = (int) (reqItem.getAmount() * percentDmg(tool));
		double fraction = doubleCost - cost;
		if (fraction >= 0.5) {
			//return (int) Math.ceil(doubleCost);
			repCost = (int) Math.ceil(doubleCost);
		} else {
			//return (int) Math.floor(doubleCost);
			repCost = (int) Math.floor(doubleCost);
		}
		if (plugin.getRounding().compareToIgnoreCase("min") == 0) {
			if (repCost == 0) {
				repCost = 1;
			}
		}
		if (plugin.getRounding().compareToIgnoreCase("flat") == 0) {
			repCost = reqItem.getAmount();
		}
		return repCost;
	}

	public double costICon(ItemStack tool) {
		if (plugin.getRounding().compareToIgnoreCase("flat") == 0) {
			return getPlugin().getiConCosts().get(tool.getType().toString());
		}
		double doubleCost = (getPlugin().getiConCosts().get(tool.getType().toString()) * percentDmg(tool));
		return roundTwoDecimals(doubleCost);
	}

	double roundTwoDecimals(double d) {
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		return Double.valueOf(twoDForm.format(d));
	}

	public ArrayList<ItemStack> partialReq(ArrayList<ItemStack> req, ItemStack tool) {
		ArrayList<ItemStack> newReq = new ArrayList<ItemStack>();
		if (plugin.getRounding().compareToIgnoreCase("flat") == 0) {
			return req;
		}
		for (ItemStack i : req) {
			newReq.add(new ItemStack (i.getTypeId(), (costItem(tool, i))));
		}
		return newReq;
	}

	// checks to see if the player has enough of a list of items
	public boolean isEnoughItems (ArrayList<ItemStack> req) {
		if (req == null) {
			return false;
		}

		for (int i =0; i<req.size(); i++) {
			ItemStack currItem = new ItemStack(req.get(i).getTypeId(), req.get(i).getAmount());
			int neededAmount = req.get(i).getAmount();
			int currTotal = getTotalItems(currItem);
			if (neededAmount > currTotal && neededAmount != 0) {
				return false;
			}
		}
		return true;
	}

	public String itemType(ItemStack tool) {
		if (getPlugin().durability.containsKey(tool.getTypeId())) {
			if(tool.getTypeId() == 259) {
				return "flint";				
			} else if(tool.getTypeId() == 346) {
				return "rod";
			} else if(tool.getTypeId() == 267 || tool.getTypeId() == 268 || tool.getTypeId() == 272
					|| tool.getTypeId() == 276|| tool.getTypeId() == 283) {
				return "sword";
			} else if (tool.getTypeId() >= 298 && tool.getTypeId() <= 317) {
				return "armour";
			} else {
				return "tools";
			}
		}
		return "";
	}

	public String printItem(ItemStack tool) {
		String name = "§2" + tool.getType().toString();
		name = name.toLowerCase();
		name = name.replace('_', ' ');
		return name;
	}

	/**
	 * Methods to print the warning for lacking items and/or iConomy money
	 */
	public void iConWarn(String itemName, double cost) {
		getPlayer().sendMessage("§cYou cannot afford to repair "  + printItem(tool));
		getPlayer().sendMessage("§cNeed: §f" + iConomy.format(cost));
	}

	public void bothWarn(String itemName, double cost, ArrayList<ItemStack> req) {
		getPlayer().sendMessage("§cYou are missing one or more items to repair " + printItem(tool));
		getPlayer().sendMessage("§cNeed: " + printFormatReqs(req) + " and §f" +
				iConomy.format(cost));
	}

	public void justItemsWarn(String itemName, ArrayList<ItemStack> req) {
		player.sendMessage("§cYou are missing one or more items to repair " + printItem(tool));
		player.sendMessage("§cNeed: " + printFormatReqs(req));
	}

	public String printFormatReqs(ArrayList<ItemStack> items) {
		StringBuffer string = new StringBuffer();
		string.append(" ");
		for (int i = 0; i < items.size(); i++) {
			string.append("§f" + items.get(i).getAmount() + " " + printItem(items.get(i)) + " ");
		}
		return string.toString();
	}

	public String printFormatReqsCost(ArrayList<ItemStack> items, ItemStack tool) {
		StringBuffer string = new StringBuffer();
		string.append(" ");
		for (int i = 0; i < items.size(); i++) {
			string.append("§f" + costItem(tool, items.get(i)) + " " + printItem(items.get(i)) + " ");
		}
		return string.toString();
	}

	public boolean zeroItems(ArrayList<ItemStack> req) {
		boolean zero = false;
		for (ItemStack i : req) {
			if (i.getAmount() == 0) {
				zero = true;
			} else {
				zero = false;
			}
		}
		return zero;
	}

	public boolean getWarning() {
		return warning;
	}

	public boolean getLastWarning() {
		return lastWarning;
	}

	public void setWarning(boolean newValue) {
		this.warning = newValue;
	}

	public void setLastWarning(boolean newValue) {
		this.lastWarning = newValue;
	}

	public AutoRepairPlugin getPlugin() {
		return plugin;
	}

	public Player getPlayer() {
		return player;
	}
	public void setPlayer(Player player) {
		this.player = player;
	}
	public ItemStack getTool() {
		return tool;
	}
	public void setTool(ItemStack tool) {
		this.tool = tool;
	}
}
