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

import util.EEExtras;

public class PermissionsFixer {

	private ConfigReader cr;
	private EEconfig config;
	private CommandExecuter ce;

	// Constructor
	public PermissionsFixer(ConfigReader cr, EEconfig config) {
		this.cr = cr;
		this.config = config;
		this.ce = new CommandExecuter(config);
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
		String command = "find " + appDest + " -type f -exec chmod 644 {} \\;"
				+ " && find " + appDest + " -type d -exec chmod 755 {} \\;";
		
		// If system in separate directory from main app, fix it permissions as well
		if(!sysDest.equals(appDest)) {
			command += " && find " + sysDest + " -type f -exec chmod 644 {} \\;"
					+ " && find " + sysDest + " -type d -exec chmod 755 {} \\;";
		}
		
		// Set version specific permissions
		if(cr.eeVer == 3) {
			command += " && chmod -R 777 " + sysDest + cr.getSysDir() + "/user/cache/"
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
		} else if(cr.eeVer == 2) {
			command += " && chmod -R 777 " + sysDest + cr.getSysDir() + "/expressionengine/cache/"
					+ " && chmod -R 777 " + sysDest + cr.getSysDir() + "/expressionengine/templates/"
					+ " && chmod 666 " + sysDest + cr.getSysDir() + "/expressionengine/config/config.php"
					+ " && chmod 666 " + sysDest + cr.getSysDir() + "/expressionengine/config/database.php"
					+ " && chmod -R 755 " + appDest + "/themes/"
					+ uploadDirPerms
					+ " && chmod -R 777 " + appDest + "/images/avatars/"
					+ " && chmod -R 777 " + appDest + "/images/captchas/"
					+ " && chmod -R 777 " + appDest + "/images/member_photos/"
					+ " && chmod -R 777 " + appDest + "/images/pm_attachments/"
					+ " && chmod -R 777 " + appDest + "/images/signature_attachments/"
					+ " && chmod -R 777 " + appDest + "/images/uploads/";
		} else {
			System.out.println("ExpressionEngine version " + cr.eeVer + " not supported, please update config and try again.");
			System.exit(1);
		}

		// Show user the command we're sending
		System.out.println(EEExtras.ANSI_PURPLE + "\tremote | " + EEExtras.ANSI_RESET + command);

		// Create the session and execute command on desired environment
		String ssh = "";
		if( cr.useKeyAuth ) {
			ssh = "ssh -i " + cr.getKeyfile() + " " + config.getSshUser() + "@" + config.getHost();
		} else {
			ssh = EEExtras.SSHPASSPATH + "sshpass -e ssh " + config.getSshUser() + "@" + config.getHost();
		}

		String commandWithAuth = ssh + " '" + command + "'";

		if( ! this.ce.executeCommand( commandWithAuth ) ) {
			consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_RED + "Error: unable to execute MYSQL command " + EEExtras.ANSI_RESET, 80, '▬');
			System.out.println(consolMsg);
			System.out.println(EEExtras.ANSI_YELLOW + "Please double check your credentials and eemove config file and try again" + EEExtras.ANSI_RESET);
			System.exit(-1);
		}

		consolMsg = Strings.padEnd(
				"▬▬ ✓ " + EEExtras.ANSI_CYAN + "Complete! " + EEExtras.ANSI_RESET, 80, '▬');
		System.out.println(consolMsg);
	}

}
