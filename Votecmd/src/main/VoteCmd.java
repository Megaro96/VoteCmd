package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

import listeners.PlayerJoinListener;
import listeners.VoteEventListener;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import util.VoteMySQLHandler;
import executor.VoteCommandExecutor;

public class VoteCmd extends JavaPlugin implements Listener {
	
	public static Economy economy = null;
	public Integer reward_vp;
	public Integer reward_bonus;
	public Integer reward_money;
	public Integer posx;
	public Integer posy;
	public Integer posz;
	public String world;
	public String link;
	public String lastwinner;
	private int pid;
	public int counter = 0;
	public me.mrlennartxd.dailyreward.DailyReward dailyReward = null;
	public me.lucko.luckperms.api.LuckPermsApi api = null;
	VoteMySQLHandler sql;
	
	@Override
	public void onEnable(){
		
		dailyReward = getDailyReward();
		if(dailyReward == null){
			Bukkit.getConsoleSender().sendMessage("[VoteCmd] §cKeine Verbindung zum DailyReward!");
		}
		
		api = LuckPerms.getApi();
		if(api == null){
			Bukkit.getConsoleSender().sendMessage("[VoteCmd] §cKeine Verbindung zum LuckPerms!");
		}
		
		new VoteEventListener(this);
		new PlayerJoinListener(this);
		this.getCommands();
		this.loadConfig();
		this.setupEconomy();
		this.loadVote();
		createMySQLConfig();
		reward_vp = this.getConfig().getInt("reward_vp");
		reward_bonus = this.getConfig().getInt("reward_bonus");
		reward_money = this.getConfig().getInt("reward_money");
		posx = this.getConfig().getInt("posx");
		posy = this.getConfig().getInt("posy");
		posz = this.getConfig().getInt("posz");
		world = this.getConfig().getString("world");
		link = this.getConfig().getString("link");
		lastwinner = this.getConfig().getString("lastwinner");
		sql = new VoteMySQLHandler(this);
		sql.query("CREATE TABLE IF NOT EXISTS votecmd(uuid VARCHAR(32) primary key, votes INT, weekvotes INT)");
		sql.query("CREATE TABLE IF NOT EXISTS votepoints(uuid VARCHAR(32) primary key, votepoints INT, bonus INT, last DATE)");
		Runnable taskBC = new Runnable(){
			@Override
			public void run() {
				if (counter > 0){
					String s = " VotePoints";
					if (reward_vp == 1){
						s = " VotePoint";
					}
					Bukkit.broadcastMessage(ChatColor.GOLD + "Es wurde " + counter + "x für AyoCraft gevotet! Ein Spieler erhält pro Vote " + ChatColor.GREEN + reward_vp +s+ ChatColor.GOLD + " sowie " + ChatColor.GREEN + reward_money + " Pixel" + ChatColor.GOLD + "!");
					counter = 0;
				}
			}
		};
		pid = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, taskBC, 1200L, 2400L);
		System.out.println("VoteCmd version " + this.getDescription().getVersion() + " enabled!");
		System.out.println("Plugin by Megaro & CoonFight");
	}
	
	private me.mrlennartxd.dailyreward.DailyReward getDailyReward() {
	    Plugin plugin = getServer().getPluginManager().getPlugin("DailyReward");
	 
	    if (plugin == null || !(plugin instanceof me.mrlennartxd.dailyreward.DailyReward)) {
	        return null;
	    }
	 
	    return (me.mrlennartxd.dailyreward.DailyReward) plugin;
	}
	
	public void asynctaskReminder(Player p){
		Runnable ataskReminder = new Runnable(){
			@Override
			public void run(){
				ResultSet rs = sql.queryResult("SELECT * FROM votepoints WHERE last = CURDATE() AND uuid = '" + p.getUniqueId().toString().replace("-", "") + "'");
				try {
					if (!rs.next()){
						p.spigot().sendMessage(new ComponentBuilder("Du hast heute noch nicht gevotet! ").color(ChatColor.GOLD).append("Klick mich!").color(ChatColor.DARK_PURPLE).bold(true)
								.event(new ClickEvent(ClickEvent.Action.OPEN_URL, link))
								.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Jetzt voten!").bold(true).create()))
								.create());
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		};
		Bukkit.getScheduler().runTaskLaterAsynchronously(this, ataskReminder, 60L);
	}
	
	private void getCommands() {
		VoteCommandExecutor exe = new VoteCommandExecutor(this);
		getCommand("vote").setExecutor(exe);
	}
	
	private void createMySQLConfig() {
		File f = new File(getDataFolder().getPath() + "/mysql.yml");
		if(!f.exists()){
			try {
				new File(getDataFolder().getPath() + "/").mkdir();
				f.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			Scanner s;
			try {
				InputStream is = getResource("mysql.yml");
				s = new Scanner(is);
				while(s.hasNextLine()){
					String n = s.nextLine();
					PrintWriter fw = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
					fw.println(n);
					fw.close();
				}	
				is.close();
				s.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void onDisbale(){
		Bukkit.getScheduler().cancelTask(pid);
		System.out.println("VoteCmd version " + this.getDescription().getVersion() + " disabled!");
	}
	
	private void loadConfig(){
		FileConfiguration cfg = this.getConfig();
		cfg.options().copyDefaults(true);
		this.saveConfig();
	}
	
	public void reloadPlugin(){
		this.reloadConfig();
		reward_vp = this.getConfig().getInt("reward_vp");
		reward_bonus = this.getConfig().getInt("reward_bonus");
		reward_money = this.getConfig().getInt("reward_money");
		posx = this.getConfig().getInt("posx");
		posy = this.getConfig().getInt("posy");
		posz = this.getConfig().getInt("posz");
		world = this.getConfig().getString("world");
		link = this.getConfig().getString("link");
		lastwinner = this.getConfig().getString("lastwinner");
		System.out.println("VoteCmd version " + this.getDescription().getVersion() + " reloaded!");
	}
	
	public void loadVote(){
		File votes = new File("plugins/VoteCmd", "votes.yml");
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(votes);
		try {
			cfg.save(votes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void giveSpecialItem(Player p, Integer i, Integer slot){
		World w = getServer().getWorld(world);
		Location l = new Location(w, posx, posy, posz);
		BlockState state = l.getBlock().getState();
		if (state instanceof Chest){
			Inventory chest=((Chest)state).getInventory();
			ItemStack stack = chest.getItem(slot).clone();
			stack.setAmount(i);
			p.getInventory().addItem(stack);
		}	
	}
	
	private boolean addPermission(User user, String permission) {

	    // build the permission node
	    Node node = api.getNodeFactory().newBuilder(permission).build();

	    // set the permission
	    DataMutateResult result = user.setPermission(node);

	    // wasn't successful.
	    // they most likely already have the permission
	    if (result != DataMutateResult.SUCCESS) {
	        return false;
	    }

	    // now, before we return, we need to have the user to storage.
	    // this method will save the user, then run the callback once complete.
	    api.getStorage().saveUser(user)
	            .thenAcceptAsync(wasSuccessful -> {
	                if (!wasSuccessful) {
	                    return;
	                }

	                System.out.println("Successfully set permission! Permi: " +permission+" Player: "+user.getName());

	                // refresh the user's permissions, so the change is "live"
	                // this method is blocking, but it's fine, because this callback is
	                // ran async.
	                user.refreshCachedData();

	            }, api.getStorage().getAsyncExecutor());

	    return true;
	}
	
	private boolean givePet(Player p, String s){
		//PermissionUser user = PermissionsEx.getUser(p);
		//user.addPermission("MinaturePets." + s);
		if (addPermission (api.getUser(p.getUniqueId()),"MinaturePets." +s)) {
			return true;
		}
		return false;
	}
	
	public void addVoteOffline(String s){
		File votes = new File("plugins/VoteCmd", "votes.yml");
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(votes);
		
		Integer i = cfg.getInt(s.toLowerCase());
		i++;
		cfg.set(s.toLowerCase(), i);
		
		try {
			cfg.save(votes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isBonus(UUID uuid){
		boolean set = false;
		ResultSet rs = sql.queryResult("SELECT * FROM votepoints WHERE uuid = '"+uuid.toString().replace("-", "")+"' AND bonus = 1");
		try {
			if (rs.next()){
				set = true;
				return true;
			}else{
				ResultSet rs2 = sql.queryResult("SELECT * FROM votepoints WHERE uuid = '"+uuid.toString().replace("-", "")+"' AND last = DATE_SUB(CURDATE(), INTERVAL 1 DAY)");
				if (rs2.next()){
					sql.query("UPDATE votepoints SET bonus = 1 WHERE uuid = '"+uuid.toString().replace("-", "")+"'");
					set = true;
					return true;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return set;
	}
	
	public void addVote(UUID uuid, Integer i){
		sql.query("INSERT INTO votecmd (uuid, votes, weekvotes) VALUES ('"+uuid.toString().replace("-", "")+"', "+i+", "+i+") ON DUPLICATE KEY UPDATE votes = votes + "+i+", weekvotes = weekvotes + "+i);
		dailyReward.addNextVote(uuid, i);
	}
	
	public void addVotePoints(UUID uuid, Integer i){
		Integer a = i*reward_vp;
		sql.query("INSERT INTO votepoints (uuid, votepoints, bonus, last) VALUES ('"+uuid.toString().replace("-", "")+"', "+a+", 0, CURDATE()) ON DUPLICATE KEY UPDATE votepoints = votepoints + "+a+", last = CURDATE()");
	}
	
	public void setVotePoints(UUID uuid, Integer i){
		sql.query("INSERT INTO votepoints (uuid, votepoints, bonus) VALUES ('"+uuid.toString().replace("-", "")+"', "+i+", 0) ON DUPLICATE KEY UPDATE votepoints = "+i);
	}
	
	public void resetWeekvotes() {
		sql.query("UPDATE votecmd SET weekvotes = 0");
	}
	
	public void resetBonus(){
		sql.query("UPDATE votepoints SET bonus = 0");
	}
	
	@SuppressWarnings("deprecation")
	public void giveItem(Player p, String id, String amounts, Integer i){
		if (hasPoints(p.getUniqueId(), i)){
			if (p.getInventory().firstEmpty() != -1){
				if (id.toLowerCase().contains("slot:")){
					Integer slot = Integer.parseInt(id.replaceAll("[^0-9]",""));
					Integer amount = Integer.parseInt(amounts);
					sql.query("UPDATE votepoints SET votepoints = votepoints - "+i+" WHERE uuid = '"+p.getUniqueId().toString().replace("-", "") +"'");
					giveSpecialItem(p, amount, slot);
					String s = " VotePoints";
					if (i == 1){
						s = " VotePoint";
					}
					p.sendMessage(ChatColor.GREEN + "Kauf erfolgreich! Kosten: " + i + s);
				}
				else if (id.equalsIgnoreCase("pet")){
					if (!p.hasPermission("MinaturePets."+amounts)){
						if (!givePet(p, amounts)) {
							p.sendMessage(ChatColor.RED + "Kauf abgebrochen! Es ist ein Fehler aufgetreten! Bitte teile dies dem Staff mit! Fehlercode: VCMDgiveItem");
							return;
						}
						sql.query("UPDATE votepoints SET votepoints = votepoints - "+i+" WHERE uuid = '"+p.getUniqueId().toString().replace("-", "") +"'");
						String s = " VotePoints";
						if (i == 1){
							s = " VotePoint";
						}
						p.sendMessage(ChatColor.GREEN + "Kauf erfolgreich! Kosten: " + i + s);
					}else{
						p.sendMessage(ChatColor.RED + "Kauf abgebrochen! Du besitzt dieses Pet bereits!");
					}
					
				}
				else{
					Integer amount = Integer.parseInt(amounts);
					sql.query("UPDATE votepoints SET votepoints = votepoints - "+i+" WHERE uuid = '"+p.getUniqueId().toString().replace("-", "") +"'");
					String str1 = id;
					int dotIndex = str1.indexOf(":");
					String str2 = str1.substring(dotIndex+1);
					str1 = str1.substring(0, dotIndex);
					Integer itemID = Integer.parseInt(str1);
					short typeID = (short) Integer.parseInt(str2);
					ItemStack stack = new ItemStack(itemID, amount, typeID);
					p.getInventory().addItem(stack);
					String s = " VotePoints";
					if (i == 1){
						s = " VotePoint";
					}
					p.sendMessage(ChatColor.GREEN + "Kauf erfolgreich! Kosten: " + i + s);
				}
				
			}else{
				p.sendMessage(ChatColor.RED + "Kauf abgebrochen! Leider ist dein Inventar voll. Bitte räume entsprechend Platz frei!");
			}
		}else{
			p.sendMessage(ChatColor.RED + "Kauf abgebrochen! Du hast nicht genügend VotePoints! Diese erhälst du durch Voten. " + ChatColor.WHITE + "/vote");
		}
	}
	
	public boolean hasPoints(UUID uuid, Integer i){
		ResultSet rs = sql.queryResult("SELECT votepoints FROM votepoints WHERE uuid = '" + uuid.toString().replace("-", "") + "'");
		int points = 0;
		try {
			if (rs.next()){
				points = rs.getInt("votepoints");
			}
			rs.getStatement().getConnection().close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (points < i){
			return false;
		}else{
			return true;
		}
	}
	
	@SuppressWarnings("deprecation")
	public Integer allVotes (String s){
		ResultSet rs = sql.queryResult("SELECT votes FROM votecmd WHERE uuid = '" + Bukkit.getOfflinePlayer(s).getUniqueId().toString().replace("-", "") + "'");
		int votes = 0;
		try {
			if (rs.next()){
				votes = rs.getInt("votes");
			}
			rs.getStatement().getConnection().close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return votes;
	}
	
	@SuppressWarnings("deprecation")
	public Integer weekVotes (String s){
		int weekvotes = 0;
		ResultSet rs = sql.queryResult("SELECT weekvotes FROM votecmd WHERE uuid = '" + Bukkit.getOfflinePlayer(s).getUniqueId().toString().replace("-", "") + "'");
		try {
			if (rs.next()){
				weekvotes = rs.getInt("weekvotes");
			}
			rs.getStatement().getConnection().close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return weekvotes;
	}
	
	@SuppressWarnings("deprecation")
	public Integer VotePoints (String s){
		int votepoints = 0;
		ResultSet rs = sql.queryResult("SELECT votepoints FROM votepoints WHERE uuid = '" + Bukkit.getOfflinePlayer(s).getUniqueId().toString().replace("-", "") + "'");
		try {
			if (rs.next()){
				votepoints = rs.getInt("votepoints");
			}
			rs.getStatement().getConnection().close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return votepoints;
	}
	
	public void VoteGet (Player p){
		File votes = new File("plugins/VoteCmd", "votes.yml");
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(votes);
		Integer i = cfg.getInt(p.getName().toLowerCase());
		if (i != 0){
			Integer b = 0;
			Integer vp = reward_vp*i;
			Integer money = reward_money*i;
			if (isBonus(p.getUniqueId())){
				if (i > 3){
					b = reward_bonus*3;
				}else{
					b = reward_bonus*i;
				}
			}
			cfg.set(p.getName().toLowerCase(), 0);
			addVote(p.getUniqueId(), i);
			giveReward(p, vp+b, money);
			String s = " VotePoints";
			if (reward_vp*i == 1){
				s = " VotePoint";
			}
			String s2 = " VotePoints";
			if (reward_bonus == 1){
				s2 = " VotePoint";
			}
			p.sendMessage(ChatColor.GREEN + "Du hast " + i + "x in Abwesenheit gevotet und erhälst " + (reward_vp*i) + s +" sowie "+ (reward_money*i) +" Pixel!");
			if (b > 0){
				p.sendMessage(ChatColor.GREEN + "Da du 2 Tage in Folge votest erhälst du jeweils " + reward_bonus + s2 + " extra! (Für maximal 3 deiner Votes)");
			}
			try {
				cfg.save(votes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{
			p.sendMessage(ChatColor.RED + "Du hast keine Votes in Abwesenheit!");
		}
	}
	
	public void giveReward(Player p, Integer i, Integer i2){
		VoteCmd.economy.depositPlayer(p, i2);
		addVotePoints(p.getUniqueId(), i);
	}
	
	@SuppressWarnings("deprecation")
	public void giveRewardBonus(int i, int v1, int v2){
		String p = getRandomPlayer(i);
		Random r = new Random();
		UUID uuid = UUID.fromString(p.substring(0, 8) + "-" + p.substring(8, 12) + "-" + p.substring(12, 16) + "-" + p.substring(16, 20) + "-" +p.substring(20, 32));
		Integer rnd = r.nextInt(v2-v1)+v1;
		VoteCmd.economy.depositPlayer(Bukkit.getOfflinePlayer(uuid).getName(), rnd);
		setLastWinner(Bukkit.getOfflinePlayer(uuid).getName());
		Bukkit.broadcastMessage(ChatColor.RED + "[VoteLotterie] " + ChatColor.AQUA + Bukkit.getOfflinePlayer(uuid).getName() + ChatColor.GOLD + " hatte " + i + " oder mehr Votes und hat die wöchtentliche VoteLotterie gewonnen! Der zufällige Preis ist: " + ChatColor.GREEN + rnd + " Pixel" + ChatColor.GOLD + "!");
	}
	
	public void setLastWinner(String s){
		FileConfiguration cfg = this.getConfig();
		cfg.set("lastwinner", s);
		this.saveConfig();
		lastwinner = s;
	}
	
	public String getRandomPlayer(Integer i){
		ResultSet rs = sql.queryResult("SELECT uuid FROM votecmd WHERE weekvotes >=" + i);
		final ArrayList<String> l = new ArrayList<>();
		Random r = new Random();
		try {
			while (rs.next()){
				l.add(rs.getString("uuid"));
			}
			rs.getStatement().getConnection().close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		Integer rnd = r.nextInt(l.size()-1);
		return l.get(rnd) == null ? "": l.get(rnd);
	}
	
	public void getTop(Player p){
		ResultSet rs = sql.queryResult("SELECT * FROM votecmd ORDER BY weekvotes DESC LIMIT 0,5");
		p.sendMessage(ChatColor.GOLD + "Top 5 Voter dieser Woche:");
		int i = 1;
		try {
			while (rs.next()){
				String pl = rs.getString("uuid");
				UUID uuid = UUID.fromString(pl.substring(0, 8) + "-" + pl.substring(8, 12) + "-" + pl.substring(12, 16) + "-" + pl.substring(16, 20) + "-" +pl.substring(20, 32));
				p.sendMessage(ChatColor.GOLD + "Platz " + i + ": " + ChatColor.GREEN + ChatColor.ITALIC + Bukkit.getOfflinePlayer(uuid).getName() + ChatColor.RESET + ChatColor.DARK_GREEN + " - " + rs.getInt("weekvotes") + " Votes");
				i++;
			}
			rs.getStatement().getConnection().close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void getTopAll(Player p){
		ResultSet rs = sql.queryResult("SELECT * FROM votecmd ORDER BY votes DESC LIMIT 0,5");
		p.sendMessage(ChatColor.GOLD + "Top 5 Voter aller Zeiten:");
		int i = 1;
		try {
			while (rs.next()){
				String pl = rs.getString("uuid");
				UUID uuid = UUID.fromString(pl.substring(0, 8) + "-" + pl.substring(8, 12) + "-" + pl.substring(12, 16) + "-" + pl.substring(16, 20) + "-" +pl.substring(20, 32));
				p.sendMessage(ChatColor.GOLD + "Platz " + i + ": " + ChatColor.GREEN + ChatColor.ITALIC + Bukkit.getOfflinePlayer(uuid).getName() + ChatColor.RESET + ChatColor.DARK_GREEN + " - " + rs.getInt("votes") + " Votes");
				i++;
			}
			rs.getStatement().getConnection().close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("deprecation")
	public void onVote(String user, String service) {
		if (Bukkit.getOfflinePlayer(user).isOnline()){
			UUID uuid = Bukkit.getOfflinePlayer(user).getUniqueId();
			Integer b = 0;
			if (isBonus(uuid)){
				b = reward_bonus;
			}
			giveReward (Bukkit.getPlayer(uuid), reward_vp+b, reward_money);
			String s = " VotePoints";
			if (reward_vp == 1){
				s = " VotePoint";
			}
			String s2 = " VotePoints";
			if (reward_bonus == 1){
				s2 = " VotePoint";
			}
			Bukkit.getPlayer(uuid).sendMessage(ChatColor.GREEN + "Vielen Dank für deinen Vote auf " + service + "! Du erhälst " + reward_vp + s + " sowie " + reward_money + " Pixel!");
			if (b > 0){
				Bukkit.getPlayer(uuid).sendMessage(ChatColor.GREEN + "Da du 2 Tage in Folge votest erhälst du jeweils " + reward_bonus + s2 + " extra!");
			}
			counter++;
			this.addVote(uuid, 1);
		}
		else{
			counter++;
			this.addVoteOffline(user);
		}
	}
	
	private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
	
	@SuppressWarnings("deprecation")
	@EventHandler (priority = EventPriority.NORMAL)
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if(cmd.getName().equalsIgnoreCase("votepoints")){
			if (sender instanceof Player) {
				
				Player p = (Player) sender;
				
				if (args.length == 0){
					Integer i = VotePoints(p.getName());
					p.sendMessage(ChatColor.GREEN + "VotePoints: " + ChatColor.WHITE + i);
					return true;
				}
				if (args.length == 1){
					if (p.hasPermission("votecmd.other")){
						Integer i = VotePoints(args[0]);
						p.sendMessage(ChatColor.GREEN + "VotePoints von "+ args[0] + ": " + ChatColor.WHITE + i);
						return true;
					}else{
						p.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, dies zu machen!");
						return false;
					}
				}
				if (args.length == 3){
					if (args[1].contains("'") == false && args[1].contains("\\") == false && args[2].contains("'") == false && args[2].contains("\\") == false){
						if (p.hasPermission("votecmd.admin")){
							if (args[0].equalsIgnoreCase("set")){
								if (args[2].matches("[0-9]+")){
									OfflinePlayer b = Bukkit.getOfflinePlayer(args[1]);
									if (b.hasPlayedBefore()){
										Integer i = Integer.parseInt(args[2]);
										setVotePoints(b.getUniqueId(), i);
										p.sendMessage(ChatColor.GREEN + "Du hast die VotePoints von " + b.getName() + " auf " + i + " gesetzt!");
										return true;
									}else{
										p.sendMessage(ChatColor.RED + "Spieler nicht gefunden!");
										return false;
									}
								}else{
									p.sendMessage(ChatColor.RED + "Fehlerhafte Zeichen vorhanden!");
									return false;
								}
							}
						}else{
							p.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, dies zu machen!");
							return false;
						}
					}else{
						p.sendMessage(ChatColor.RED + "Fehlerhafte Zeichen vorhanden!");
						return false;
					}
				}
				return false;
			}else{
				if (args.length == 4){
					if (args[1].contains("'") == false && args[1].contains("\\") == false && args[2].contains("'") == false && args[2].contains("\\") == false && args[3].contains("'") == false && args[3].contains("\\") == false){
						Integer i = Integer.parseInt(args[3]);
						giveItem(Bukkit.getPlayer(args[0]), args[1], args[2], i);
						return true;
					}
				}
			}
			return false;
		}
		return false;
	}
	
}