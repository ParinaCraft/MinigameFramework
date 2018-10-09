package fi.joniaromaa.minigameframework.communication.tcp.incoming;

import java.util.UUID;

import fi.joniaromaa.minigameframework.MinigamePlugin;
import fi.joniaromaa.minigameframework.api.game.Minigame;
import fi.joniaromaa.minigameframework.communication.tcp.TcpIncomingPacket;
import fi.joniaromaa.minigameframework.game.AbstractPreMinigame;
import fi.joniaromaa.minigameframework.net.TcpPacketDataHandler;
import fi.joniaromaa.minigameframework.utils.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public class RequestPlayerSlotsIncomingPacket implements TcpIncomingPacket
{
	@Override
	public void handle(TcpPacketDataHandler handler, ByteBuf buf)
	{
		int uuidsCount = buf.readInt();
		
		UUID[] uuids = new UUID[uuidsCount];
		for(int i = 0; i < uuidsCount; i++)
		{
			uuids[i] = ByteBufUtils.readUUID(buf);
		}
		
		Minigame minigame = handler.getNetworkManager().getMinigame();
		if (minigame instanceof AbstractPreMinigame)
		{
			AbstractPreMinigame preMingiame = (AbstractPreMinigame)minigame;
			
			preMingiame.tryIncludePlayersInSameTeam(uuids);
		
			MinigamePlugin.getPlugin().getGameManager().addJoiningPlayers(preMingiame.getGameId(), uuids);
		}
	}
}
