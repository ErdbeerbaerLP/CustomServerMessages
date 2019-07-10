package de.erdbeerbaerlp.customServerMessages;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.LegacyPingHandler;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.NettyVarint21FrameDecoder;
import net.minecraft.network.NettyVarint21FrameEncoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.Packet;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.ServerStatusResponse.Players;
import net.minecraft.network.ServerStatusResponse.Version;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.server.SPacketDisconnect;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.status.server.SPacketServerInfo;
import net.minecraft.server.network.NetHandlerHandshakeTCP;
import net.minecraft.util.LazyLoadBase;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class CustomNetworkSystem extends NetworkSystem {

	public CustomNetworkSystem() {
		super(FMLCommonHandler.instance().getMinecraftServerInstance());
	}
	private static int version;
	public void addLanEndpoint(InetAddress address, int port) throws IOException
	{
		if (address instanceof java.net.Inet6Address) System.setProperty("java.net.preferIPv4Stack", "false");
		synchronized (this.endpoints)
		{
			Class <? extends ServerSocketChannel > oclass;
			LazyLoadBase <? extends EventLoopGroup > lazyloadbase;

			if (Epoll.isAvailable() && this.mcServer.shouldUseNativeTransport())
			{
				oclass = EpollServerSocketChannel.class;
				lazyloadbase = SERVER_EPOLL_EVENTLOOP;
				LOGGER.info("Using epoll channel type");
			}
			else
			{
				oclass = NioServerSocketChannel.class;
				lazyloadbase = SERVER_NIO_EVENTLOOP;
				LOGGER.info("Using default channel type");
			}

			this.endpoints.add(((ServerBootstrap)((ServerBootstrap)(new ServerBootstrap()).channel(oclass)).childHandler(new ChannelInitializer<Channel>()
			{
				protected void initChannel(Channel p_initChannel_1_) throws Exception
				{
					try
					{
						p_initChannel_1_.config().setOption(ChannelOption.TCP_NODELAY, Boolean.valueOf(true));
					}
					catch (ChannelException var3)
					{
						;
					}

					p_initChannel_1_.pipeline().addLast("timeout", new ReadTimeoutHandler(net.minecraftforge.fml.common.network.internal.FMLNetworkHandler.READ_TIMEOUT)).addLast("legacy_query", new LegacyPingHandler(CustomNetworkSystem.this)).addLast("splitter", new NettyVarint21FrameDecoder()).addLast("decoder", new NettyPacketDecoder(EnumPacketDirection.SERVERBOUND)).addLast("prepender", new NettyVarint21FrameEncoder()).addLast("encoder", new NettyPacketEncoder(EnumPacketDirection.CLIENTBOUND));
					NetworkManager networkmanager = new NetworkManager(EnumPacketDirection.SERVERBOUND) {

						@Override
						public void sendPacket(Packet<?> packetIn) {
							packetIn = CustomNetworkSystem.modifyPacket(packetIn);
							super.sendPacket(packetIn);
						}
						@Override
						public void sendPacket(Packet<?> packetIn,
								GenericFutureListener<? extends Future<? super Void>> listener,
										@SuppressWarnings("unchecked") GenericFutureListener<? extends Future<? super Void>>... listeners) {
							packetIn = CustomNetworkSystem.modifyPacket(packetIn);
							super.sendPacket(packetIn, listener, listeners);
						}
					};
					CustomNetworkSystem.this.networkManagers.add(networkmanager);
					p_initChannel_1_.pipeline().addLast("packet_handler", networkmanager);
					networkmanager.setNetHandler(new NetHandlerHandshakeTCP(CustomNetworkSystem.this.mcServer, networkmanager) {
						@Override
						public void processHandshake(C00Handshake c) {
							if(CustomServerMessagesMod.serverStarted) {
								if(c.getRequestedState() == EnumConnectionState.STATUS && CustomMessages.CUSTOM_MOTD_ENABLED) {
									if(MinecraftForge.MC_VERSION.equals("1.12.2"))
										version = 340;
									else if(MinecraftForge.MC_VERSION.equals("1.12.1"))
										version = 338;
									else if(MinecraftForge.MC_VERSION.equals("1.12"))
										version = 335;
									else
										version = 0;
									final ServerStatusResponse statusResp = server.getServerStatusResponse();
									Players statusPlayers = new Players(server.getMaxPlayers(), server.getCurrentPlayerCount());
									GameProfile[] playersIn = new GameProfile[]{new GameProfile(UUID.randomUUID(), 
											CustomMessages.CUSTOM_MOTD_PLAYER_HOVER
											.replace("%online%", server.getCurrentPlayerCount()+"")
											.replace("%max%", server.getMaxPlayers()+"")
											.replace("%gamemode%", server.getGameType().getName())
											.replace("%playerlist%", getPlayerList())
											)};
									statusPlayers.setPlayers(playersIn);
									statusResp.setVersion(new Version(CustomMessages.CUSTOM_MOTD_USE_VERSION?CustomMessages.CUSTOM_MOTD_VERSION:MinecraftForge.MC_VERSION, CustomMessages.CUSTOM_MOTD_USE_VERSION?Integer.MAX_VALUE:version));
									statusResp.setPlayers(statusPlayers);
									statusResp.setServerDescription(new TextComponentString(CustomMessages.getRandomMOTD().replace("%online%", server.getCurrentPlayerCount()+"").replace("%max%", ""+server.getMaxPlayers())));
									server.applyServerIconToResponse(statusResp);
									super.processHandshake(c);
								}else if(c.getRequestedState() != EnumConnectionState.STATUS) super.processHandshake(c);
							}
							else
							{
								if(MinecraftForge.MC_VERSION.equals("1.12.2"))
									version = 340;
								else if(MinecraftForge.MC_VERSION.equals("1.12.1"))
									version = 338;
								else if(MinecraftForge.MC_VERSION.equals("1.12"))
									version = 335;
								else
									version = 0;
								final ServerStatusResponse statusResp = new ServerStatusResponse();
								Players statusPlayers = new Players(-1, -1);
								GameProfile[] playersIn = new GameProfile[]{new GameProfile(UUID.randomUUID(), CustomMessages.START_VERSION_HOVER)};
								statusPlayers.setPlayers(playersIn);
								statusResp.setVersion(new Version(CustomMessages.USE_VERSION?CustomMessages.START_VERSION:MinecraftForge.MC_VERSION, CustomMessages.USE_VERSION?Integer.MAX_VALUE:version));
								statusResp.setPlayers(statusPlayers);
								statusResp.setServerDescription(new TextComponentString(CustomMessages.START_MOTD));
								if(CustomMessages.LOG_START_DISCONNECT) FMLLog.log.info("Disconnecting Player: Server is starting");
								TextComponentString startKick = new TextComponentString(CustomMessages.START_KICK_MSG);
								if(c.getRequestedState() == EnumConnectionState.STATUS) networkManager.sendPacket(new SPacketServerInfo(statusResp));
								else if(c.getRequestedState() == EnumConnectionState.LOGIN) networkManager.sendPacket(new SPacketDisconnect(startKick));
								else System.out.println("Invalid state: "+c.getRequestedState());
								networkManager.closeChannel(startKick);
							}
						}

						private String getPlayerList() {
							final GameProfile[] agameprofile = new GameProfile[Math.min(server.getCurrentPlayerCount(), 12)];
							int j = MathHelper.getInt(new Random(), 0, server.getCurrentPlayerCount() - agameprofile.length);

							for (int k = 0; k < agameprofile.length; ++k)
							{
								agameprofile[k] = ((EntityPlayerMP)server.getPlayerList().getPlayers().get(j + k)).getGameProfile();
							}

							Collections.shuffle(Arrays.asList(agameprofile));
							String out = "";
							for(GameProfile p : agameprofile) {
								out = out + p.getName();
							}
							return out;
						}
					});
				}
			}).group(lazyloadbase.getValue()).localAddress(address, port)).bind().syncUninterruptibly());
		}
	}
	private static boolean nextTimeout = false;
	private static Packet<?> modifyPacket(Packet<?> packetIn) {
		if(packetIn instanceof SPacketChat) {
			final SPacketChat msg = (SPacketChat) packetIn;
			final ITextComponent com = ObfuscationReflectionHelper.getPrivateValue(SPacketChat.class, msg, "chatComponent", "field_148919_a", "a");
			if(com instanceof TextComponentTranslation) {
				final TextComponentTranslation tct = (TextComponentTranslation) com;
				final String player = tct.getUnformattedText().split(" ")[0];
				if(tct.getKey().startsWith("multiplayer.player.left")){
					final TextComponentString leaveMsg = new TextComponentString((nextTimeout?CustomMessages.LEAVE_MSG_TIMEOUT:CustomMessages.LEAVE_MSG).replace("%player%", player));
					leaveMsg.setStyle(tct.getStyle());
					nextTimeout = false;
					return new SPacketChat(leaveMsg, msg.getType());

				}
				if(tct.getKey().startsWith("multiplayer.player.joined")){
					final TextComponentString joinMsg = new TextComponentString(CustomMessages.JOIN_MSG.replace("%player%", player));
					joinMsg.setStyle(tct.getStyle());
					return new SPacketChat(joinMsg, msg.getType());
				}
			}
		}
		if(packetIn instanceof net.minecraft.network.play.server.SPacketDisconnect) {
			final net.minecraft.network.play.server.SPacketDisconnect p = (net.minecraft.network.play.server.SPacketDisconnect) packetIn;
			if(ObfuscationReflectionHelper.getPrivateValue(net.minecraft.network.play.server.SPacketDisconnect.class, p, "reason", "field_149167_a", "a") instanceof TextComponentTranslation) {
				final TextComponentTranslation tct = (TextComponentTranslation) ObfuscationReflectionHelper.getPrivateValue(net.minecraft.network.play.server.SPacketDisconnect.class, p, "reason", "field_149167_a", "a");
				System.out.println(tct.getKey());
				if(tct.getKey().startsWith("multiplayer.disconnect.server_shutdown")){
					return new net.minecraft.network.play.server.SPacketDisconnect(new TextComponentString(CustomMessages.STOP_MSG));
				}
			}
		}
		return packetIn;
	}
}
