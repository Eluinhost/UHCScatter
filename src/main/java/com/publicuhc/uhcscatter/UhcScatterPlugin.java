package com.publicuhc.uhcscatter;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class UhcScatterPlugin extends JavaPlugin {

    @Override
    public void onEnable()
    {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        List<String> stringMats = config.getStringList("allowed blocks");
        List<Material> mats = new ArrayList<Material>();
        for(String stringMat : stringMats) {
            Material mat = Material.matchMaterial(stringMat);
            if(null == mat) {
                getLogger().severe("Unknown material " + stringMat);
            } else {
                mats.add(mat);
            }
        }

        int maxAttempts = config.getInt("max attempts per location");

        getServer().getPluginCommand("scatter").setExecutor(new ScatterCommand(mats, maxAttempts));
    }
}
