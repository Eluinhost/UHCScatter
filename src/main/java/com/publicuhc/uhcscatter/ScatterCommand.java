package com.publicuhc.uhcscatter;

import com.publicuhc.scatter.DefaultScatterer;
import com.publicuhc.scatter.Scatterer;
import com.publicuhc.scatter.exceptions.ScatterLocationException;
import com.publicuhc.scatter.logic.RandomCircleScatterLogic;
import com.publicuhc.scatter.logic.RandomSquareScatterLogic;
import com.publicuhc.scatter.logic.StandardScatterLogic;
import com.publicuhc.scatter.zones.CircularDeadZoneBuilder;
import com.publicuhc.scatter.zones.DeadZone;
import com.publicuhc.ultrahardcore.framework.configuration.Configurator;
import com.publicuhc.ultrahardcore.framework.routing.CommandMethod;
import com.publicuhc.ultrahardcore.framework.routing.CommandRequest;
import com.publicuhc.ultrahardcore.framework.routing.OptionsMethod;
import com.publicuhc.ultrahardcore.framework.routing.converters.LocationValueConverter;
import com.publicuhc.ultrahardcore.framework.routing.converters.OnlinePlayerValueConverter;
import com.publicuhc.ultrahardcore.framework.shaded.javax.Inject;
import com.publicuhc.ultrahardcore.framework.shaded.joptsimple.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class ScatterCommand  {

    private final List<Material> mats = new ArrayList<Material>();
    private final int maxAttempts;

    private NonOptionArgumentSpec<Player[]> nonOptions;

    @Inject
    public ScatterCommand(Configurator configurator, PluginLogger logger)
    {
        FileConfiguration config = configurator.getConfig("main");
        List<String> stringMats = config.getStringList("allowed blocks");
        for(String stringMat : stringMats) {
            Material mat = Material.matchMaterial(stringMat);
            if(null == mat)
                logger.severe("Unknown material " + stringMat);
            else
                mats.add(mat);
        }
        maxAttempts = config.getInt("max attempts per location");
    }

    @CommandMethod(command = "sct", permission = "UHC.scatter.command", options = true)
    public void onScatterCommand(CommandRequest request)
    {
        OptionSet set = request.getOptions();
        StandardScatterLogic logic = (StandardScatterLogic) set.valueOf("t");
        Double radius = (Double) set.valueOf("r");
        Double minRadius = (Double) set.valueOf("minradius");
        Double min = (Double) set.valueOf("min");
        Location center = (Location) set.valueOf("c");
        boolean asTeams = set.has("teams");

        List<Player[]> playerlist = set.valuesOf(nonOptions);

        List<Player> toScatter = new ArrayList<Player>();
        for(Player[] plist : playerlist) {
            Collections.addAll(toScatter, plist);
        }

        logic.setCentre(center);
        logic.setMaxAttempts(maxAttempts);
        logic.setRadius(radius);

        if(!mats.isEmpty())
            logic.setMaterials(mats);

        CircularDeadZoneBuilder deadZoneBuilder = new CircularDeadZoneBuilder(min);

        List<DeadZone> baseDeadZones = new ArrayList<DeadZone>();
        if(min > 0) {
            //add dead zones for all not scattered players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!toScatter.contains(player)) {
                    baseDeadZones.add(deadZoneBuilder.buildForLocation(player.getLocation()));
                }
            }
        }

        if(minRadius > 0) {
            CircularDeadZoneBuilder builder = new CircularDeadZoneBuilder(minRadius);
            baseDeadZones.add(builder.buildForLocation(center));
        }

        Scatterer scatterer = new DefaultScatterer(logic, baseDeadZones, deadZoneBuilder);

        HashMap<String, List<Player>> teams = new HashMap<String, List<Player>>();
        if(asTeams) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Iterator<Player> players = toScatter.iterator();
            while(players.hasNext()) {
                Player player = players.next();
                Team team = scoreboard.getPlayerTeam(player);
                if(null != team) {
                    List<Player> playerList = teams.get(team.getName());
                    if(null == playerList) {
                        playerList = new ArrayList<Player>();
                        teams.put(team.getName(), playerList);
                    }
                    playerList.add(player);
                    players.remove();
                }
            }
        }

        int amount = toScatter.size() + teams.size();
        List<Location> locations;
        try {
            locations = scatterer.getScatterLocations(amount);
        } catch (ScatterLocationException e) {
            request.sendMessage(ChatColor.RED + "Couldn't find valid locations for all players");
            return;
        }

        Collections.shuffle(locations);
        Bukkit.broadcastMessage(ChatColor.GOLD + "Starting scatter of " + amount + " players/teams");

        Iterator<Location> locationIterator = locations.iterator();
        int count = 1;

        for(Map.Entry<String, List<Player>> team : teams.entrySet()) {
            Location location = locationIterator.next();
            location.add(0, 1, 0);

            for(Player p : team.getValue()) {
                p.teleport(location);
            }

            Bukkit.broadcastMessage(ChatColor.GREEN + "[" + count++ + "/" + amount + "] Team " + team.getKey() + " scattered");
        }

        for(Player p : toScatter) {
            Location location = locationIterator.next();
            location.add(0, 1, 0);

            p.teleport(location);
            Bukkit.broadcastMessage(ChatColor.GREEN + "[" + count++ + "/" + amount + "] " + p.getName() + " scattered");
        }

        Bukkit.broadcastMessage(ChatColor.GOLD + "Scatter complete");
    }

    @OptionsMethod
    public void onScatterCommand(OptionParser parser)
    {
        nonOptions = parser.nonOptions().withValuesConvertedBy(new OnlinePlayerValueConverter(true));
        parser.accepts("t")
                .withRequiredArg()
                .required()
                .withValuesConvertedBy(new ValueConverter<StandardScatterLogic>() {
                    @Override
                    public StandardScatterLogic convert(String value) {
                        if(value.equalsIgnoreCase("circle"))
                            return new RandomCircleScatterLogic(new Random());
                        if(value.equalsIgnoreCase("square"))
                            return new RandomSquareScatterLogic(new Random());
                        throw new ValueConversionException("Invalid type");
                    }
                    @Override
                    public Class<StandardScatterLogic> valueType() {
                        return StandardScatterLogic.class;
                    }
                    @Override
                    public String valuePattern() {
                        return null;
                    }
                })
                .describedAs("Type of scatter to use");
        parser.accepts("teams", "Scatter players as teams");
        parser.accepts("c")
                .withRequiredArg()
                .required()
                .withValuesConvertedBy(new LocationValueConverter())
                .describedAs("World/coordinates for the center");
        parser.accepts("r")
                .withRequiredArg()
                .ofType(Double.class)
                .required()
                .describedAs("Radius for scatter");
        parser.accepts("min")
                .withRequiredArg()
                .ofType(Double.class)
                .defaultsTo(0D)
                .describedAs("Minimum distance between players after scatter");
        parser.accepts("minradius")
                .withRequiredArg()
                .ofType(Double.class)
                .defaultsTo(0D)
                .describedAs("Minimum radius from the center of the scatter");
    }
}
