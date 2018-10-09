package fi.joniaromaa.minigameframework.nms;

import java.io.File;

import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.ServerNBTManager;

public class NoDataServerNBTManager extends ServerNBTManager
{
	private static final String[] EMPTY_STRING = new String[0];
	
	public NoDataServerNBTManager(File file, String s, boolean flag)
	{
		super(file, s, flag);
	}
	
	@Override
    public void save(EntityHuman entityhuman)
    {
    	//We save... nothing
    }
	
	@Override
    public NBTTagCompound load(EntityHuman entityhuman)
    {
    	return null; //We load... nothing
    }
	
	@Override
	public NBTTagCompound getPlayerData(String s)
	{
    	return null; //We load... nothing
	}
	
	@Override
    public String[] getSeenPlayers()
    {
    	return NoDataServerNBTManager.EMPTY_STRING; //We don't have any player data...
    }
}
