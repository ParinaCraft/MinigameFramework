package fi.joniaromaa.minigameframework.net;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import fi.joniaromaa.minigameframework.communication.tcp.TcpOutgoingPacket;
import fi.joniaromaa.minigameframework.communication.tcp.outgoing.UpdateGameOutgoingPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.TimeoutException;
import lombok.Getter;

public class TcpPacketDataHandler extends SimpleChannelInboundHandler<ByteBuf>
{
	@Getter private NetworkManager networkManager;
	@Getter private Channel channel;
	
	public TcpPacketDataHandler(NetworkManager networkManager)
	{
		this.networkManager = networkManager;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx)
	{
		this.channel = ctx.channel();
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception
	{
		this.networkManager.getTcpPacketManager().handleIncoming(this, msg);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object event)
	{
		if (event instanceof IdleStateEvent)
		{
			ctx.channel().writeAndFlush(new UpdateGameOutgoingPacket());
		}
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
	{
		ctx.channel().eventLoop().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					TcpPacketDataHandler.this.networkManager.start();
				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}
			}
		}, 1, TimeUnit.SECONDS);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		try
		{
			if (cause instanceof IOException)
			{
				if (cause.getMessage().startsWith("An existing connection was forcibly closed by the remote host"))
				{
					System.out.println("An existing connection was forcibly closed by the remote host, disconnecting");
					
					return;
				}
				else if (cause.getMessage().startsWith("Connection reset by peer"))
				{
					System.out.println("Connection reset by peer, disconnecting");
					
					return;
				}
			}
			else if (cause instanceof TimeoutException)
			{
				System.out.println("Timeout, disconnecting");
				
				return;
			}
			else if (cause instanceof TooLongFrameException)
			{
				System.out.println("Received too long frame, disconnecting");
				
				return;
			}
			
			cause.printStackTrace();
		}
		finally
		{
			ctx.close();
		}
	}
	
	public void sendPacket(TcpOutgoingPacket packet)
	{
		this.channel.writeAndFlush(packet);
	}
}
