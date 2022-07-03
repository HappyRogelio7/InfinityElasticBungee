package com.github.kaorystudios.mc.happyrogelio7.infinityelasticbungee.sync;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.github.kaorystudios.mc.happyrogelio7.infinityelasticbungee.InfinityElasticBungee;
import com.github.kaorystudios.mc.happyrogelio7.infinityelasticbungee.sync.results.PlayerSyncResult;
import com.github.kaorystudios.mc.happyrogelio7.infinityelasticbungee.utils.MessageUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerSync implements Listener {

    private static final String QUERY_PREFIX = "eb_user_";

    private final InfinityElasticBungee plugin;

    public PlayerSync(final InfinityElasticBungee plugin) {
        this.plugin = plugin;

        plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
            @Override
            public void run() {
                sync();
            }
        }, 1, 40, TimeUnit.SECONDS);
    }

    private void updatePlayerData(final String username, final String server, final String address) {
        final String key = QUERY_PREFIX + username.toLowerCase();

        if (server == null || server.isEmpty()) {
            this.plugin.getStorage().delete(key);
            return;
        }

        final PlayerSyncResult result = new PlayerSyncResult(this.plugin.getServerID(), server, address);
        this.plugin.getStorage().set(key, result.toString(), 60);
    }

    public void syncPlayer(final ProxiedPlayer player, final String server) {
        final String username = player.getName();
        final String address = ((InetSocketAddress) player.getSocketAddress()).getAddress().toString();

        this.updatePlayerData(username, server, address);
    }

    public void syncPlayer(final ProxiedPlayer player) {
        this.syncPlayer(player, player.getServer().getInfo().getName());
    }

    public void sync() {
        for (final ProxiedPlayer player : this.plugin.getProxy().getPlayers()) {
            this.syncPlayer(player);
        }
    }

    public void cleanup() {
        for (final ProxiedPlayer player : this.plugin.getProxy().getPlayers()) {
            this.updatePlayerData(player.getName(), null, null);
        }
    }

    public PlayerSyncResult getPlayer(final String username) {
        final String data = this.plugin.getStorage().get(QUERY_PREFIX + username.toLowerCase());
        if (data != null) {
            return PlayerSyncResult.fromString(data);
        } else {
            return null;
        }
    }

    @EventHandler
    public void onPlayerPreLogin(final PreLoginEvent e) {
        if (!this.plugin.getConfig().getBoolean("settings.prevent-existing-connection-another-proxy", false)) {
            return;
        }

        if (this.getPlayer(e.getConnection().getName()) != null) {
            final BaseComponent[] reason = MessageUtils
                    .format(this.plugin.getConfig().getString("messages.already-connected"));
            e.setCancelReason(reason);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onServerConnected(final ServerConnectedEvent e) {
        this.syncPlayer(e.getPlayer(), e.getServer().getInfo().getName());
    }

    @EventHandler
    public void onPlayerQuit(final PlayerDisconnectEvent e) {
        this.updatePlayerData(e.getPlayer().getName(), null, null);
    }

    @EventHandler
    public void onPlayerKick(final ServerKickEvent e) {
        this.updatePlayerData(e.getPlayer().getName(), null, null);
    }
}

