/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 *
 */

package cmsmove;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import com.google.common.base.Strings;

import craft.CRAFTConfigReader;
import craft.CRAFTSync;
import db.DBPushPull;
import expressionengine.EEConfigReader;
import expressionengine.EEPermissionsFixer;
import expressionengine.EESync;
import helpers.Config;
import helpers.ConfigReader;
import rsync.PushPull;
import util.Extras;

public class CMSMove implements Extras {

	// Our primary variables
	private HashMap<String, Config> config;
	// EE folder structure configuration, set some defaults just in case
	private String cmsApp;
	private String cmsSystem;
	private String uploadDir;
	private ConfigReader cr;
	private String[] arguments;
	private boolean appAboveRoot = true;

	// Since this program will be run from command line, add main method
	public static void main(String[] args) {
		// Start 'er up!!
		new CMSMove(args);
	}

	// Constructor
	public CMSMove(String[] args) {

		// Set the arguments for other methods to use
		this.arguments = args;
		// See if we have enough (need at least two)
		if(args.length < 2) {
			System.out.println(Extras.ANSI_RED + "Incorrect number of arguments supplied. Example of valid commands:" + Extras.ANSI_RESET);
			System.out.println(exampleCmd());
			System.exit(0);
		} else {
			String cmsType = "";
			if(args[0].equalsIgnoreCase("init")) {
				cmsType = args[1];
				if(cmsType.equalsIgnoreCase("craft")) {
					cmsType = "craft";
				} else if(cmsType.equalsIgnoreCase("ee") || cmsType.equalsIgnoreCase("expressionengine")) {
					cmsType = "ee";
				}
			}
			try {
				// Attempt to create move ignore file, if not created
				if (cmsType.equals("craft")) {
					File craftIgnore = new File("craftmove.ignore");
					craftmoveIgnore(craftIgnore);
				} else if (cmsType.equals("ee")) {
					File eeIgnore = new File("eemove.ignore");
					eemoveIgnore(eeIgnore);
				} else {
					/*
					 * Not using init command, need to determine which CMS were using
					 */
					File craftIgnore = new File("craftmove.ignore");
					File eeIgnore = new File("eemove.ignore");
					if (craftIgnore.exists()) {
						cmsType = "craft";
					} else if (eeIgnore.exists()) {
						cmsType = "ee";
					} else {
						System.out.println(Extras.ANSI_RED + "No config files found. Please run the \"init\" command to start:" + Extras.ANSI_RESET);
						System.out.println(exampleCmd());
						System.exit(0);
					}
				}
				
				// Instantiate the ConfigReader class for reading and creating our config file
				if (cmsType.equals("craft"))
					cr = new CRAFTConfigReader("craftmove.config");
				else if (cmsType.equals("ee"))
					cr = new EEConfigReader("eemove.config");
				
				/*
				 * Create or load our config file:
				 * If no config file found, will generate a bootstrap one
				 * then prompt user to adjust as needed and exit.
				 */
				this.config = cr.getConfig();
				
				/*
				 * If config file read successfully
				 * grab some of the globals we'll need later
				 */
				this.cmsApp = cr.getAppDir();
				this.cmsSystem = cr.getSysDir();
				this.uploadDir = cr.getUpDir();
				this.appAboveRoot = cr.isAboveRoot();
				
				/*
				 * Done loading config and getting needed startup info
				 */
			} catch (Exception e) { // Catch generic exception
				System.out.println(
						"There was an error creating the config file, please ensure the directory move is writeable.");
				e.printStackTrace();
				System.exit(0);
			}
			
			// String[] arguments, HashMap<String, Config> config, EEConfigReader cr, String cmsApp, String cmsSystem, String uploadDir, boolean appAboveRoot
			
			// Start working!
			if (cmsType.equals("craft"))
				new CRAFTSync(this.arguments, this.config, this.cr, this.cmsApp, this.cmsSystem, this.uploadDir, this.appAboveRoot);
			else if (cmsType.equals("ee"))
				new EESync(this.arguments, this.config, this.cr, this.cmsApp, this.cmsSystem, this.uploadDir, this.appAboveRoot);

		}

	}

