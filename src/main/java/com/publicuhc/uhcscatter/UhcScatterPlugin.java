package com.publicuhc.uhcscatter;

import com.publicuhc.pluginframework.FrameworkJavaPlugin;
import com.publicuhc.pluginframework.configuration.Configurator;
import com.publicuhc.pluginframework.routing.exception.CommandParseException;
import com.publicuhc.pluginframework.shaded.inject.Inject;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class UhcScatterPlugin extends FrameworkJavaPlugin {

    @Override
    public void onFrameworkEnable()
    {
        try {
            getRouter().registerCommands(ScatterCommand.class);
        } catch (CommandParseException e) {
            e.printStackTrace();
        }
    }
}
