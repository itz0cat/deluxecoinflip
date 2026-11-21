/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import net.zithium.deluxecoinflip.DeluxeCoinflipPlugin;
import net.zithium.deluxecoinflip.bias.BiasManager;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.command.CommandSender;

import java.util.Collection;

@CommandAlias("bias|cfbias|coinflipbias")
@Description("Admin command to configure coinflip win bias / weight per player")
@CommandPermission("coinflip.admin")
public class BiasCommand extends BaseCommand {

    private final DeluxeCoinflipPlugin plugin;

    public BiasCommand(DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    @Subcommand("help")
    public void onHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.color("&e&lDeluxeCoinflip &8- &6Bias Control System"));
        sender.sendMessage(ColorUtil.color("&e/bias add <player> [weight] &7- Force player to win (default 100% / always)"));
        sender.sendMessage(ColorUtil.color("&e/bias set <player> <weight> &7- Set specific win weight/percent (e.g. 75)"));
        sender.sendMessage(ColorUtil.color("&e/bias remove <player> &7- Remove bias for a player"));
        sender.sendMessage(ColorUtil.color("&e/bias check <player> &7- Check current bias of a player"));
        sender.sendMessage(ColorUtil.color("&e/bias list &7- List all currently biased players"));
        sender.sendMessage(ColorUtil.color("&e/bias clear &7- Clear all active biases"));
        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
    }

    @Subcommand("add")
    @CommandCompletion("@players")
    public void onAdd(CommandSender sender, String playerName, @Optional Double weight) {
        double finalWeight = (weight != null) ? weight : 100.0;
        if (finalWeight < 0.0) {
            sender.sendMessage(ColorUtil.color("&cWeight cannot be negative!"));
            return;
        }

        BiasManager.BiasEntry entry = plugin.getBiasManager().setBias(playerName, finalWeight);
        if (finalWeight >= 100.0) {
            sender.sendMessage(ColorUtil.color("&aPlayer &e" + entry.getName() + " &awill now &6&lALWAYS WIN &aall coinflips (weight: &e" + finalWeight + "%&a)."));
        } else {
            sender.sendMessage(ColorUtil.color("&aSet coinflip win weight for &e" + entry.getName() + " &ato &e" + finalWeight + "%&a."));
        }
    }

    @Subcommand("set|weight")
    @CommandCompletion("@players")
    public void onSet(CommandSender sender, String playerName, double weight) {
        if (weight < 0.0) {
            sender.sendMessage(ColorUtil.color("&cWeight cannot be negative!"));
            return;
        }

        BiasManager.BiasEntry entry = plugin.getBiasManager().setBias(playerName, weight);
        sender.sendMessage(ColorUtil.color("&aUpdated coinflip win weight for &e" + entry.getName() + " &ato &e" + weight + "%&a."));
    }

    @Subcommand("remove|delete")
    @CommandCompletion("@players")
    public void onRemove(CommandSender sender, String playerName) {
        boolean removed = plugin.getBiasManager().removeBias(playerName);
        if (removed) {
            sender.sendMessage(ColorUtil.color("&aRemoved coinflip bias for player &e" + playerName + "&a. Coinflips are now standard 50/50."));
        } else {
            sender.sendMessage(ColorUtil.color("&cPlayer &e" + playerName + " &cdoes not have an active bias configured."));
        }
    }

    @Subcommand("check")
    @CommandCompletion("@players")
    public void onCheck(CommandSender sender, String playerName) {
        Double bias = plugin.getBiasManager().getBias(null, playerName);
        if (bias != null) {
            sender.sendMessage(ColorUtil.color("&aPlayer &e" + playerName + " &ahas an active bias of &6" + bias + "%&a win weight."));
        } else {
            sender.sendMessage(ColorUtil.color("&ePlayer &f" + playerName + " &ehas no active bias (standard 50/50)."));
        }
    }

    @Subcommand("list")
    public void onList(CommandSender sender) {
        Collection<BiasManager.BiasEntry> entries = plugin.getBiasManager().getAll();
        if (entries.isEmpty()) {
            sender.sendMessage(ColorUtil.color("&eThere are currently no players with active coinflip bias."));
            return;
        }

        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.color("&6&lConfigured Coinflip Biases (" + entries.size() + "):"));
        for (BiasManager.BiasEntry entry : entries) {
            String winText = entry.getWeight() >= 100.0 ? "&c&lALWAYS WINS" : "&e" + entry.getWeight() + "%";
            sender.sendMessage(ColorUtil.color(" &8- &f" + entry.getName() + " &7(" + (entry.getUuid() != null ? entry.getUuid() : "offline") + "): " + winText));
        }
        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
    }

    @Subcommand("clear")
    public void onClear(CommandSender sender) {
        int count = plugin.getBiasManager().getAll().size();
        plugin.getBiasManager().clear();
        sender.sendMessage(ColorUtil.color("&aCleared &e" + count + " &aconfigured coinflip player biases."));
    }
}
