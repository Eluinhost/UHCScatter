package com.publicuhc.uhcscatter;

import com.publicuhc.scatter.DefaultScatterer;
import com.publicuhc.scatter.Scatterer;
import com.publicuhc.scatter.exceptions.ScatterLocationException;
import com.publicuhc.scatter.logic.RandomCircleScatterLogic;
import com.publicuhc.scatter.logic.RandomSquareScatterLogic;
import com.publicuhc.scatter.logic.StandardScatterLogic;
import com.publicuhc.scatter.zones.CircularDeadZoneBuilder;
import com.publicuhc.scatter.zones.DeadZone;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class ScatterCommand implements CommandExecutor {

    public static final String SYNTAX = ChatColor.RED + "/sct typeID radius worldName players [-c=x,z] [-t] [-min=minDist] [-minradius=minRadius]";

    private final List<Material> mats;
    private final int maxAttempts;

    public ScatterCommand(List<Material> mats, int maxAttempts)
    {
        this.mats = mats;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Parse the scatter command in the syntax:
     *     /sct typeID radius worldName players [-c=x,z] [-t] [-min=minDist] [-minradius=minRadius]
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!sender.hasPermission("UHC.scatter.command")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to run this command");
            return true;
        }
        if(args.length < 4) {
            sender.sendMessage(SYNTAX);
            return true;
        }

        String typeID = args[0];
        StandardScatterLogic logic;
        if(typeID.equalsIgnoreCase("circle")) {
            logic = new RandomCircleScatterLogic(new Random());
        } else if(typeID.equalsIgnoreCase("square")) {
            logic = new RandomSquareScatterLogic(new Random());
        } else {
            sender.sendMessage(ChatColor.RED + "Type ID must be 'circle' or 'square', " + args[0] + "given");
            return true;
        }

        double radius;
        try {
            radius = Double.parseDouble(args[1]);
            if(radius < 0)
                throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Radius must be a positive number!");
            return true;
        }

        World world = Bukkit.getWorld(args[2]);
        if(null == world) {
            sender.sendMessage(ChatColor.RED + "World " + args[2] + " not found!");
            return true;
        }

        List<Player> toScatter = new ArrayList<Player>();
        boolean asTeams = false;
        double minDist = 0;
        double minRadius = 0;

        double centerX = world.getSpawnLocation().getX();
        double centerZ = world.getSpawnLocation().getZ();

        for(int i = 3; i < args.length; i++) {
            String arg = args[i];

            if(arg.charAt(0) == '-') {
                String lowerArg = arg.toLowerCase();
                if (lowerArg.startsWith("-c=")) {
                    arg = arg.substring(3);
                    String[] coords = arg.split(",");
                    if(coords.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Invalid coords given, must be in the format x,z");
                        return true;
                    }
                    try {
                        centerX = Double.parseDouble(coords[0]);
                        centerZ = Double.parseDouble(coords[1]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "Invalid number/s for coords given");
                        return true;
                    }
                } else if (lowerArg.startsWith("-t")) {
                    asTeams = true;
                } else if (lowerArg.startsWith("-min=")) {
                    arg = arg.substring(5);
                    try {
                        minDist = Double.parseDouble(arg);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "Invalid number for minimum distance given");
                        return true;
                    }
                } else if (lowerArg.startsWith("-minradius=")) {
                    arg = arg.substring(11);
                    try {
                        minRadius = Double.parseDouble(arg);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "Invalid number for minimum radius given");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid option: " + arg);
                    return true;
                }
                continue;
            }

            if(arg.equals("*")) {
                toScatter.addAll(Arrays.asList(Bukkit.getOnlinePlayers()));
                continue;
            }

            Player player = Bukkit.getPlayer(arg);
            if(null == player) {
                sender.sendMessage(ChatColor.RED + "Player " + arg + " not found!");
                continue;
            }
            toScatter.add(player);
        }

        logic.setCentre(new Location(world, centerX, 0, centerZ));
        logic.setMaxAttempts(maxAttempts);
        logic.setRadius(radius);
        if(!mats.isEmpty()) {
            logic.addMaterials(mats.toArray(new Material[mats.size()]));
        }

        List<DeadZone> baseDeadZones = new ArrayList<DeadZone>();

        if(minDist > 0) {
            CircularDeadZoneBuilder builder = new CircularDeadZoneBuilder(minDist);

            //add dead zones for all not scattered players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!toScatter.contains(player)) {
                    baseDeadZones.add(builder.buildForLocation(player.getLocation()));
                }
            }
        }

        if(minRadius > 0) {
            CircularDeadZoneBuilder builder = new CircularDeadZoneBuilder(minRadius);
            baseDeadZones.add(builder.buildForLocation(new Location(world, centerX, 0, centerZ)));
        }

        Scatterer scatterer = new DefaultScatterer(
                logic,
                baseDeadZones,
                new CircularDeadZoneBuilder(minDist)
        );

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
            sender.sendMessage(ChatColor.RED + "Couldn't find valid locations for all players");
            return true;
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
        return true;
    }
}