	/*
	 * Example commands
	 */
	private String exampleCmd() {
		String returnString = Extras.ANSI_YELLOW + "_________________________________________________________________\n";
		returnString += "\n[ Notes ]\n1) the flags [-d, -l] represent dry and live runs repsectively. "
				+ "To see what will be transfered without actually transfering anything use the -d flag.\n"
				+ "2) must use the -l flag if doing a database push/pull.\n"
				+ "3) command shorthand denoted within parenthesis. E.g. the shorthand for 'templates' us '-t' \n\n";
		returnString += "[ Init examples ]\n";
		returnString += "\"cmsmove init craft\"\t\t\t\t(initializes config files for Craft)\n";
		returnString += "\"cmsmove init ee\"\t\t\t\t(initializes config files for ExpressionEngine)\n\n";
		returnString += "[ Helper examples ]\n";
		returnString += "\"cmsmove fixperms staging\"\t\t\t(attempts to fix permissions on selected environment using \"chmod\" command)\n\n";
		returnString += "[ Push examples ]\n";
		returnString += "\"cmsmove push -l staging all\"\t\t\t(pushes app and system directories to desired environment)\n";
		returnString += "\"cmsmove push -l staging addons(-a)\"\t\t(pushes add-ons to desired environment)\n";
		returnString += "\"cmsmove push -l production templates(-t)\"\t(pushes templates to desired environment)\n";
		returnString += "\"cmsmove push -l production uploads(-u)\"\t(pushes uploads to desired environment)\n";
		returnString += "\"cmsmove push -l production system(-s)\"\t\t(pushes system directory to desired environment)\n";
		returnString += "\"cmsmove push -l production database(-d)\"\t(pushes database to desired environment)\n";
		returnString += "\"cmsmove push -l production custom(-c)\"\t\t(will be prompted for source and destination)\n";
		returnString += "\"cmsmove push -l staging update\"\t\t(pushes the system/ee and app/themes/ee directories as well as the system/user/config/config.php file)\n";
		returnString += "\n[ Pull examples ]\n";
		returnString += "\"cmsmove pull -l production addons(-a)\"\t\t(pulls add-ons from desired environment)\n";
		returnString += "\"cmsmove pull -l staging templates(-t)\"\t\t(pulls templates from desired environment)\n";
		returnString += "\"cmsmove pull -l production uploads(-u)\"\t(pulls uploads from desired environment)\n";
		returnString += "\"cmsmove pull -l staging app\"\t\t\t(pulls app directory from desired environment)\n";
		returnString += "\"cmsmove pull -l staging database(-d)\"\t\t(pulls database from desired environment)\n\n" + Extras.ANSI_RESET;
		return returnString;
	}

	/*
	 * See if ignore file exists, and create it if not
	 */
	private void eemoveIgnore(File rsyncIgnore) throws IOException {
		if (!rsyncIgnore.exists()) {
			FileWriter ignoreFile = new FileWriter(rsyncIgnore);
			ignoreFile.write("*.sql");
			ignoreFile.write("\n");
			ignoreFile.write("*.swp");
			ignoreFile.write("\n");
			ignoreFile.write(".git");
			ignoreFile.write("\n");
			ignoreFile.write(".sass-cache");
			ignoreFile.write("\n");
			ignoreFile.write(".DS_Store");
			ignoreFile.write("\n");
			ignoreFile.write("npm-debug.log");
			ignoreFile.write("\n");
			ignoreFile.write("db_backups");
			ignoreFile.write("\n");
			ignoreFile.write("node_modules");
			ignoreFile.write("\n");
			ignoreFile.write("bower_components");
			ignoreFile.write("\n");
			ignoreFile.write("sized/");
			ignoreFile.write("\n");
			ignoreFile.write("thumbs/");
			ignoreFile.write("\n");
			ignoreFile.write("_thumbs/");
			ignoreFile.write("\n");
			ignoreFile.write("# ignore system in the case it's in same directory as app");
			ignoreFile.write("\n");
			ignoreFile.write("system");
			ignoreFile.write("\n");
			ignoreFile.write("/user/cache/");
			ignoreFile.write("\n");
			ignoreFile.write("eemove.config");
			ignoreFile.write("\n");
			ignoreFile.write("eemove.ignore");
			ignoreFile.write("\n");
			ignoreFile.close();
		}
		rsyncIgnore.setExecutable(true);
		rsyncIgnore.setReadable(true);
	}

	/*
	 * See if ignore file exists, and create it if not
	 */
	private void craftmoveIgnore(File rsyncIgnore) throws IOException {
		if (!rsyncIgnore.exists()) {
			FileWriter ignoreFile = new FileWriter(rsyncIgnore);
			ignoreFile.write("*.sql");
			ignoreFile.write("\n");
			ignoreFile.write("*.swp");
			ignoreFile.write("\n");
			ignoreFile.write(".git");
			ignoreFile.write("\n");
			ignoreFile.write(".sass-cache");
			ignoreFile.write("\n");
			ignoreFile.write(".DS_Store");
			ignoreFile.write("\n");
			ignoreFile.write("npm-debug.log");
			ignoreFile.write("\n");
			ignoreFile.write("db_backups");
			ignoreFile.write("\n");
			ignoreFile.write("node_modules");
			ignoreFile.write("\n");
			ignoreFile.write("bower_components");
			ignoreFile.write("\n");
			ignoreFile.write("sized/");
			ignoreFile.write("\n");
			ignoreFile.write("thumbs/");
			ignoreFile.write("\n");
			ignoreFile.write("_thumbs/");
			ignoreFile.write("\n");
			ignoreFile.write("# ignore system in the case it's in same directory as app");
			ignoreFile.write("\n");
			ignoreFile.write("app");
			ignoreFile.write("\n");
			ignoreFile.write("craftmove.config");
			ignoreFile.write("\n");
			ignoreFile.write("craftmove.ignore");
			ignoreFile.write("\n");
			ignoreFile.close();
		}
		rsyncIgnore.setExecutable(true);
		rsyncIgnore.setReadable(true);
	}

}
