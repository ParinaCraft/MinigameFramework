package fi.joniaromaa.minigameframework.communication.tcp.incoming;

import java.util.UUID;

import fi.joniaromaa.minigameframework.MinigamePlugin;
import fi.joniaromaa.minigameframework.api.game.Minigame;
import fi.joniaromaa.minigameframework.communication.tcp.TcpIncomingPacket;
import fi.joniaromaa.minigameframework.game.AbstractMinigame;
import fi.joniaromaa.minigameframework.net.TcpPacketDataHandler;
import fi.joniaromaa.minigameframework.utils.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public class RequestSpectateIncomingPacket implements TcpIncomingPacket
{
	@Override
	public void handle(TcpPacketDataHandler handler, ByteBuf buf)
	{
		UUID uniqueId = ByteBufUtils.readUUID(buf);
		
		Minigame m = handler.getNetworkManager().getMinigame();
		if (m instanceof AbstractMinigame)
		{
			AbstractMinigame<?, ?> minigame = (AbstractMinigame<?, ?>)m;
			
			MinigamePlugin.getPlugin().getGameManager().requestSpectator(minigame.getGameId(), uniqueId);
		}
	}
}
