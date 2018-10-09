package fi.joniaromaa.minigameframework.communication.tcp.incoming;

import fi.joniaromaa.minigameframework.communication.tcp.TcpIncomingPacket;
import fi.joniaromaa.minigameframework.net.TcpPacketDataHandler;
import io.netty.buffer.ByteBuf;

public class GameRegistedIncomingPacket implements TcpIncomingPacket
{
	@Override
	public void handle(TcpPacketDataHandler handler, ByteBuf buf)
	{
	}
}
