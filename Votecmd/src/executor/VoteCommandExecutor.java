package executor;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import main.VoteCmd;

public class VoteCommandExecutor implements CommandExecutor {
	
	private VoteCmd plugin;

	public VoteCommandExecutor(VoteCmd instance){
		this.plugin = instance;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
			if(sender instanceof Player){
				Player p = (Player) sender;
				if (args.length == 0){
					p.spigot().sendMessage(new ComponentBuilder("Klick mich!").color(ChatColor.DARK_PURPLE).bold(true)
							.event(new ClickEvent(ClickEvent.Action.OPEN_URL, plugin.link))
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Jetzt voten!").bold(true).create()))
							.create());
					return true;
				}
				else if (args.length == 1) {
					if (args[0].equalsIgnoreCase("winner")){
						if (this.plugin.lastwinner.equals("maxmustermann") || this.plugin.lastwinner.equals("")){
							p.sendMessage(ChatColor.RED + "Es gab noch keinen Gewinner!");
							return true;
						}
						else{
							p.sendMessage(ChatColor.GOLD + "Gewinner der letzten wöchtentlichen VoteLotterie: " + ChatColor.WHITE + this.plugin.lastwinner);
							return true;
						}
					}
					if (args[0].equalsIgnoreCase("get")){
						this.plugin.VoteGet(p);
						return true;
					}
					if (args[0].equalsIgnoreCase("all")){
						Integer i = this.plugin.allVotes(p.getName());
						p.sendMessage(ChatColor.GOLD + "Votes: " + ChatColor.WHITE + i);
						return true;
					}
					if (args[0].equalsIgnoreCase("week")){
						Integer i = this.plugin.weekVotes(p.getName());
						p.sendMessage(ChatColor.GOLD + "Votes dieser Woche: " + ChatColor.WHITE + i);
						return true;
					}
					if (args[0].equalsIgnoreCase("points")){
						Integer i = this.plugin.VotePoints(p.getName());
						p.sendMessage(ChatColor.GREEN + "VotePoints: " + ChatColor.WHITE + i);
						return true;
					}
					if (p.hasPermission("votecmd.other")){
						if (args[0].equalsIgnoreCase("top")){
							this.plugin.getTop(p);
							return true;
						}
						if (args[0].equalsIgnoreCase("alltop")){
							this.plugin.getTopAll(p);
							return true;
						}
					}
					else{
						p.sendMessage(ChatColor.RED + "Du hast keine Berechtigung dies zu machen!");
						return false;
					}
					if (p.hasPermission("votecmd.admin")){
						if (args[0].equalsIgnoreCase("reload")){
							this.plugin.reloadPlugin();
							p.sendMessage(ChatColor.GREEN + "Plugin wurde erneut geladen!");
							return true;
						}
					}
					else{
						p.sendMessage(ChatColor.RED + "Du hast keine Berechtigung dies zu machen!");
						return false;
					}
					if (p.hasPermission("votecmd.staff")){
						if (Bukkit.getOfflinePlayer(args[0]) != null && Bukkit.getOfflinePlayer(args[0]).isOnline()){
							Player target = Bukkit.getPlayer(args[0]);
							target.sendMessage(ChatColor.GOLD + p.getName() + " hat dir den Votelink geschickt: " + ChatColor.RED + ChatColor.UNDERLINE + plugin.link);
							p.sendMessage(ChatColor.GREEN + "Du hast den Votelink an " + target.getName() + " verschickt!");
							return true;
						}
						else{
							p.sendMessage(ChatColor.RED + "Der Spieler " + args[0] + " ist nicht online!");
							return true;
						}
					}
					else{
						p.sendMessage(ChatColor.RED + "Du hast keine Berechtigung dies zu machen!");
					}
					return false;
					
				}
				else if(args.length == 2){
					if (p.hasPermission("votecmd.other")){
						if (args[0].equalsIgnoreCase("all")){
							Integer i = this.plugin.allVotes(args[1]);
							p.sendMessage(ChatColor.GOLD + "Votes von "+ args[1] + ": " + ChatColor.WHITE + i);
							return true;
						}
						if (args[0].equalsIgnoreCase("week")){
							Integer i = this.plugin.weekVotes(args[1]);
							p.sendMessage(ChatColor.GOLD + "Votes dieser Woche von "+ args[1] + ": " + ChatColor.WHITE + i);
							return true;
						}
						if (args[0].equalsIgnoreCase("points")){
							Integer i = this.plugin.VotePoints(args[1]);
							p.sendMessage(ChatColor.GREEN + "VotePoints von "+ args[1] + ": " + ChatColor.WHITE + i);
							return true;
						}
					}
					else{
						p.sendMessage(ChatColor.RED + "Du hast keine Berechtigung dies zu machen!");
						return false;
					}
					return false;
				}
				p.sendMessage(ChatColor.RED + "Deine Eingabe ist zu lang!");
				return false;
			}
			else {
				if (args.length == 0){
					System.out.println("Votelink: " + plugin.link);
					return true;
				}
				else if (args.length == 4) {
					if (args[0].equalsIgnoreCase("lot")){
						int i, v1, v2;
						try{
							i = new Integer(args[1]);
							}
							catch(NumberFormatException e){
								System.out.println("Der Wert muss eine Zahl sein!");
								return true;
							}
						try{
							v1 = new Integer(args[2]);
							}
							catch(NumberFormatException e){
								System.out.println("Der Wert muss eine Zahl sein!");
								return true;
							}
						try{
							v2 = new Integer(args[3]);
							}
							catch(NumberFormatException e){
								System.out.println("Der Wert muss eine Zahl sein!");
								return true;
							}
						this.plugin.giveRewardBonus(i, v1, v2);
						return true;
					}
				}else if (args.length == 1){
					if (args[0].equalsIgnoreCase("resetweek")){
						this.plugin.resetWeekvotes();
						return true;
					}
					if (args[0].equalsIgnoreCase("resetbonus")){
						this.plugin.resetBonus();
						return true;
					}
					return false;
				}
				return false;
			}
	}
}