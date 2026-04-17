package org.stepan1411.pvp_bot.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import org.stepan1411.pvp_bot.PvpBotPlugin;

import javax.annotation.Nullable;
import java.util.UUID;

public class PaperBotSpawner {

    public static ServerPlayer spawnBot(String name, MinecraftServer server, ServerLevel level,
                                        double x, double y, double z, float yaw, float pitch, String gamemode) {
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), name);
            ServerPlayer player = new ServerPlayer(server, level, profile, net.minecraft.server.network.ClientInformation.createDefault());

            Connection conn = new Connection(PacketFlow.SERVERBOUND);
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
            ServerGamePacketListenerImpl handler = new ServerGamePacketListenerImpl(server, conn, player, cookie) {
                @Override
                public void send(Packet<?> packet) { /* no-op */ }
                @Override
                public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
                    if (listener != null) listener.onSuccess();
                }
            };
            player.connection = handler;
            player.moveTo(x, y, z, yaw, pitch);

            GameType gameType = GameType.SURVIVAL;
            try { gameType = GameType.byName(gamemode, GameType.SURVIVAL); } catch (Exception ignored) {}
            player.setGameMode(gameType);

            server.getPlayerList().placeNewPlayer(conn, player, cookie);
            return player;
        } catch (Exception e) {
            PvpBotPlugin.LOGGER.error("Failed to spawn bot {}: {}", name, e.getMessage());
            return null;
        }
    }

    public static void removeBot(ServerPlayer player) {
        try {
            MinecraftServer server = player.getServer();
            if (server != null) {
                server.getPlayerList().remove(player);
            }
        } catch (Exception e) {
            PvpBotPlugin.LOGGER.error("Failed to remove bot: " + e.getMessage());
        }
    }
}
