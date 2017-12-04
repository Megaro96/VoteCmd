package listeners;

import main.VoteCmd;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteEventListener implements Listener {
	
	private VoteCmd plugin;
	
	public VoteEventListener(VoteCmd instance) {
		this.plugin = instance;
		Bukkit.getPluginManager().registerEvents(this, this.plugin);
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
    public void onVotifierEvent(VotifierEvent event) {
		Vote vote = event.getVote();
		if (vote.getUsername() != null && vote.getServiceName() != null && !vote.getUsername().equals("") && !vote.getServiceName().equals("")){
			this.plugin.onVote(vote.getUsername(), vote.getServiceName());
		}
		else{
			System.out.println("Fehlerhafter Vote wurde erkannt! Spieler- oder Servicename fehlt.");
		}
    }
}
