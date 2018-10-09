package fi.joniaromaa.minigameframework.utils;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.ArmorStand;

public class EntityUtils
{
	public static ArmorStand addHologram(Location location, String hologram)
	{
		ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
		armorStand.setMarker(true);
		armorStand.setVisible(false);
		armorStand.setGravity(false);
		armorStand.setCustomNameVisible(true);
		armorStand.setCustomName(hologram);
		
		((CraftEntity)armorStand).getHandle().k = false; //Block placement collision
		
		return armorStand;
	}
}
