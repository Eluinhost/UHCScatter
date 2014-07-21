package com.publicuhc.uhcscatter;

import org.bukkit.plugin.java.JavaPlugin;

public class UhcScatterPlugin extends JavaPlugin {

    @Override
    public void onEnable()
    {
        getServer().getPluginCommand("scatter").setExecutor(new ScatterCommand());
    }
}
