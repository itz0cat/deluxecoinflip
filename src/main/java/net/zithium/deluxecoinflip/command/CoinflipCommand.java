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
import net.zithium.deluxecoinflip.api.events.CoinflipCreatedEvent;
import net.zithium.deluxecoinflip.config.ConfigType;
import net.zithium.deluxecoinflip.config.Messages;
import net.zithium.deluxecoinflip.economy.EconomyManager;
import net.zithium.deluxecoinflip.economy.provider.EconomyProvider;
import net.zithium.deluxecoinflip.game.CoinflipGame;
import net.zithium.deluxecoinflip.game.GameManager;
import net.zithium.deluxecoinflip.storage.PlayerData;
import net.zithium.deluxecoinflip.utility.TextUtil;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@CommandAlias("coinflip|cf")
@Description("Main command for using DeluxeCoinflip")
public class CoinflipCommand extends BaseCommand {

    private final DeluxeCoinflipPlugin plugin;
    private final EconomyManager economyManager;
    private final GameManager gameManager;

    public CoinflipCommand(final DeluxeCoinflipPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.gameManager = plugin.getGameManager();
    }

    @Default
    public void defaultCommand(final CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Console cannot open the coinflip GUI, use /coinflip help");
            return;
        }

        plugin.getInventoryManager().getGamesGUI().openInventory((Player) sender);
    }

    @Subcommand("reload")
    @CommandPermission("coinflip.reload")
    public void reloadSubCommand(final CommandSender sender) {
        plugin.reload();
        Messages.RELOAD.send(sender);
    }

    @Subcommand("help")
    public void helpSubCommand(final CommandSender sender) {
        Messages.HELP_DEFAULT.send(sender, "{PROVIDERS}", economyManager.getEconomyProviders().values().stream().map(p -> p.getDisplayName().toLowerCase()).collect(Collectors.joining(", ")));
        if (sender.hasPermission("coinflip.admin")) {
            Messages.HELP_ADMIN.send(sender);
            sender.sendMessage(ColorUtil.color("&e/bias <add|set|remove|check|list|clear> &7- Coinflip player win bias controls"));
        }
    }

    @Subcommand("bias add")
    @CommandPermission("coinflip.admin")
    @CommandCompletion("@players")
    public void biasAddSubCommand(final CommandSender sender, String playerName, @Optional Double weight) {
        double finalWeight = (weight != null) ? weight : 100.0;
        if (finalWeight < 0.0) {
            sender.sendMessage(ColorUtil.color("&cWeight cannot be negative!"));
            return;
        }
        var entry = plugin.getBiasManager().setBias(playerName, finalWeight);
        if (finalWeight >= 100.0) {
            sender.sendMessage(ColorUtil.color("&aPlayer &e" + entry.getName() + " &awill now &6&lALWAYS WIN &aall coinflips (weight: &e" + finalWeight + "%&a)."));
        } else {
            sender.sendMessage(ColorUtil.color("&aSet coinflip win weight for &e" + entry.getName() + " &ato &e" + finalWeight + "%&a."));
        }
    }

    @Subcommand("bias set|bias weight")
    @CommandPermission("coinflip.admin")
    @CommandCompletion("@players")
    public void biasSetSubCommand(final CommandSender sender, String playerName, double weight) {
        if (weight < 0.0) {
            sender.sendMessage(ColorUtil.color("&cWeight cannot be negative!"));
            return;
        }
        var entry = plugin.getBiasManager().setBias(playerName, weight);
        sender.sendMessage(ColorUtil.color("&aUpdated coinflip win weight for &e" + entry.getName() + " &ato &e" + weight + "%&a."));
    }

    @Subcommand("bias remove|bias delete")
    @CommandPermission("coinflip.admin")
    @CommandCompletion("@players")
    public void biasRemoveSubCommand(final CommandSender sender, String playerName) {
        boolean removed = plugin.getBiasManager().removeBias(playerName);
        if (removed) {
            sender.sendMessage(ColorUtil.color("&aRemoved coinflip bias for &e" + playerName + "&a. Coinflips are now standard 50/50."));
        } else {
            sender.sendMessage(ColorUtil.color("&cPlayer &e" + playerName + " &cdoes not have an active bias configured."));
        }
    }

    @Subcommand("bias list")
    @CommandPermission("coinflip.admin")
    public void biasListSubCommand(final CommandSender sender) {
        var entries = plugin.getBiasManager().getAll();
        if (entries.isEmpty()) {
            sender.sendMessage(ColorUtil.color("&eThere are currently no players with active coinflip bias."));
            return;
        }
        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.color("&6&lConfigured Coinflip Biases (" + entries.size() + "):"));
        for (var entry : entries) {
            String winText = entry.getWeight() >= 100.0 ? "&c&lALWAYS WINS" : "&e" + entry.getWeight() + "%";
            sender.sendMessage(ColorUtil.color(" &8- &f" + entry.getName() + " &7(" + (entry.getUuid() != null ? entry.getUuid() : "offline") + "): " + winText));
        }
        sender.sendMessage(ColorUtil.color("&8&m----------------------------------------"));
    }

    @Subcommand("bias clear")
    @CommandPermission("coinflip.admin")
    public void biasClearSubCommand(final CommandSender sender) {
        int count = plugin.getBiasManager().getAll().size();
        plugin.getBiasManager().clear();
        sender.sendMessage(ColorUtil.color("&aCleared &e" + count + " &aconfigured coinflip player biases."));
    }

    @Subcommand("bias check")
    @CommandPermission("coinflip.admin")
    @CommandCompletion("@players")
    public void biasCheckSubCommand(final CommandSender sender, String playerName) {
        Double bias = plugin.getBiasManager().getBias(null, playerName);
        if (bias != null) {
            sender.sendMessage(ColorUtil.color("&aPlayer &e" + playerName + " &ahas an active bias of &6" + bias + "%&a win weight."));
        } else {
            sender.sendMessage(ColorUtil.color("&ePlayer &f" + playerName + " &ehas no active bias (standard 50/50)."));
        }
    }

    @Subcommand("about")
    public void aboutSubCommand(final CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ColorUtil.color("&e&lDeluxeCoinflip"));
        sender.sendMessage(ColorUtil.color("&eVersion: &fv" + plugin.getDescription().getVersion()));
        sender.sendMessage(ColorUtil.color("&eAuthor: &fItsLewizzz"));

        if (!TextUtil.isValidDownload()) {
            sender.sendMessage(ColorUtil.color("&4Registered to: &cFailed to find licensed owner to this plugin. Contact developer to report possible leak (ItsLewizzz#6023)."));
        } else if (TextUtil.isMCMarket()) {
            sender.sendMessage(ColorUtil.color("&4Registered to: &chttps://www.mc-market.org/members/%%__USER__%%/"));
        } else {
            sender.sendMessage(ColorUtil.color("&4Registered to: &chttps://www.spigotmc.org/members/%%__USER__%%/"));
        }
        sender.sendMessage("");
    }

    @Subcommand("toggle")
    public void toggleSubCommand(final CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can toggle broadcast messages");
            return;
        }

        Player player = (Player) sender;
        java.util.Optional<PlayerData> playerDataOptional = plugin.getStorageManager().getPlayer(player.getUniqueId());

        if(playerDataOptional.isEmpty()) {
            sender.sendMessage(ColorUtil.color("&cYour player data has not loaded yet, please wait a few moments or relog."));
            return;
        }

        PlayerData playerData = playerDataOptional.get();
        if (playerData.isDisplayBroadcastMessages()) {
            Messages.BROADCASTS_TOGGLED_OFF.send(player);
            playerData.setDisplayBroadcastMessages(false);
        } else {
            Messages.BROADCASTS_TOGGLED_ON.send(player);
            playerData.setDisplayBroadcastMessages(true);
        }
    }

    @Subcommand("delete|remove")
    public void deleteSubCommand(final CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can remove a coinflip game");
            return;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        if (gameManager.getCoinflipGames().containsKey(uuid)) {
            final CoinflipGame game = gameManager.getCoinflipGames().get(uuid);

            economyManager.getEconomyProvider(game.getProvider()).deposit(player, game.getAmount());
            gameManager.removeCoinflipGame(uuid);
            Messages.DELETED_GAME.send(player);

        } else {
            Messages.GAME_NOT_FOUND.send(player);
        }
    }

    @Subcommand("create|new")
    //@WrongUsage("&c/coinflip create <amount> [economy]")
    @CommandCompletion("* @providers")
    public void createSubCommand(final Player player, String amountInput, @Optional String currencyProvider) {
        final long amount;
        try {
            amount = Long.parseLong(amountInput.replace(",", ""));
        } catch (Exception ex) {
            Messages.INVALID_AMOUNT.send(player, "{INPUT}", amountInput);
            return;
        }

        if (gameManager.getCoinflipGames().containsKey(player.getUniqueId())) {
            Messages.GAME_ACTIVE.send(player);
            return;
        }

        final FileConfiguration config = plugin.getConfigHandler(ConfigType.CONFIG).getConfig();

        if (amount > config.getLong("settings.maximum-bet")) {
            Messages.CREATE_MAXIMUM_AMOUNT.send(player, "{MAX_BET}", TextUtil.numberFormat(config.getLong("settings.maximum-bet")));
            return;
        }

        if (amount < config.getLong("settings.minimum-bet")) {
            Messages.CREATE_MINIMUM_AMOUNT.send(player,"{MIN_BET}", TextUtil.numberFormat(config.getLong("settings.minimum-bet")));
            return;
        }

        final List<EconomyProvider> providers = new ArrayList<>(economyManager.getEconomyProviders().values());
        if (providers.isEmpty()) {
            player.sendMessage(ColorUtil.color("&cThere are no economy providers found or not enabled in the configuration. Please contact an administrator."));
            return;
        }

        EconomyProvider provider = null;
        if (currencyProvider == null) {
            if (providers.size() == 1) {
                provider = providers.get(0);
            } else {
                String defaultProvider = config.getString("settings.providers.default_provider");
                if (defaultProvider != null && !defaultProvider.isEmpty()) {
                    provider = economyManager.getEconomyProviders().get(defaultProvider);
                }
            }
        } else {
            provider = getProviderByName(currencyProvider);
        }

        if (provider == null) {
            Messages.INVALID_CURRENCY.send(player,"{CURRENCY_TYPES}", economyManager.getEconomyProviders().values().stream().map(p -> p.getDisplayName().toLowerCase()).collect(Collectors.joining(", ")));
            return;
        }

        if (amount <= provider.getBalance(player)) {
            CoinflipGame coinflipGame = new CoinflipGame(player.getUniqueId(), provider.getIdentifier().toUpperCase(), amount);

            final CoinflipCreatedEvent event = new CoinflipCreatedEvent(player, coinflipGame);
            Bukkit.getPluginManager().callEvent(event);
            if(event.isCancelled()) return;

            provider.withdraw(player, amount);
            gameManager.addCoinflipGame(player.getUniqueId(), coinflipGame);

            if(config.getBoolean("settings.broadcast-coinflip-creation")) {
                Messages.COINFLIP_CREATED_BROADCAST.broadcast("{PLAYER}", player.getName(), "{CURRENCY}", provider.getDisplayName(), "{AMOUNT}", TextUtil.numberFormat(amount));
            }

            Messages.CREATED_GAME.send(player, "{CURRENCY}", provider.getDisplayName(), "{AMOUNT}", TextUtil.numberFormat(amount));
        } else {
            Messages.INSUFFICIENT_FUNDS.send(player);
        }
    }

    private EconomyProvider getProviderByName(String name) {
        for (EconomyProvider provider : economyManager.getEconomyProviders().values()) {
            if (name.equalsIgnoreCase(provider.getDisplayName())) return provider;
        }
        return null;
    }
}