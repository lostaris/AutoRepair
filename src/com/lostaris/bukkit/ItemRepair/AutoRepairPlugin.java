package com.lostaris.bukkit.ItemRepair;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;
import org.bukkit.plugin.Plugin;

/**
 * Auto repair plugin for bukkit
 *
 * @author lostaris
 */
public class AutoRepairPlugin extends JavaPlugin {
	private final AutoRepairBlockListener blockListener = new AutoRepairBlockListener(this);
	private static HashMap<String, ArrayList<ItemStack>> repairRecipies; // item costs
	private static HashMap<String, Integer> iConCosts; // iConomy costs
	private HashMap<String, String> settings; // settings for the plugin
	private static boolean useiConomy;
	private static String isiCon; //are we using icon, both or not at all
	private static boolean autoRepair; // is there auto repairing
	private static boolean repairCosts; // is there repair costs
	public static PermissionHandler Permissions = null;
	public static boolean isPermissions = false;
	public static final Logger log = Logger.getLogger("Minecraft");
	public Repair repair = new Repair(this);

	public AutoRepairPlugin(PluginLoader pluginLoader, Server instance,
			PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);
		// TODO: Place any custom initialisation code here

		// NOTE: Event registration should be done in onEnable not here as all events are
		// unregistered when a plugin is disabled
	}

	public void onEnable() {
		//  Place any custom enable code here including the registration of any events

		// Register our events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.BLOCK_DAMAGED , blockListener, Priority.Monitor, this);

		// EXAMPLE: Custom code, here we just output some info so we can check all is well
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled" );
		setupPermissions();
		refreshConfig();
	}
	public void onDisable() {
		//  Place any custom disable code here

		// NOTE: All registered events are automatically unregistered when a plugin is disabled

		// EXAMPLE: Custom code, here we just output some info so we can check all is well
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled" );
	}

	/**
	 * Method to deal with player commands
	 */
	@Override
	public boolean onCommand(org.bukkit.command.CommandSender sender,
			org.bukkit.command.Command command, String commandLabel, String[] args) {
		Player player = null;
		// if the command sender is a player
		if(sender instanceof Player) {
			player = (Player) sender;
		}
		PlayerInventory inven = player.getInventory();
		String[] split = args;
		String commandName = command.getName().toLowerCase();
		AutoRepairSupport support = new AutoRepairSupport(this, player);

		// if the command is /repair
		if (commandName.equals("repair")) {
			// if the player is no allowed to use the command end noe
			if (!isAllowed(player, "access")) {
				return true;
			}

			ItemStack tool;
			int itemSlot = 0;
			// for /rep
			if (split.length == 0) {
				if (isAllowed(player, "repair")) {
					tool = player.getItemInHand();
					repair.manualRepair(tool, inven.getHeldItemSlot() );
				} else {
					player.sendMessage("§cYou dont have permission to do the repair command.");
				}
			// we have further arguments
			} else if (split.length == 1) {
				try {
					char repairList = split[0].charAt(0);
					// /rep ?
					if (repairList == '?') {						
						support.toolReq(player.getItemInHand());
					// /rep dmg
					} else if (split[0].equalsIgnoreCase("dmg")) {						
						support.durabilityLeft(inven.getItem(inven.getHeldItemSlot()));
					// /rep arm
					} else if (split[0].equalsIgnoreCase("arm")) {						
						repair.repairArmour();
					// /rep reload
					} else if(split[0].equalsIgnoreCase("reload")) {
						if (isAllowed(player, "reload")){ 
							refreshConfig();
							player.sendMessage("§3Re-loaded AutoRepair config files");
						} else {
							player.sendMessage("§cYou dont have permission to do the reload command.");
						}
					}else {
					// rep [itemslot]
						if (isAllowed(player, "repair")) {
							itemSlot = Integer.parseInt(split[0]);
							if (itemSlot >0 && itemSlot <=9) {
								tool = inven.getItem(itemSlot -1);
								repair.manualRepair(tool, itemSlot -1);
							} else {
								player.sendMessage("§6ERROR: Slot must be a quick bar slot between 1 and 9");
							}	
						} else {
							player.sendMessage("§cYou dont have permission to do the repair command.");
						}
					}
				} catch (Exception e) {
					return false;
				}
			// /rep arm ?
			}else if (split.length == 2 && split[0].equalsIgnoreCase("arm") && split[1].length() ==1) {
				if (isAllowed(player, "info")) { 
					support.repArmourInfo(split[1]);
				} else {
					player.sendMessage("§cYou dont have permission to do the ? or dmg commands.");
				}
			// /rep [itemslot] ?
			}else if ((split.length == 2 && split[1].length() ==1)) {
				try {
					char getRecipe = split[1].charAt(0);
					itemSlot = Integer.parseInt(split[0]);
					if (getRecipe == '?' && itemSlot >0 && itemSlot <=9) {
						if (isAllowed(player, "info")) {
							support.toolReq(inven.getItem(itemSlot-1) );
						} else {
							player.sendMessage("§cYou dont have permission to do the ? or dmg commands.");
						}
					}
				} catch (Exception e) {
					return false;
				}
			// /rep [itemslot] dmg
			} else if (split.length == 2 && split[1].equalsIgnoreCase("dmg")) {
				try {
					if (isAllowed(player, "info")) {
						itemSlot = Integer.parseInt(split[0]);
						if (itemSlot >0 && itemSlot <=9) {
							support.durabilityLeft(inven.getItem(itemSlot -1));
						} else {
							player.sendMessage("§6ERROR: Slot must be a quick bar slot between 1 and 9");
						}
					} else {
						player.sendMessage("§cYou dont have permission to do the ? or dmg commands.");
					}
				} catch (Exception e) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Method to find if a player is allowed to use this command
	 * @param player - player that used the command
	 * @param com - command to see if they can use
	 * @return true if they can use it
	 */
	public static boolean isAllowed(Player player, String com) {		
		boolean allowed = false;
		if(AutoRepairPlugin.Permissions != null) {
			if(AutoRepairPlugin.Permissions.has(player, "AutoRepair."+com)) {
				allowed = true;
			} else {
				allowed = false;
			}
		}else if(!isPermissions) {
			allowed = true;
		}
		return allowed;
	}

	/**
	 * Reads in the config file for this plugin
	 * @return the hashmap with the config values
	 */
	public HashMap<String, String> readConfig() {
		String fileName = "plugins/AutoRepair/Config.properties";
		HashMap<String, String> settings = new HashMap<String, String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line;
			String setting = null;
			String value = null;
			while ((line = reader.readLine()) != null) {
				if ((line.trim().length() == 0) || 
						(line.charAt(0) == '#')) {
					continue;
				}
				int keyPosition = line.indexOf('=');
				setting = line.substring(0, keyPosition).trim();
				value = line.substring(keyPosition +1, line.length());
				settings.put(setting, value);
			}			
		}catch (Exception e) {
			log.info("Error reading AutoRepair config");
		}		
		return settings;

	}

	/**
	 * Refreshes the config files for this plugin
	 */
	public void refreshConfig() {
		try {
			readProperties();
			setSettings(readConfig());
			if (getSettings().containsKey("auto-repair")) {
				if (getSettings().get("auto-repair").equals("true")) {
					setAutoRepair(true);
				} else if (getSettings().get("auto-repair").equals("false")) {
					setAutoRepair(false);
				}
			}
			if (getSettings().containsKey("repair-costs")) {
				if (getSettings().get("repair-costs").equals("true")) {
					setRepairCosts(true);
				} else if (getSettings().get("repair-costs").equals("false")) {
					setRepairCosts(false);
				}
			}
			if (getSettings().containsKey("iconomy")) {
				if (getSettings().get("iconomy").equals("true")) {
					setIsICon("true");
				} else if (getSettings().get("iconomy").equals("false")) {
					setIsICon("false");
				} else if (getSettings().get("iconomy").equals("both")) {
					setIsICon("both");
				}
			}
			if (getSettings().containsKey("permissions")) {
				if (getSettings().get("permissions").equals("false")) {
					AutoRepairPlugin.isPermissions = false;
				} if (getSettings().get("permissions").equals("true")) {
					Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");
					if(test != null) {
						AutoRepairPlugin.Permissions = ((Permissions)test).getHandler();
						AutoRepairPlugin.isPermissions = true;
					}
				}
			}
		} catch (Exception e){
			log.info("Error reading AutoRepair config files");
		}
	}

	/**
	 * Reads in the repair recipes for this plugin
	 * @throws Exception
	 */
	public static void readProperties() throws Exception {
		HashMap<String, ArrayList<ItemStack> > map = new HashMap<String, ArrayList<ItemStack> >();
		HashMap<String, Integer> iConomy = new HashMap<String, Integer>();
		String fileName = "plugins/AutoRepair/RepairCosts.properties";
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line;
		while ((line = reader.readLine()) != null) {
			if ((line.trim().length() == 0) || 
					(line.charAt(0) == '#')) {
				continue;
			}
			int keyPosition = line. indexOf('=');
			String[] reqs;
			ArrayList<ItemStack> itemReqs = new ArrayList<ItemStack>();
			String item = line.substring(0, keyPosition).trim();
			String recipiesString;
			// this line has an iConomy value
			if (line.indexOf(' ') != -1) {
				recipiesString = line.substring(keyPosition+1, line.indexOf(' '));
				try {
					int amount = Integer.parseInt(line.substring(line.lastIndexOf("=") +1, line.length()));
					iConomy.put(item, amount);
				} catch (Exception e) {
				}
			// this line doesnt have an iConomy value
			} else {
				recipiesString = line.substring(keyPosition+1, line.length()).trim();
			}
			// this line has more than one item cost
			String[] allReqs = recipiesString.split(":");		
			for (int i =0; i < allReqs.length; i++) {
				reqs = allReqs[i].split(",");
				ItemStack currItem = new ItemStack(Integer.parseInt(reqs[0]), Integer.parseInt(reqs[1]));
				itemReqs.add(currItem);
			}
			map.put(item, itemReqs);
		}
		reader.close();
		setiConCosts(iConomy);
		setRepairRecipies(map);
	}

	/**
	 * Sets up the permissions for this plugin if the permissions plugin is installed
	 */
	public void setupPermissions() {
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

		if(AutoRepairPlugin.Permissions == null) {
			if(test != null) {
				AutoRepairPlugin.Permissions = ((Permissions)test).getHandler();
				AutoRepairPlugin.isPermissions = true;
			} else {
				log.info("Permission system not enabled. AutoRepair plugin defaulting to everybody can use all commands");
			}
		}
	}
	
	/**
	 * Checks to see if the iConomy plugin is installed
	 * @return
	 */
	public boolean checkiConomy() {
		Plugin test = this.getServer().getPluginManager().getPlugin("iConomy");

		if (test != null) {
			if (getiSICon().compareToIgnoreCase("true") > 0 || getiSICon().compareToIgnoreCase("both") > 0) {
				AutoRepairPlugin.useiConomy = true;
			} else {
				AutoRepairPlugin.useiConomy = true;
			}

		} else {
			AutoRepairPlugin.useiConomy = false;
		}

		return useiConomy;
	}

	public void setIsICon(String b) {
		AutoRepairPlugin.isiCon = b;		
	}

	public static String getiSICon() {
		return AutoRepairPlugin.isiCon;
	}

	public static boolean getUseIcon() {
		return AutoRepairPlugin.useiConomy;
	}

	public static void setRepairRecipies(HashMap<String, ArrayList<ItemStack>> hashMap) {
		AutoRepairPlugin.repairRecipies = hashMap;
	}

	public static HashMap<String, ArrayList<ItemStack>> getRepairRecipies() {
		return repairRecipies;
	}

	public void setSettings(HashMap<String, String> settings) {
		this.settings = settings;
	}

	public HashMap<String, String> getSettings() {
		return settings;
	}

	public void setAutoRepair(boolean autoRepair) {
		AutoRepairPlugin.autoRepair = autoRepair;
	}

	public static boolean isAutoRepair() {
		return autoRepair;
	}

	public void setRepairCosts(boolean repairCosts) {
		AutoRepairPlugin.repairCosts = repairCosts;
	}

	public static boolean isRepairCosts() {
		return repairCosts;
	}

	public static void setiConCosts(HashMap<String, Integer> iConomy) {
		AutoRepairPlugin.iConCosts = iConomy;
	}

	public static HashMap<String, Integer> getiConCosts() {
		return iConCosts;
	}
}
