/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 * 
 */

package helpers;

import java.io.IOException;

import com.google.common.base.Strings;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import util.EEExtras;

public class PermissionsFixer {
	
	private ConfigReader cr;
	private EEconfig config;
	
	// Constructor
	public PermissionsFixer(ConfigReader cr, EEconfig config) {
		this.cr = cr;
		this.config = config;
		try {
			this.fixPermissions();
		} catch (IOException e) {
			System.out.println("Error setting permissions: " + e.getMessage()); 
		}
	}
	
	/*
	 * If uploading files to server, ensure proper permissions set
	 * re: https://docs.expressionengine.com/latest/installation/installation.html#file-permissions
	 */
	public void fixPermissions() throws IOException {
		// Create a connection
		Connection connection = this.connectTo();
		// Notify user of our intent
		String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + "Updating permissions " + EEExtras.ANSI_RESET, 80, '▬');
		System.out.println(consolMsg);
		// Grab the system and app directories
		String sysDest = "";
		String appDest = config.getDirectory();
		String envDirParts[] = config.getDirectory().split("/");
		// Determine if system is above root or not
		if (cr.aboveRoot == true) {
			for (int i = 0; i < envDirParts.length - 1; i++) {
				if (envDirParts[i].equals("")) {
					sysDest += "/";
				} else {
					sysDest += envDirParts[i] + "/";
				}
			}
		} else {
			sysDest = config.getDirectory() + "/";
		}
		// Determine if we have custom upload directory
		String uploadDirPerms = "";
		if(!cr.upDir.equals("")) 
			uploadDirPerms = " && chmod -R 777 " + appDest + "/" + cr.upDir;
		// Build the command
		String command = "chmod -R 777 " + sysDest + cr.getSysDir() + "/user/cache/"
				+ " && chmod -R 777 " + sysDest + cr.getSysDir() + "/user/templates/"
				+ " && chmod 666 " + sysDest + cr.getSysDir() + "/user/config/config.php"
				+ " && chmod -R 755 " + appDest + "/themes/"
				+ " && chmod -R 777 " + appDest + "/cache/"
				+ uploadDirPerms
				+ " && chmod -R 777 " + appDest + "/images/avatars/"
				+ " && chmod -R 777 " + appDest + "/images/captchas/"
				+ " && chmod -R 777 " + appDest + "/images/member_photos/"
				+ " && chmod -R 777 " + appDest + "/images/pm_attachments/"
				+ " && chmod -R 777 " + appDest + "/images/signature_attachments/"
				+ " && chmod -R 777 " + appDest + "/images/uploads/";
		// Show user the command we're sending
		System.out.println(EEExtras.ANSI_PURPLE + "\tremote | " + EEExtras.ANSI_RESET + command);
		// Create the session and execute command on desired environment
		Session session = null;
		try {
			session = connection.openSession();
			session.execCommand(command);			
		} finally {
			if (session != null) {
				session.close();
			}
			if (connection != null) {
				connection.close();
			}
		}
		consolMsg = Strings.padEnd(
				"▬▬ ✓ " + EEExtras.ANSI_CYAN + "Complete! " + EEExtras.ANSI_RESET, 80, '▬');
		System.out.println(consolMsg);
	}

	/*
	 * Creates a connection to host and returns it
	 */
	public Connection connectTo() throws IOException {
		Connection connection = new Connection(config.getHost());
		connection.connect();
		if (cr.isUseKeyAuth()) {
			connection.authenticateWithPublicKey(config.getSshUser(), cr.getKeyfile(), cr.getKeyPass());
		} else {
			connection.authenticateWithPassword(config.getSshUser(), config.getSshPass());
		}
		return connection;
	}

}
