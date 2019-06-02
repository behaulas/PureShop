package com.behaulas.pureshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopManager implements CommandExecutor, Listener {

    public Map<Player, Shop> playersInShops = new HashMap<>();

    ConfigFile shopConfig;

    public Map<UUID, Shop> shops = new HashMap<>();
    public List<Player> playersFiltering = new ArrayList<>();
    public ShopManager() {
        shopConfig = new ConfigFile("shop.yml");

        LoadConfig();


    }

    public void EmptyShop(Shop shop, Player player) {
        for (ShopItem item :
                shop.items) {
            HashMap<Integer, ItemStack> a = player.getInventory().addItem(item.item);

            if(!a.isEmpty()) {
                for (ItemStack s :
                        a.values()) {
                    player.getWorld().dropItem(player.getLocation(),s);
                }
            }
        }

        shop.items.clear();

        shopConfig.config.set(player.getUniqueId().toString() + ".items", new ArrayList<ShopItem>());
        shopConfig.save();
    }

    public void LoadConfig() {
        for (String shopOwnerUUID :
                shopConfig.config.getKeys(false)) {
            shops.put(UUID.fromString(shopConfig.config.getString(shopOwnerUUID + ".id")), new Shop(Bukkit.getOfflinePlayer(UUID.fromString(shopOwnerUUID)),shopConfig.config.getString(shopOwnerUUID + ".name"),shopConfig.config.getString(shopOwnerUUID + ".id"),(List<ShopItem>)shopConfig.config.getList(shopOwnerUUID + ".items")));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("shop") && sender instanceof Player) {
            Player player = (Player)sender;

            if(!player.hasPermission("bueessentials.shop")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
                return true;
            }

            if(args.length == 0) {
                Inventory inv = GetShopInventory(null, 0);
                player.openInventory(inv);
                return  true;
            }

            if(args[0].equalsIgnoreCase("delete")) {
                if(!player.hasPermission("bueessentials.delete")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
                    return true;
                }
                if(args.length != 1) {
                    player.sendMessage(ChatColor.RED + "/shop delete");
                    return true;
                }
                if(!shopConfig.config.contains(player.getUniqueId().toString())) {
                    player.sendMessage(ChatColor.RED + "You don't have a shop to delete");
                    return true;
                }
                Shop shop = shops.get(UUID.fromString(shopConfig.config.getString(player.getUniqueId().toString()+ ".id")));
                if(shop == null) {
                    player.sendMessage(ChatColor.RED + "You don't have a shop to delete");
                    return true;
                }
                EmptyShop(shop,player);
                shops.remove(UUID.fromString(shop.uuid));
                shopConfig.config.set(player.getUniqueId().toString(), null);
                shopConfig.save();
                player.sendMessage(ChatColor.RED + "Successfully deleted shop");

                RefreshShopInventory(shop);
            }

            else if(args[0].equalsIgnoreCase("empty")) {
                if(!player.hasPermission("bueessentials.empty")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
                    return true;
                }
                if(args.length != 1) {
                    player.sendMessage(ChatColor.RED + "/shop empty");
                    return true;
                }
                if(!shopConfig.config.contains(player.getUniqueId().toString())) {
                    player.sendMessage(ChatColor.RED + "You don't have a shop to empty");
                    return true;
                }
                Shop shop = shops.get(UUID.fromString(shopConfig.config.getString(player.getUniqueId().toString()+ ".id")));
                if(shop == null) {
                    player.sendMessage(ChatColor.RED + "You don't have a shop to empty");
                    return true;
                }
                EmptyShop(shop,player);
                player.sendMessage(ChatColor.RED + "Successfully emptied shop");

                RefreshShopInventory(shop);
            }

            else if(args[0].equalsIgnoreCase("create")) {

                if(!player.hasPermission("bueessentials.create")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
                    return true;
                }

                if(args.length == 2) {
                    if(shopConfig.config.contains(player.getUniqueId().toString())) {
                        player.sendMessage(ChatColor.RED + "You already have a shop, do '/shop delete' first!");
                    } else {
                        int shopCost = Main.Instance.getConfig().getInt("shopCost");
                        if(Main.economy.getBalance(player) < shopCost) {
                            player.sendMessage(ChatColor.RED + "You don't have enough money to create a shop, you need $" + shopCost + " to create a shop!");
                            return true;
                        }
                        Main.economy.withdrawPlayer(player,shopCost);
                        UUID shopUUID = UUID.randomUUID();
                        shopConfig.config.set(player.getUniqueId().toString() + ".name", args[1].replaceAll("_", " "));
                        shopConfig.config.set(player.getUniqueId().toString() + ".id",shopUUID.toString());
                        shopConfig.config.set(player.getUniqueId().toString() + ".items", new ArrayList<ShopItem>());
                        shopConfig.save();


                        Shop shop = new Shop((OfflinePlayer)player,args[1].replaceAll("_"," "), shopUUID.toString(), new ArrayList<ShopItem>());
                        shops.put(shopUUID,shop);

                        player.sendMessage(ChatColor.GOLD + "Created shop " + ChatColor.RED + shop.name + ChatColor.GOLD + " for " + ChatColor.RED + "$" + shopCost);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "/shop create <name> #Use _ for spaces");
                }
            }

            else if(args[0].equalsIgnoreCase("sell")) {

                if(!player.hasPermission("bueessentials.sell")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
                    return true;
                }

                if(args.length == 2 && isInteger(args[1])) {
                    if(shopConfig.config.contains(player.getUniqueId().toString())) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        int price = Integer.parseInt(args[1]);
                        if(item.getType() == Material.AIR) {
                            player.sendMessage(ChatColor.RED  + "You should be holding an item to sell");
                            return true;
                        }


                        Shop shop = shops.get(UUID.fromString(shopConfig.config.getString(player.getUniqueId().toString() + ".id")));
                        if(shop.items.size() >= 40) {
                            player.sendMessage(ChatColor.RED + "You can't have more than 40 items per shop");
                            return true;
                        }

                        String itemName = ChatColor.RED + "" + item.getAmount() + "x" + item.getType().name();

                        player.sendMessage(ChatColor.GOLD + "You put " + itemName + ChatColor.GOLD + " on sale for " + ChatColor.RED + "$" + price);
                        shop.items.add(new ShopItem(item,shop.uuid,price));

                        player.getInventory().clear(player.getInventory().getHeldItemSlot());

                        shopConfig.config.set(player.getUniqueId().toString() + ".items", shop.items);
                        shopConfig.save();

                        RefreshShopInventory(shop);
                    } else {
                        player.sendMessage(ChatColor.RED + "You have to create a shop first, /shop create");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "/shop sell <price>");
                }
            } else {
                player.sendMessage(ChatColor.RED + "/shop <create/delete/empty/sell>");
            }
        }
        return true;
    }

    public Inventory GetShopInventory(String filter, int page) {
        if(filter == null || filter.trim().isEmpty() || filter.isEmpty()) filter = null;
        if(page < 0) page = 0;
        Inventory shopInv = Bukkit.createInventory(null,54,"#$ SHOP $#");

        int i = 0;
        int startF = (page*36);
        for (Shop shop:
             shops.values()) {
            if(i < startF || i >= startF+36) continue;

            List<String> lore = new ArrayList<>();
            lore.add(convertToInvisibleString("#$%#%SHOP"));
            lore.add(convertToInvisibleString(shop.uuid));


            if(filter != null) {
                boolean found = false;
                for (ShopItem d:
                     shop.items) {
                    if(d.item.getType().name().toLowerCase().contains(filter.toLowerCase())){
                        found = true;
                        lore.add(ChatColor.GOLD + "" + d.item.getAmount() + "x" + d.item.getType().name());
                        lore.add(ChatColor.RED + "$" + d.price);
                        break;
                    }
                }

                if(!found) continue;
            }
            ItemStack shopItem = new ItemStack(Material.IRON_BLOCK);
            ItemMeta shopItemMeta = shopItem.getItemMeta();
            shopItemMeta.setDisplayName(shop.name);
            shopItemMeta.setLore(lore);
            shopItem.setItemMeta(shopItemMeta);

            shopInv.setItem(i,shopItem);
            i++;
        }

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoItemMeta = infoItem.getItemMeta();
        infoItemMeta.setDisplayName(ChatColor.AQUA + "INFO");
        infoItemMeta.setLore(Arrays.asList(ChatColor.BLACK + "Page " + page, ChatColor.BLACK + "Filter " + ((filter == null)?"":filter)));
        infoItem.setItemMeta(infoItemMeta);
        shopInv.setItem(45,infoItem);

        ItemStack filterItem = new ItemStack(Material.HOPPER);
        ItemMeta filterItemMeta = filterItem.getItemMeta();
        filterItemMeta.setDisplayName(ChatColor.AQUA + "FILTER");
        filterItem.setItemMeta(filterItemMeta);
        shopInv.setItem(53,filterItem);

        if(shops.size() > 36) {
            ItemStack nextItem = new ItemStack(Material.PAPER);
            ItemMeta nextItemMeta = nextItem.getItemMeta();
            nextItemMeta.setDisplayName(ChatColor.AQUA + "NEXT");
            nextItem.setItemMeta(nextItemMeta);
            shopInv.setItem(50, nextItem);
        }

        if(page != 0) {
            ItemStack backItem = new ItemStack(Material.PAPER);
            ItemMeta backItemMeta = backItem.getItemMeta();
            backItemMeta.setDisplayName(ChatColor.AQUA + "BACK");
            backItem.setItemMeta(backItemMeta);
            shopInv.setItem(48, backItem);
        }

        return shopInv;
    }

    @EventHandler
    public void OnCloseInventory(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith("$#$#") && playersInShops.containsKey(event.getPlayer())) {
            playersInShops.remove(event.getPlayer());
        }
    }

    @EventHandler
    public void OnChat(PlayerChatEvent event) {
        if(playersFiltering.contains(event.getPlayer())) {
            playersFiltering.remove(event.getPlayer());

            event.getPlayer().openInventory(GetShopInventory(event.getMessage(),0));

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void OnInventoryInteract(InventoryClickEvent event) {
        if(event.getView().getTitle().equalsIgnoreCase("#$ SHOP $#")) {
            event.setCancelled(true);

            if(event.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.AQUA + "FILTER")) {
                playersFiltering.add((Player) event.getWhoClicked());
                event.getWhoClicked().sendMessage(ChatColor.GOLD + "Write the material name you want to filter: ");
                Bukkit.getServer().getScheduler().runTaskLater(Main.Instance, new Runnable(){
                    public void run(){
                        event.getWhoClicked().closeInventory();
                    }
                },1);

                Bukkit.getServer().getScheduler().runTaskLater(Main.Instance, new Runnable(){
                    public void run(){
                        if(playersFiltering.contains((Player) event.getWhoClicked())) {
                            playersFiltering.remove((Player)event.getWhoClicked());
                            event.getWhoClicked().sendMessage(ChatColor.RED  +"Filter canceled");
                        }
                    }
                },200);
                return;
            }
            if(event.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.AQUA + "NEXT")) {
                List<String> infoLore = event.getView().getItem(45).getItemMeta().getLore();

                event.getWhoClicked().openInventory(GetShopInventory(infoLore.get(1).substring(9),Integer.parseInt(infoLore.get(0).substring(7))+1));
            }
            if(event.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.AQUA + "BACK")) {
                List<String> infoLore = event.getView().getItem(45).getItemMeta().getLore();
                event.getWhoClicked().openInventory(GetShopInventory(infoLore.get(1).substring(9),Integer.parseInt(infoLore.get(0).substring(7))-1));
            }
            List<String> lore = event.getCurrentItem().getItemMeta().getLore();

            if((lore.get(0).replaceAll("§", "")).equalsIgnoreCase("#$%#%SHOP")) {
                UUID shopUUID = UUID.fromString(lore.get(1).replaceAll("§", ""));
                if(!shops.containsKey(shopUUID)) return;

                Shop shop = shops.get(shopUUID);
                Inventory shopInv = getShopInventory(shop,(Player) event.getWhoClicked(),event.getView().getItem(45).getItemMeta().getLore().get(1).substring(9));
                event.getWhoClicked().closeInventory();
                event.getWhoClicked().openInventory(shopInv);
                playersInShops.put((Player) event.getWhoClicked(), shop);
                return;
            }
        }
        if(event.getView().getTitle().startsWith("$#$#")) {

            event.setCancelled(true);
            if(event.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GOLD + "BACK")) {
                event.getWhoClicked().closeInventory();
                String filt = event.getView().getItem(47).getItemMeta().getLore().get(1).substring(9);
                event.getWhoClicked().openInventory(GetShopInventory((filt.isEmpty())?null:filt,0));
                if(playersInShops.containsKey((Player)event.getWhoClicked())) {
                    playersInShops.remove((Player) event.getWhoClicked());
                }
                return;
            }

            if(event.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.RED + "DELETE")) {
                ((Player)event.getWhoClicked()).performCommand("shop delete");
                Bukkit.getServer().getScheduler().runTaskLater(Main.Instance, new Runnable(){
                    public void run(){
                        event.getWhoClicked().closeInventory();
                    }
                },1);
                return;
            }

            if(event.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.AQUA + "INFO")) {
                return;
            }

            if(event.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.RED + "EMPTY")) {
                ((Player)event.getWhoClicked()).performCommand("shop empty");
                Bukkit.getServer().getScheduler().runTaskLater(Main.Instance, new Runnable(){
                    public void run(){
                        event.getWhoClicked().closeInventory();

                    }
                },1);
                return;
            }
            Main.Instance.getLogger().info(event.getView().getTitle());
            UUID shopUUID = UUID.fromString(event.getView().getItem(47).getItemMeta().getLore().get(0).replaceAll("§", ""));
            if(!shops.containsKey(shopUUID)) return;
            Shop shop = shops.get(shopUUID);
            int pos = Integer.parseInt(event.getCurrentItem().getItemMeta().getLore().get(event.getCurrentItem().getItemMeta().getLore().size()-1).substring(2));

            ShopItem item = shop.items.get(pos);


            if(!shop.owner.equals((OfflinePlayer)event.getWhoClicked()) && Main.economy.getBalance((OfflinePlayer)event.getWhoClicked()) < item.price) {
                event.getWhoClicked().sendMessage(ChatColor.RED + "Not enough money to buy this item");
                return;
            }
            if(!shop.owner.equals((OfflinePlayer)event.getWhoClicked())) {
                Main.economy.withdrawPlayer((OfflinePlayer) event.getWhoClicked(), item.price);
                Main.economy.depositPlayer(shop.owner, item.price);
            }
            Map<Integer, ItemStack> map = event.getWhoClicked().getInventory().addItem(item.item);
            if(!map.isEmpty()) {
                for (ItemStack i :
                        map.values()) {
                    event.getWhoClicked().getWorld().dropItem(event.getWhoClicked().getLocation(),i);
                }
            }
            List<ShopItem> itemsIn = shop.items;
            itemsIn.remove(pos);

            shopConfig.config.set(shop.owner.getUniqueId() + ".items", itemsIn);
            shopConfig.save();


            String itemName = ChatColor.RED + "" + item.item.getAmount() + "x" + item.item.getType().name();
            if(shop.owner.isOnline()) ((Player) shop.owner) .sendMessage(ChatColor.RED + ((OfflinePlayer) event.getWhoClicked()).getPlayer().getDisplayName() + ChatColor.GOLD + " bought " + itemName + ChatColor.GOLD + " for " + ChatColor.RED + "$" + item.price);
            event.getWhoClicked().sendMessage(ChatColor.GOLD + "Bought " + itemName + ChatColor.GOLD +  " for " + ChatColor.RED + "$" + item.price );


            Bukkit.getServer().getScheduler().runTaskLater(Main.Instance, new Runnable(){
                public void run(){
                    event.getWhoClicked().closeInventory();

                    RefreshShopInventory(shop);
                }
            },1);
        }
    }

    public void RefreshShopInventory(Shop shop) {
        for (Map.Entry<Player, Shop> a:
             playersInShops.entrySet()) {
            if(shop.equals(a.getValue())) {
                a.getKey().openInventory(getShopInventory(shop,a.getKey(), a.getKey().getOpenInventory().getItem(47).getItemMeta().getLore().get(1)));
            }
        }
    }

    Inventory getShopInventory(Shop shop, Player player, String filter) {
        String s = "$#$# " + shop.name;

        Inventory inventory = Bukkit.createInventory(null,54,s);
        int i = 0;
        for (ShopItem itemS :
                shop.items) {
            ItemStack item = new ItemStack(itemS.item);
            ItemMeta meta = item.getItemMeta();
            List<String> lore = (meta.hasLore())?meta.getLore(): new ArrayList<>();
            lore.add(ChatColor.GRAY + "$" + itemS.price);
            lore.add("#!" + i);

            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i, item);

            i++;
        }

        ItemStack backItem = new ItemStack(Material.PAPER);
        ItemMeta backItemMeta = backItem.getItemMeta();
        backItemMeta.setDisplayName(ChatColor.GOLD + "BACK");
        backItem.setItemMeta(backItemMeta);

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoItemMeta = infoItem.getItemMeta();
        infoItemMeta.setDisplayName(ChatColor.AQUA + "INFO");
        infoItemMeta.setLore(Arrays.asList(convertToInvisibleString(shop.uuid), ChatColor.BLACK + "Filter " + ((filter == null)?"":filter)));
        infoItem.setItemMeta(infoItemMeta);
        inventory.setItem(47,infoItem);

        if(shop.owner.equals((OfflinePlayer)player)) {
            ItemStack deleteShopItem = new ItemStack(Material.REDSTONE_BLOCK);
            ItemMeta deleteShopItemMeta = deleteShopItem.getItemMeta();
            deleteShopItemMeta.setDisplayName(ChatColor.RED + "DELETE");
            deleteShopItem.setItemMeta(deleteShopItemMeta);
            inventory.setItem(45,deleteShopItem);

            ItemStack clearShopItem = new ItemStack(Material.MAP);
            ItemMeta clearShopItemMeta = clearShopItem.getItemMeta();
            clearShopItemMeta.setDisplayName(ChatColor.RED + "EMPTY");
            clearShopItem.setItemMeta(clearShopItemMeta);
            inventory.setItem(46,clearShopItem);
        }

        inventory.setItem(53,backItem);

        return inventory;
    }
    public static String convertToInvisibleString(String s) {
        String hidden = "";
        for (char c : s.toCharArray()) hidden += ChatColor.COLOR_CHAR+""+c;
        return hidden;
    }

    public static boolean isInteger(String s) {
        boolean isValidInteger = false;
        try
        {
            Integer.parseInt(s);

            // s is a valid integer

            isValidInteger = true;
        }
        catch (NumberFormatException ex)
        {
            // s is not an integer
        }

        return isValidInteger;
    }
}
