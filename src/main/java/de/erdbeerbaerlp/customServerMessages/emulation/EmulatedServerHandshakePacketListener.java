package de.erdbeerbaerlp.customServerMessages.emulation;

import de.erdbeerbaerlp.customServerMessages.Configuration;
import de.erdbeerbaerlp.customServerMessages.CustomMessageMod;
import de.erdbeerbaerlp.customServerMessages.CustomStatusPacketListenerImpl;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.dedicated.DedicatedServerProperties;

public class EmulatedServerHandshakePacketListener implements ServerHandshakePacketListener {
   private static final Component IGNORE_STATUS_REASON = Component.literal("Ignoring status request");
   private final Connection connection;
   private final DedicatedServerProperties props;

   public EmulatedServerHandshakePacketListener(Connection p_9970_, DedicatedServerProperties props) {
      this.connection = p_9970_;
      this.props = props;
   }

   public void handleIntention(ClientIntentionPacket packet) {
      switch (packet.getIntention()) {
      case LOGIN:
         this.connection.setProtocol(ConnectionProtocol.LOGIN);
         if (packet.getProtocolVersion() != SharedConstants.getCurrentVersion().getProtocolVersion()) {
            Component component;
            if (packet.getProtocolVersion() < 754) {
               component = Component.translatable("multiplayer.disconnect.outdated_client", SharedConstants.getCurrentVersion().getName());
            } else {
               component = Component.translatable("multiplayer.disconnect.incompatible", SharedConstants.getCurrentVersion().getName());
            }
            this.connection.send(new ClientboundLoginDisconnectPacket(component));
            this.connection.disconnect(component);
         } else {
               MutableComponent component = Component.literal(Configuration.instance().messages.serverStartingKickMessage);
               CustomMessageMod.LOGGER.info("Disconnecting Player (server is still starting): {}", component.getContents());
               connection.send(new ClientboundLoginDisconnectPacket(component));
               connection.disconnect(component);
         }
         break;
      case STATUS:
         if (props.enableStatus) {
            this.connection.setProtocol(ConnectionProtocol.STATUS);
            this.connection.setListener(new CustomStatusPacketListenerImpl(props, this.connection));
         } else {
            this.connection.disconnect(IGNORE_STATUS_REASON);
         }
         break;
      default:
         throw new UnsupportedOperationException("Invalid intention " + packet.getIntention());
   }

   }

   public void onDisconnect(Component p_9973_) {
   }

   public Connection getConnection() {
      return this.connection;
   }
}
