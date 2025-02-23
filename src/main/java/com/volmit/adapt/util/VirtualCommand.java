/*------------------------------------------------------------------------------
 -   Adapt is a Skill/Integration plugin  for Minecraft Bukkit Servers
 -   Copyright (c) 2022 Arcane Arts (Volmit Software)
 -
 -   This program is free software: you can redistribute it and/or modify
 -   it under the terms of the GNU General Public License as published by
 -   the Free Software Foundation, either version 3 of the License, or
 -   (at your option) any later version.
 -
 -   This program is distributed in the hope that it will be useful,
 -   but WITHOUT ANY WARRANTY; without even the implied warranty of
 -   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 -   GNU General Public License for more details.
 -
 -   You should have received a copy of the GNU General Public License
 -   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 -----------------------------------------------------------------------------*/

package com.volmit.adapt.util;

import com.volmit.adapt.Adapt;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Represents a virtual command. A chain of iterative processing through
 * subcommands.
 *
 * @author cyberpwn
 */
public class VirtualCommand {
    private final ICommand command;
    private final String tag;

    private final Map<List<String>, VirtualCommand> children;

    public VirtualCommand(ICommand command) {
        this(command, "");
    }

    public VirtualCommand(ICommand command, String tag) {
        this.command = command;
        children = new HashMap<>();
        this.tag = tag;

        for (Field i : command.getClass().getDeclaredFields()) {
            if (i.isAnnotationPresent(Command.class)) {
                try {
                    Command cc = i.getAnnotation(Command.class);
                    ICommand cmd = (ICommand) i.getType().getConstructor().newInstance();
                    new V(command, true, true).set(i.getName(), cmd);
                    children.put(cmd.getAllNodes(), new VirtualCommand(cmd, cc.value().trim().isEmpty() ? tag : cc.value().trim()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getTag() {
        return tag;
    }

    public ICommand getCommand() {
        return command;
    }

    public Map<List<String>, VirtualCommand> getChildren() {
        return children;
    }

    public boolean hit(CommandSender sender, List<String> chain) {
        return hit(sender, chain, null);
    }

    public boolean hit(CommandSender sender, List<String> chain, String label) {
        MortarSender vs = new MortarSender(sender);
        vs.setTag(tag);

        if (label != null) {
            vs.setCommand(label);
        }

        if (chain.isEmpty()) {
            if (!checkPermissions(sender, command)) {
                return true;
            }

            return command.handle(vs, new String[0]);
        }

        String nl = chain.get(0);

        for (List<String> i : children.k()) {
            for (String j : i) {
                if (j.equalsIgnoreCase(nl)) {
                    vs.setCommand(chain.get(0));
                    VirtualCommand cmd = children.get(i);
                    List<String> c = chain.copy();
                    c.remove(0);
                    if (cmd.hit(sender, c, vs.getCommand())) {
                        if (vs.isPlayer()) {
                            vs.player().getWorld().playSound(vs.player().getLocation(), Sound.ITEM_AXE_STRIP, 0.35f, 1.8f);
                        }

                        return true;
                    }
                }
            }
        }

        if (!checkPermissions(sender, command)) {
            return true;
        }

        return command.handle(vs, chain.toArray(new String[chain.size()]));
    }

    public List<String> hitTab(CommandSender sender, List<String> chain, String label) {
        MortarSender vs = new MortarSender(sender);
        vs.setTag(tag);

        if (label != null)
            vs.setCommand(label);

        if (chain.isEmpty()) {
            if (!checkPermissions(sender, command)) {
                return null;
            }

            return command.handleTab(vs, new String[0]);
        }

        String nl = chain.get(0);

        for (List<String> i : children.k()) {
            for (String j : i) {
                if (j.equalsIgnoreCase(nl)) {
                    vs.setCommand(chain.get(0));
                    VirtualCommand cmd = children.get(i);
                    List<String> c = chain.copy();
                    c.remove(0);
                    List<String> v = cmd.hitTab(sender, c, vs.getCommand());
                    if (v != null) {
                        return v;
                    }
                }
            }
        }

        if (!checkPermissions(sender, command)) {
            return null;
        }

        return command.handleTab(vs, chain.toArray(new String[chain.size()]));
    }

    private boolean checkPermissions(CommandSender sender, ICommand command2) {
        boolean failed = false;

        for (String i : command.getRequiredPermissions()) {
            if (!sender.hasPermission(i)) {
                failed = true;
                Bukkit.getScheduler().scheduleSyncDelayedTask(Adapt.instance, () -> Adapt.messagePlayer(sender.getServer().getPlayer(sender.getName()),"- " + C.WHITE + i), 0);
            }
        }

        if (failed) {
            Adapt.messagePlayer(sender.getServer().getPlayer(sender.getName()),"Insufficient Permissions");
            return false;
        }

        return true;
    }
}
