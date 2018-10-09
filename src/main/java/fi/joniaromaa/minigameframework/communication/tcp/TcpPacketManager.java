package fi.joniaromaa.minigameframework.communication.tcp;

import java.util.HashMap;

import fi.joniaromaa.minigameframework.communication.tcp.incoming.GameRegistedIncomingPacket;
import fi.joniaromaa.minigameframework.communication.tcp.incoming.RequestPlayerSlotsIncomingPacket;
import fi.joniaromaa.minigameframework.communication.tcp.incoming.RequestSpectateIncomingPacket;
import fi.joniaromaa.minigameframework.communication.tcp.incoming.UpdateGameOKIncomingPacket;
import fi.joniaromaa.minigameframework.net.TcpPacketDataHandler;
import io.netty.buffer.ByteBuf;

public class TcpPacketManager
{
	private HashMap<Integer, TcpIncomingPacket> incomingPackets = new HashMap<>();
	
	public TcpPacketManager()
	{
		this.incomingPackets.put(0, new GameRegistedIncomingPacket());
		this.incomingPackets.put(1, new UpdateGameOKIncomingPacket());
		this.incomingPackets.put(2, new RequestPlayerSlotsIncomingPacket());
		this.incomingPackets.put(3, new RequestSpectateIncomingPacket());
	}
	
	public void handleIncoming(TcpPacketDataHandler handler, ByteBuf msg)
	{
		int packetId = msg.readShort();
		
		TcpIncomingPacket incoming = this.incomingPackets.get(packetId);
		if (incoming != null)
		{
			incoming.handle(handler, msg);
		}
		else
		{
			System.out.println("Unhandled packet: " + packetId);
		}
	}
}
