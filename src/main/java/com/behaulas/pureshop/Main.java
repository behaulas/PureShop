package com.behaulas.pureshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    public static Economy economy;
    public static Plugin Instance;
    ShopManager shop;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        ConfigurationSerialization.registerClass(ShopItem.class, "ShopItem");
        Instance = this;
        shop = new ShopManager();

        getCommand("shop").setExecutor(shop);
        getServer().getPluginManager().registerEvents(shop, this);

        if(!setupEconomy()) Bukkit.shutdown();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
