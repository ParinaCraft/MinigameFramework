package fi.joniaromaa.minigameframework.communication.tcp;

import fi.joniaromaa.minigameframework.net.TcpPacketDataHandler;
import io.netty.buffer.ByteBuf;

public interface TcpIncomingPacket
{
	public void handle(TcpPacketDataHandler handler, ByteBuf buf);
}
