package fi.joniaromaa.minigameframework.communication.tcp.outgoing;

import fi.joniaromaa.minigameframework.communication.tcp.TcpOutgoingPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class UpdateGameOutgoingPacket implements TcpOutgoingPacket
{
	private static final int PACKET_ID = 1;
	
	@Override
	public ByteBuf getBytes()
	{
		ByteBuf buf = Unpooled.buffer();
		buf.writeShort(UpdateGameOutgoingPacket.PACKET_ID);
		return buf;
	}
}
