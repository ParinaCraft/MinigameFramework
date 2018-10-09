package fi.joniaromaa.minigameframework.utils;

import java.util.UUID;

import io.netty.buffer.ByteBuf;

public class ByteBufUtils
{
	public static void writeUUID(ByteBuf buf, UUID uuid)
	{
		buf.writeLong(uuid.getMostSignificantBits());
		buf.writeLong(uuid.getLeastSignificantBits());
	}
	
	public static UUID readUUID(ByteBuf buf)
	{
		return new UUID(buf.readLong(), buf.readLong());
	}
}
