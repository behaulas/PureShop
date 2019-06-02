package com.behaulas.pureshop;

import org.bukkit.OfflinePlayer;

import java.util.List;

public class Shop {
    public OfflinePlayer owner;
    public String name ,uuid;
    public List<ShopItem> items;


    public Shop(OfflinePlayer owner, String name, String uuid, List<ShopItem> items) {
        this.owner = owner;
        this.name = name;
        this.uuid = uuid;
        this.items = items;
    }
}
