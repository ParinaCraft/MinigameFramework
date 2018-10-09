package fi.joniaromaa.minigameframework.user.dataset;

import fi.joniaromaa.parinacorelibrary.api.user.dataset.UserDataStorage;
import lombok.Getter;

public class UserPreferedMinigameTeamDataStorage implements UserDataStorage
{
	@Getter String team;
	@Getter private long time;
	
	public UserPreferedMinigameTeamDataStorage(String team)
	{
		this.setTeam(team);
	}
	
	public void setTeam(String team)
	{
		this.team = team;
		this.time = System.nanoTime();
	}
}
