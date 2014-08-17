package com.publicuhc.uhcscatter;

import com.publicuhc.pluginframework.FrameworkJavaPlugin;
import com.publicuhc.pluginframework.configuration.Configurator;
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
        FileConfiguration config = getConfigurator().getConfig("main");
        List<String> stringMats = config.getStringList("allowed blocks");
        List<Material> mats = new ArrayList<Material>();
        for(String stringMat : stringMats) {
            Material mat = Material.matchMaterial(stringMat);
            if(null == mat)
                getLogger().severe("Unknown material " + stringMat);
            else
                mats.add(mat);
        }
        int maxAttempts = config.getInt("max attempts per location");

        getServer().getPluginCommand("sct").setExecutor(new ScatterCommand(mats, maxAttempts));
    }
}
