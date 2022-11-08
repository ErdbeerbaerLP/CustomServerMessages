package de.erdbeerbaerlp.customServerMessages;

import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.Validate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CustomStatusPacketListenerImpl extends ServerStatusPacketListenerImpl {
    private static final Component DISCONNECT_REASON = Component.translatable("multiplayer.status.request_handled");
    private final MinecraftServer server;
    private final DedicatedServerProperties props;
    private final Connection connection;
    private boolean hasRequestedStatus;

    public CustomStatusPacketListenerImpl(MinecraftServer mcserver, Connection connection) {
        super(mcserver, connection);
        this.server = mcserver;
        this.connection = connection;
        this.props = ((DedicatedServer) mcserver).getProperties();
    }


    public CustomStatusPacketListenerImpl(DedicatedServerProperties props, Connection connection) {
        super(null, connection);
        this.server = null;
        this.connection = connection;
        this.props = props;
    }

    public void onDisconnect(Component p_10091_) {
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void handleStatusRequest(ServerboundStatusRequestPacket packet) {
        if (this.hasRequestedStatus) {
            this.connection.disconnect(DISCONNECT_REASON);
        } else {
            this.hasRequestedStatus = true;
            ServerStatus status;
            if (!CustomMessageMod.started) {
                status = new ServerStatus();
                final ServerStatus.Players players = new ServerStatus.Players(-1, -1);
                final GameProfile[] playerProfiles = new GameProfile[]{new GameProfile(UUID.randomUUID(), Configuration.instance().messages.startPlayerListTextHover)};
                players.setSample(playerProfiles);
                status.setPlayers(players);
                status.setEnforcesSecureChat(props.enforceSecureProfile);
                status.setVersion(new ServerStatus.Version(Configuration.instance().general.useStartVersion ? Configuration.instance().messages.startPlayerListText.replace("%time%", CustomMessageMod.getEstimatedStartTime()) : SharedConstants.getCurrentVersion().getName(), Configuration.instance().general.useStartVersion ? Integer.MAX_VALUE : SharedConstants.getProtocolVersion()));
                status.setDescription(Component.literal(Configuration.instance().messages.serverStartingMOTD.replace("%time%", CustomMessageMod.getEstimatedStartTime())));
                addServerIcon(status);
            } else {
                status = this.server.getStatus();
                if (Configuration.instance().general.enableCustomMOTD) {
                    status = new ServerStatus();
                    status.setEnforcesSecureChat(props.enforceSecureProfile);
                    final ServerStatus.Players statusPlayers = new ServerStatus.Players(server.getMaxPlayers(), server.getPlayerCount());
                    final GameProfile[] gameProfiles = new GameProfile[]{new GameProfile(UUID.randomUUID(),
                            Configuration.instance().messages.customMOTDPlayerListHover
                                    .replace("%online%", server.getPlayerCount() + "")
                                    .replace("%max%", server.getMaxPlayers() + "")
                                    .replace("%gamemode%", server.getDefaultGameType().getName())
                                    .replace("%playerlist%", getPlayerList())
                                    .replace("%time%", CustomMessageMod.getOverworldTime(false))
                                    .replace("%time-colored%", CustomMessageMod.getOverworldTime(true))
                    )};
                    statusPlayers.setSample(gameProfiles);
                    status.setPlayers(statusPlayers);
                    status.setVersion(new ServerStatus.Version(Configuration.instance().general.useCustomMOTDVersion ?
                            Configuration.instance().messages.customMOTDPlayerListText
                                    .replace("%online%", server.getPlayerCount() + "")
                                    .replace("%max%", server.getMaxPlayers() + "")
                            : SharedConstants.getCurrentVersion().getName(), Configuration.instance().general.useCustomMOTDVersion ? Integer.MAX_VALUE : SharedConstants.getProtocolVersion()));;
                    status.setDescription(Component.literal(Configuration.instance().getRandomMOTD()
                            .replace("%online%", server.getPlayerCount() + "")
                            .replace("%max%", "" + server.getMaxPlayers())
                            .replace("%time%", CustomMessageMod.getOverworldTime(false))
                            .replace("%time-colored%", CustomMessageMod.getOverworldTime(true))));
                    addServerIcon(status);
                }
            }
            this.connection.send(new ClientboundStatusResponsePacket(status));
        }
    }
    private String getPlayerList() {
        final GameProfile[] agameprofile = new GameProfile[Math.min(server.getPlayerCount(), 12)];
        int j = Mth.nextInt(RandomSource.create(), 0, server.getPlayerCount() - agameprofile.length);

        for (int k = 0; k < agameprofile.length; ++k) {
            agameprofile[k] = server.getPlayerList().getPlayers().get(j + k).getGameProfile();
        }

        Collections.shuffle(Arrays.asList(agameprofile));
        String out = "";
        for (GameProfile p : agameprofile) {
            out = out + p.getName();
        }
        return out;
    }
    private void addServerIcon(ServerStatus response) {
        File file1 = new File(Configuration.instance().general.startServerIconPath);
        if (CustomMessageMod.started || !file1.exists()) {
            file1 = new File(Configuration.instance().general.serverIconPath);
        }
        Optional<File> optional = Optional.of(file1).filter(File::isFile);
        optional.ifPresent((p_202470_) -> {
            try {
                BufferedImage bufferedimage = ImageIO.read(p_202470_);
                Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide");
                Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high");
                ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
                ImageIO.write(bufferedimage, "PNG", bytearrayoutputstream);
                byte[] abyte = Base64.getEncoder().encode(bytearrayoutputstream.toByteArray());
                response.setFavicon("data:image/png;base64," + new String(abyte, StandardCharsets.UTF_8));
            } catch (Exception exception) {
                CustomMessageMod.LOGGER.error("Couldn't load server icon", exception);
            }

        });
    }

    public void handlePingRequest(ServerboundPingRequestPacket p_10093_) {
        this.connection.send(new ClientboundPongResponsePacket(p_10093_.getTime()));
        this.connection.disconnect(DISCONNECT_REASON);
    }
}