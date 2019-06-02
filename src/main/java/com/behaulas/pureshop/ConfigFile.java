package com.behaulas.pureshop;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigFile {
    File file;
    FileConfiguration config;

    public ConfigFile(String fileName) {

        if(!Main.Instance.getDataFolder().exists()) Main.Instance.getDataFolder().mkdirs();
        file = new File(Main.Instance.getDataFolder(),fileName);
        if(!file.exists())try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        config = YamlConfiguration.loadConfiguration(file);

    }

    public void save() {
        try{
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
