package fi.joniaromaa.minigameframework.communication.tcp.outgoing;

import java.util.Collection;
import java.util.UUID;

import fi.joniaromaa.minigameframework.communication.tcp.TcpOutgoingPacket;
import fi.joniaromaa.minigameframework.utils.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class RegisterGameOutgoingPacket implements TcpOutgoingPacket
{
	private static final int PACKET_ID = 0;
	
	private boolean ongoing;
	private int serverId;
	private int gameType;
	private int maxPlayers;
	
	private Collection<UUID> players;
	
	public RegisterGameOutgoingPacket(int serverId, int gameType, int maxPlayers, Collection<UUID> players)
	{
		this.ongoing = false;
		this.serverId = serverId;
		this.gameType = gameType;
		this.maxPlayers = maxPlayers;
		
		this.players = players;
	}
	
	public RegisterGameOutgoingPacket(int serverId, int gameType, Collection<UUID> players)
	{
		this.ongoing = true;
		this.serverId = serverId;
		this.gameType = gameType;
		
		this.players = players;
	}
	
	@Override
	public ByteBuf getBytes()
	{
		ByteBuf buf = Unpooled.buffer();
		buf.writeShort(RegisterGameOutgoingPacket.PACKET_ID);
		buf.writeBoolean(this.ongoing);
		buf.writeInt(this.serverId);
		buf.writeInt(this.gameType);
		
		if (!this.ongoing)
		{
			buf.writeInt(this.maxPlayers);
		}
		
		buf.writeInt(this.players.size());
		for(UUID uuid : this.players)
		{
			ByteBufUtils.writeUUID(buf, uuid);
		}
		return buf;
	}
}
