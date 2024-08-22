package pl.polsatgranie.itomsd.banHammer;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BanHammer extends JavaPlugin implements CommandExecutor, Listener {

    private Map<String, ItemStack> guiItems;
    private Map<Player, Player> guiTargets;

    @Override
    public void onEnable() {
        new Metrics(this, 22933);
        this.getLogger().info("""

                ------------------------------------------------------------
                |                                                          |
                |      _  _______        __     __    _____   ____         |
                |     | ||___ ___|      |  \\   /  |  / ____| |  _ \\        |
                |     | |   | |   ___   | |\\\\ //| | | (___   | | \\ \\       |
                |     | |   | |  / _ \\  | | \\_/ | |  \\___ \\  | |  ) )      |
                |     | |   | | | (_) | | |     | |  ____) | | |_/ /       |
                |     |_|   |_|  \\___/  |_|     |_| |_____/  |____/        |
                |                                                          |
                |                                                          |
                ------------------------------------------------------------
                |                 +==================+                     |
                |                 |     BanHammer    |                     |
                |                 |------------------|                     |
                |                 |        1.0       |                     |
                |                 |------------------|                     |
                |                 |  PolsatGraniePL  |                     |
                |                 +==================+                     |
                ------------------------------------------------------------
                """);
        saveDefaultConfig();
        loadGuiItems();
        guiTargets = new HashMap<>();
        getCommand("banhammer").setExecutor(this);
        getCommand("banhammer").setAliases(java.util.List.of("bhammer", "bh", "hammer"));
        getCommand("bhreload").setExecutor(this);
        getCommand("bhreload").setAliases(java.util.List.of("banhammerreload"));
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadGuiItems() {
        guiItems = new HashMap<>();
        for (String key : getConfig().getConfigurationSection("gui").getKeys(false)) {
            String itemMaterial = getConfig().getString("gui." + key + ".item");
            String itemName = ChatColor.translateAlternateColorCodes('&', getConfig().getString("gui." + key + ".name"));
            String command = getConfig().getString("gui." + key + ".command");
            int slot = getConfig().getInt("gui." + key + ".slot");

            ItemStack item = new ItemStack(Material.matchMaterial(itemMaterial));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemName);
                ArrayList<String> lore = new ArrayList<>();
                for (String loreLine : getConfig().getStringList("gui." + key + ".lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', loreLine));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            guiItems.put(command, item);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("bhreload")) {
            if (sender.hasPermission("itomsd.banhammer")) {
                reloadConfig();
                loadGuiItems();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("plugin-reloaded")));
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-permission-message")));
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (sender.hasPermission("itomsd.banhammer")) {
            if (command.getName().equalsIgnoreCase("banhammer")) {
                if (args.length == 0) {
                    giveBanHammer(player);
                } else if (args.length == 1) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target != null) {
                        openGui(player, target);
                    } else {
                        player.sendMessage("Player not found!");
                    }
                } else {
                    player.sendMessage("Usage: /banhammer [player]");
                }
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-permission-message")));
            return true;
        }

        return false;
    }

    private void giveBanHammer(Player player) {
        ItemStack item = new ItemStack(Material.matchMaterial(getConfig().getString("item.id")));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("item.name")));
            ArrayList<String> lore = new ArrayList<>();
            for (String loreLine : getConfig().getStringList("item.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', loreLine));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("banhammer-give")));
    }

    private void openGui(Player player, Player target) {
        int guiSize = getConfig().getInt("gui-size", 9);
        Inventory gui = Bukkit.createInventory(null, guiSize, "BanHammer Menu - "+target.getName());
        guiTargets.put(player, target);

        for (String key : getConfig().getConfigurationSection("gui").getKeys(false)) {
            ItemStack item = guiItems.get(getConfig().getString("gui." + key + ".command"));
            int slot = getConfig().getInt("gui." + key + ".slot");
            if (slot >= 0 && slot < guiSize) {
                gui.setItem(slot, item);
            } else {
                gui.addItem(item);
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("BanHammer Menu")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                Player player = (Player) event.getWhoClicked();
                Player target = guiTargets.get(player);
                if (target != null) {
                    for (Map.Entry<String, ItemStack> entry : guiItems.entrySet()) {
                        if (clickedItem.isSimilar(entry.getValue())) {
                            String command = entry.getKey().replace("%player%", target.getName());
                            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
                            break;
                        }
                    }
                }
                guiTargets.remove(player);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player target = (Player) event.getEntity();
            ItemStack item = damager.getInventory().getItemInMainHand();
            if (item != null && item.getType() == Material.matchMaterial(getConfig().getString("item.id")) &&
                    item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', getConfig().getString("item.name")))) {
                event.setCancelled(true);
                openGui(damager, target);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof Player) {
            Player damager = event.getPlayer();
            Player target = (Player) event.getRightClicked();
            ItemStack item = damager.getInventory().getItemInMainHand();
            if (item != null && item.getType() == Material.matchMaterial(getConfig().getString("item.id")) &&
                    item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', getConfig().getString("item.name")))) {
                event.setCancelled(true);
                openGui(damager, target);
            }
        }
    }
}
