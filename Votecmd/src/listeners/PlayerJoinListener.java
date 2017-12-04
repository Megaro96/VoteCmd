package listeners;

import main.VoteCmd;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
	
	private VoteCmd plugin;
	
	public PlayerJoinListener(VoteCmd instance) {
		this.plugin = instance;
		Bukkit.getPluginManager().registerEvents(this, this.plugin);
	}
	
	@EventHandler (priority = EventPriority.NORMAL)
	public void PlayerReminder(PlayerJoinEvent event){
		Player p = event.getPlayer();
		this.plugin.asynctaskReminder(p);
	}

}
