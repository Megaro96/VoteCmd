package util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import main.VoteCmd;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class VoteMySQLHandler {

	private String host;
	private int port;
	private String user;
	private String pass;
	private String db;
	private Connection conn;
	
	
	public VoteMySQLHandler(VoteCmd p){
		
		File file = new File(p.getDataFolder().getPath(), "mysql.yml");

		FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

		this.host = cfg.getString("MySQL.host");
		this.port = cfg.getInt("MySQL.port");
		this.user = cfg.getString("MySQL.user");
		this.pass = cfg.getString("MySQL.pass");
		this.db = cfg.getString("MySQL.database");
		
	}
	
	
	public void closeConnection(){

		try {if(conn != null){
		
			this.conn.close();
		
		}
		
		}
		catch (SQLException e) {
		
			e.printStackTrace();
		
		}
		finally{
		
			this.conn = null;

		}
		
	}
	
	public void closeRessources(ResultSet rs, PreparedStatement st){
		
		if(rs != null){
			
			try {
			
				rs.close();
			
			} catch (SQLException e) {
		
				e.printStackTrace();
		
			}
		
		}
		
		if(st != null){
		
			try {
	
				st.close();
		
			} catch (SQLException e) {
		
				e.printStackTrace();
		
			}
	
		}
		
	}
	
	public Connection getConnection(){
		
		return this.conn;
		
	}
	
	public boolean hasConnection(){
		
		try {
		
			return this.conn != null || this.conn.isValid(1);
		
		} 
		catch (SQLException e) {
			
			return false;
		
		}
		
	}
	
	public Connection openConnection() throws Exception{
	
			Class.forName("com.mysql.jdbc.Driver");

			Connection conn = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.db, this.user, this.pass);
		
			this.conn = conn;
			
			return conn;
		

		
	
	}
	
	
	public void query(String s){
		
		Connection conn;
		
		try {
			
			conn = this.openConnection();

			PreparedStatement st = null;
			
		try {
		
			st = conn.prepareStatement(s);
			st.executeUpdate();
			
		}
		catch (SQLException e) {
		
			e.printStackTrace();
		
		}
		finally{
		
			this.closeRessources(null, st);
			this.closeConnection();
	
		}
		
		} 
		catch (Exception e1) {
	
			e1.printStackTrace();
	
		}
		
	}
	
	public ResultSet queryResult(String s){
		
		Connection conn;
		
		try {
			
			conn = this.openConnection();

			PreparedStatement st = null;
			
			ResultSet rs = null;
			
		try {
		
			st = conn.prepareStatement(s);
			rs = st.executeQuery();
	
		}
		catch (SQLException e) {
		
			e.printStackTrace();
		
		}
		
		return rs;
		
		} 
		catch (Exception e1) {
	
			e1.printStackTrace();
	
		}
		
		return null;
		
	}
	
}

