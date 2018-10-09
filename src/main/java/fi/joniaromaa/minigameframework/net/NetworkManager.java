package fi.joniaromaa.minigameframework.net;

import java.util.concurrent.TimeUnit;

import fi.joniaromaa.minigameframework.api.game.Minigame;
import fi.joniaromaa.minigameframework.api.game.PreMinigameStatus;
import fi.joniaromaa.minigameframework.communication.tcp.TcpOutgoingPacket;
import fi.joniaromaa.minigameframework.communication.tcp.TcpPacketManager;
import fi.joniaromaa.minigameframework.communication.tcp.outgoing.RegisterGameOutgoingPacket;
import fi.joniaromaa.minigameframework.game.AbstractMinigame;
import fi.joniaromaa.minigameframework.game.AbstractPreMinigame;
import fi.joniaromaa.parinacorelibrary.api.ParinaCore;
import fi.joniaromaa.parinacorelibrary.bukkit.config.BukkitParinaCoreConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;

public class NetworkManager
{
	@Getter private Minigame minigame;
	@Getter private TcpPacketManager tcpPacketManager;
	
	private EventLoopGroup bossGroup;
	private Channel channel;
	
	public NetworkManager(Minigame minigame)
	{
		this.minigame = minigame;
		this.tcpPacketManager = new TcpPacketManager();
		
		this.bossGroup = new NioEventLoopGroup(1);
	}
	
	public void start() throws InterruptedException
	{
		if (this.minigame instanceof AbstractPreMinigame)
		{
			AbstractPreMinigame preMinigame = (AbstractPreMinigame)this.minigame;
			if (preMinigame.getStatus() == PreMinigameStatus.GAME_STARTED)
			{
				return;
			}
		}

		this.channel = null; //Block sending packets
		
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(this.bossGroup)
		.channel(NioSocketChannel.class)
		.option(ChannelOption.TCP_NODELAY, true)
		.handler(new ChannelInitializer<SocketChannel>()
		{
			@Override
			protected void initChannel(SocketChannel socketChannel) throws Exception
			{
				//Incoming
				socketChannel.pipeline().addLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS));
				socketChannel.pipeline().addLast(new IdleStateHandler(0, 1, 0));
				socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(Short.MAX_VALUE, 0, 2, 0, 2));
				socketChannel.pipeline().addLast(new TcpPacketDataHandler(NetworkManager.this));
				
				//Outgoing
				socketChannel.pipeline().addLast(new LengthFieldPrepender(2));
				socketChannel.pipeline().addLast(new TcpOutgoingPackerEncoder());
			}
		});

		bootstrap.connect("127.0.0.1", 7649).addListener(new GenericFutureListener<ChannelFuture>()
		{
			@Override
			public void operationComplete(ChannelFuture future) throws Exception
			{
				NetworkManager.this.channel = future.channel();
				
				if (NetworkManager.this.minigame instanceof AbstractPreMinigame)
				{
					AbstractPreMinigame preMinigame = (AbstractPreMinigame)NetworkManager.this.minigame;
					
					NetworkManager.this.channel.writeAndFlush(new RegisterGameOutgoingPacket(((BukkitParinaCoreConfig)ParinaCore.getApi().getConfig()).getServerId(), preMinigame.getConfig().getGameType(), preMinigame.getPlayersLimit(), preMinigame.getPlayersUniqueIds()));
				}
				else if (NetworkManager.this.minigame instanceof AbstractMinigame)
				{
					AbstractMinigame<?, ?> minigame = (AbstractMinigame<?, ?>)NetworkManager.this.minigame;
					
					NetworkManager.this.channel.writeAndFlush(new RegisterGameOutgoingPacket(((BukkitParinaCoreConfig)ParinaCore.getApi().getConfig()).getServerId(), minigame.getConfig().getGameType(), minigame.getAlivePlayerUniqueIds()));
				}
			}
		});
	}
	
	public void stop()
	{
		NetworkManager.this.bossGroup.shutdownGracefully();
	}
	
	public void sendPacket(TcpOutgoingPacket packet)
	{
		Channel channel = this.channel;
		if (channel != null)
		{
			channel.writeAndFlush(packet);
		}
	}
}
