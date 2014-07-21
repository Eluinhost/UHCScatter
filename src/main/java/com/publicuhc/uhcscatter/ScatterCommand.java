package com.publicuhc.uhcscatter;

import com.publicuhc.scatter.logic.RandomCircleScatterLogic;
import com.publicuhc.scatter.logic.RandomSquareScatterLogic;
import com.publicuhc.scatter.logic.StandardScatterLogic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ScatterCommand implements CommandExecutor {

    public static final String SYNTAX = ChatColor.RED + "/scatter typeID radius worldName players [-c=x,z] [-t] [-min=minDist] [-minradius=minRadius]";

    /**
     * Parse the scatter command in the syntax:
     *     /scatter typeID radius worldName players [-c=x,z] [-t] [-min=minDist] [-minradius=minRadius]
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
                if (arg.startsWith("-c=")) {
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
                } else if (arg.startsWith("-t")) {
                    asTeams = true;
                } else if (arg.startsWith("-min=")) {
                    arg = arg.substring(5);
                    try {
                        minDist = Double.parseDouble(arg);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "Invalid number for minimum distance given");
                        return true;
                    }
                } else if (arg.startsWith("-minradius=")) {
                    arg = arg.substring(10);
                    try {
                        minRadius = Double.parseDouble(arg);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "Invalid number for minimum radius given");
                        return true;
                    }
                }
                continue;
            }

            if(arg.equals("*")) {
                toScatter.addAll(Arrays.asList(Bukkit.getOnlinePlayers()));
            }

            Player player = Bukkit.getPlayer(arg);
            if(null == player) {
                sender.sendMessage(ChatColor.RED + "Player " + arg + " not found!");
                continue;
            }
            toScatter.add(player);
        }

        logic.setCentre(new Location(world, centerX, 0, centerZ));
        logic.setMaxAttempts(250); //TODO config
        logic.setRadius(radius);




        return false;
    }
}
