package com.behaulas.pureshop;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

@SerializableAs("ShopItem")
public class ShopItem implements ConfigurationSerializable {
    public ItemStack item;
    public String shopUUID;

    public int price;

    public ShopItem(ItemStack item, String shopUUID, int price) {
        this.item = item;
        this.shopUUID = shopUUID;
        this.price = price;
    }

    public ShopItem(Map<String,Object> map) {
        this.item = (ItemStack)map.get("item");
        this.shopUUID = (String)map.get("shopUUID");
        this.price = (Integer)map.get("price");

    }

    @Override
    public Map<String, Object> serialize() {
        Map<String,Object> map = new HashMap<>();
        map.put("item",item);
        map.put("shopUUID", shopUUID);
        map.put("price", price);
        return map;
    }
}
