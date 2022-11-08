package de.erdbeerbaerlp.customServerMessages.emulation;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.network.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.network.LegacyQueryHandler;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.util.LazyLoadedValue;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static de.erdbeerbaerlp.customServerMessages.CustomMessageMod.LOGGER;

public class EmulatedServerConnectionListener extends ServerConnectionListener {
    private static final int READ_TIMEOUT = Integer.parseInt(System.getProperty("forge.readTimeout", "30"));
    public static final LazyLoadedValue<NioEventLoopGroup> SERVER_EVENT_GROUP = new LazyLoadedValue<>(() -> {
        return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Server IO #%d").setDaemon(true).setThreadFactory(net.minecraftforge.fml.util.thread.SidedThreadGroups.SERVER).build());
    });
    public static final LazyLoadedValue<EpollEventLoopGroup> SERVER_EPOLL_EVENT_GROUP = new LazyLoadedValue<>(() -> {
        return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Server IO #%d").setDaemon(true).setThreadFactory(net.minecraftforge.fml.util.thread.SidedThreadGroups.SERVER).build());
    });
    public volatile boolean running;
    private final List<ChannelFuture> channels = Collections.synchronizedList(Lists.newArrayList());
    final List<Connection> connections = Collections.synchronizedList(Lists.newArrayList());
    private final DedicatedServerProperties props;

    public EmulatedServerConnectionListener(DedicatedServerProperties props) {
        super(null);
        this.props = props;
        this.running = true;
    }

    public void startTcpServerListener(@Nullable InetAddress addr, int port) throws IOException {
        if (addr == null) addr = new java.net.InetSocketAddress(port).getAddress();
        net.minecraftforge.network.DualStackUtils.checkIPv6(addr);
        synchronized(this.channels) {
            Class<? extends ServerSocketChannel> oclass;
            LazyLoadedValue<? extends EventLoopGroup> lazyloadedvalue;
            if (Epoll.isAvailable() && props.useNativeTransport) {
                oclass = EpollServerSocketChannel.class;
                lazyloadedvalue = SERVER_EPOLL_EVENT_GROUP;
                LOGGER.info("Using epoll channel type");
            } else {
                oclass = NioServerSocketChannel.class;
                lazyloadedvalue = SERVER_EVENT_GROUP;
                LOGGER.info("Using default channel type");
            }

            this.channels.add((new ServerBootstrap()).channel(oclass).childHandler(new ChannelInitializer<Channel>() {
                protected void initChannel(Channel channel) {
                    try {
                        channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                    } catch (ChannelException ignored) {
                    }

                    channel.pipeline().addLast("timeout", new ReadTimeoutHandler(READ_TIMEOUT)).addLast("legacy_query", new LegacyQueryHandler(EmulatedServerConnectionListener.this)).addLast("splitter", new Varint21FrameDecoder()).addLast("decoder", new PacketDecoder(PacketFlow.SERVERBOUND)).addLast("prepender", new Varint21LengthFieldPrepender()).addLast("encoder", new PacketEncoder(PacketFlow.CLIENTBOUND));
                    int i = EmulatedServerConnectionListener.this.props.rateLimitPacketsPerSecond;
                    Connection connection = (Connection)(i > 0 ? new RateKickingConnection(i) : new Connection(PacketFlow.SERVERBOUND));
                    EmulatedServerConnectionListener.this.connections.add(connection);
                    channel.pipeline().addLast("packet_handler", connection);
                    connection.setListener(new EmulatedServerHandshakePacketListener(connection, props));
                }
            }).group(lazyloadedvalue.get()).localAddress(addr, port).bind().syncUninterruptibly());
        }
        tickThread.start();
    }

    public void stop() {
        this.running = false;

        for(ChannelFuture channelfuture : this.channels) {
            try {
                channelfuture.channel().close().sync();
            } catch (InterruptedException interruptedexception) {
                LOGGER.error("Interrupted whilst closing channel");
            }
        }
        tickThread.interrupt();

    }

    public void tick() {
        synchronized(this.connections) {
            Iterator<Connection> iterator = this.connections.iterator();
            while(iterator.hasNext()) {
                Connection connection = iterator.next();
                if (!connection.isConnecting()) {
                    if (connection.isConnected()) {
                        try {
                            connection.tick();
                        } catch (Exception exception) {
                            if (connection.isMemoryConnection()) {
                                throw new ReportedException(CrashReport.forThrowable(exception, "Ticking memory connection"));
                            }

                            LOGGER.warn("Failed to handle packet for {}", connection.getRemoteAddress(), exception);
                            Component component = Component.literal("Internal server error");
                            connection.send(new ClientboundDisconnectPacket(component), PacketSendListener.thenRun(() -> {
                                connection.disconnect(component);
                            }));
                            connection.setReadOnly();
                        }
                    } else {
                        iterator.remove();
                        connection.handleDisconnection();
                    }
                }
            }

        }
    }

    public List<Connection> getConnections() {
        return this.connections;
    }




    private final TickThread tickThread = new TickThread(this);
    private static class TickThread extends Thread {
        private EmulatedServerConnectionListener l;

        public TickThread(EmulatedServerConnectionListener l){
            this.l = l;
            setDaemon(true);
            setName("Early Server Connection Listener");
        }

        @Override
        public void run() {
            while(true){
                try {
                    l.tick();
                    sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

}
