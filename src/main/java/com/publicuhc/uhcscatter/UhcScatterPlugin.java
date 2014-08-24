package com.publicuhc.uhcscatter;

import com.publicuhc.ultrahardcore.UltraHardcore;
import com.publicuhc.ultrahardcore.framework.routing.Router;
import com.publicuhc.ultrahardcore.framework.routing.exception.CommandParseException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class UhcScatterPlugin extends JavaPlugin {

    @Override
    public void onEnable()
    {
        UltraHardcore uhc = (UltraHardcore) Bukkit.getPluginManager().getPlugin("UltraHardcore");

        Router router = uhc.getCommandRouter();
        try {
            router.registerCommands(ScatterCommand.class);
        } catch (CommandParseException e) {
            e.printStackTrace();
        }
    }
}
