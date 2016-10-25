/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 *
 */

package cmsmove;

import java.io.File;
import java.util.HashMap;

import com.google.common.base.Strings;

import craft.CRAFTConfigReader;
import craft.CRAFTIgnore;
import craft.CRAFTSync;
import expressionengine.EEConfigReader;
import expressionengine.EEIgnore;
import expressionengine.EESync;
import helpers.Config;
import helpers.ConfigReader;
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

	/**
	 * Since this program will be run from command line, add main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Start 'er up!!
		new CMSMove(args);
	}

	/**
	 * Constructor
	 * 
	 * @param args
	 */
	public CMSMove(String[] args) {

		// Set the arguments for other methods to use
		this.arguments = args;
		// See if we have enough (need at least two)
		if(args.length < 2) {
			System.out.println(Extras.ANSI_RED + "Incorrect number of arguments supplied. Example of valid commands:" + Extras.ANSI_RESET);

			// Show example help commands
			System.out.println(helpCommand());

			// Exit
			System.exit(0);
		} else {
			String cmsType = "";
			if(args[0].equalsIgnoreCase("init")) {
				/*
				 * Initialize cmsmove with desired CMS
				 */
				cmsType = args[1];
				if(cmsType.equalsIgnoreCase("craft")) {
					cmsType = "craft";
				} else if(cmsType.equalsIgnoreCase("ee") || cmsType.equalsIgnoreCase("expressionengine")) {
					cmsType = "ee";
				}
			} else if(args[0].equalsIgnoreCase("help")) {
				/*
				 * Show example commands for specified CMS
				 */
				String helpType = args[1];
				if(helpType.equalsIgnoreCase("craft")) {
					System.out.println(Extras.ANSI_CYAN + "Craft example commands:" + Extras.ANSI_RESET);
					System.out.println(craftExampleCmd());
				} else if(helpType.equalsIgnoreCase("ee") || helpType.equalsIgnoreCase("expressionengine")) {
					System.out.println(Extras.ANSI_CYAN + "ExpressionEngine example commands:" + Extras.ANSI_RESET);
					System.out.println(eeExampleCmd());
				}
				// Exit
				System.exit(0);
			}
			try {
				// Attempt to create move ignore file, if not created
				if (cmsType.equals("craft")) {
					CRAFTIgnore ignore = new CRAFTIgnore(new File("craftmove.ignore"));
					ignore.init();
				} else if (cmsType.equals("ee")) {
					EEIgnore ignore = new EEIgnore(new File("eemove.ignore"));
					ignore.init();
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

						// Show init examples
						System.out.println(helpCommand());

						// Exit
						System.exit(0);
					}
				}

				// Instantiate the ConfigReader class for reading and creating our config file
				if (cmsType.equals("craft")) {
					cr = new CRAFTConfigReader("craftmove.config");
					// Set the ignore file name
					cr.setIgnoreFile("craftmove.ignore");
				} else if (cmsType.equals("ee")) {
					cr = new EEConfigReader("eemove.config");
					cr.setIgnoreFile("eemove.ignore");
				}

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

	/**
	 * Init and help example commands
	 * 
	 * @return String
	 */
	public String helpCommand() {
		String returnString = Strings.padEnd("▬▬ " + Extras.ANSI_YELLOW + " Examples:" , 80, '▬');
		returnString += "\n[ Init command (required if no config files present) ]\n";
		returnString += "\"cmsmove init craft\"\t\t\t\t(initializes config files for Craft)\n";
		returnString += "\"cmsmove init ee\"\t\t\t\t(initializes config files for ExpressionEngine)\n\n";

		returnString += "\n[ Help command ]\n";
		returnString += "\"cmsmove craft help\"\t\t\t\t(display list of Craft specific commands)\n";
		returnString += "\"cmsmove ee help\"\t\t\t\t(display list of ExpressionEngine specific commands)\n\n";
		
		returnString += Extras.ANSI_RESET;

		return returnString;
	}

	/**
	 * Craft Example commands
	 * 
	 * @return String
	 */
	private String craftExampleCmd() {
		String returnString = Extras.ANSI_YELLOW + "_________________________________________________________________\n";
		returnString += "\n[ Notes ]\n1) the flags [-d, -l] represent dry and live runs repsectively. "
				+ "To see what will be transfered without actually transfering anything use the -d flag.\n"
				+ "2) must use the -l flag if doing a database push/pull.\n"
				+ "3) command shorthand denoted within parenthesis. E.g. the shorthand for 'templates' is '-t' \n\n";
		returnString += "[ Helper examples ]\n";
		returnString += "\"cmsmove fixperms staging\"\t\t\t(attempts to fix permissions on selected environment using \"chmod\" command)\n\n";
		returnString += "[ Push examples ]\n";
		returnString += "\"cmsmove push -l staging all\"\t\t\t(pushes public_html and core system directories to desired environment)\n";
		returnString += "\"cmsmove push -l staging plugins(-p)\"\t\t(pushes plugins to desired environment)\n";
		returnString += "\"cmsmove push -l production templates(-t)\"\t(pushes templates to desired environment)\n";
		returnString += "\"cmsmove push -l production uploads(-u)\"\t(pushes uploads to desired environment)\n";
		returnString += "\"cmsmove push -l production craft\"\t\t(pushes Craft core directory to desired environment)\n";
		returnString += "\"cmsmove push -l production database(-d)\"\t(pushes database to desired environment)\n";
		returnString += "\"cmsmove push -l production custom(-c)\"\t\t(will be prompted for source and destination)\n";
		returnString += "\n[ Pull examples ]\n";
		returnString += "\"cmsmove pull -l production addons(-a)\"\t\t(pulls add-ons from desired environment)\n";
		returnString += "\"cmsmove pull -l staging templates(-t)\"\t\t(pulls templates from desired environment)\n";
		returnString += "\"cmsmove pull -l production uploads(-u)\"\t(pulls uploads from desired environment)\n";
		returnString += "\"cmsmove pull -l staging public\"\t\t(pulls public_html directory from desired environment)\n";
		returnString += "\"cmsmove pull -l staging database(-d)\"\t\t(pulls database from desired environment)\n\n" + Extras.ANSI_RESET;
		return returnString;
	}

	/**
	 * ExpressionEngine Example commands
	 * 
	 * @return String
	 */
	private String eeExampleCmd() {
		String returnString = Extras.ANSI_YELLOW + "_________________________________________________________________\n";
		returnString += "\n[ Notes ]\n1) the flags [-d, -l] represent dry and live runs repsectively. "
				+ "To see what will be transfered without actually transfering anything use the -d flag.\n"
				+ "2) must use the -l flag if doing a database push/pull.\n"
				+ "3) command shorthand denoted within parenthesis. E.g. the shorthand for 'templates' is '-t' \n\n";
		returnString += "[ Helper examples ]\n";
		returnString += "\"cmsmove fixperms staging\"\t\t\t(attempts to fix permissions on selected environment using \"chmod\" command)\n\n";
		returnString += "[ Push examples ]\n";
		returnString += "\"cmsmove push -l staging all\"\t\t\t(pushes public_html and core system directories to desired environment)\n";
		returnString += "\"cmsmove push -l staging addons(-a)\"\t\t(pushes add-ons to desired environment)\n";
		returnString += "\"cmsmove push -l production templates(-t)\"\t(pushes templates to desired environment)\n";
		returnString += "\"cmsmove push -l production uploads(-u)\"\t(pushes uploads to desired environment)\n";
		returnString += "\"cmsmove push -l production ee\"\t\t(pushes ExpressionEngine core directory to desired environment)\n";
		returnString += "\"cmsmove push -l production database(-d)\"\t(pushes database to desired environment)\n";
		returnString += "\"cmsmove push -l production custom(-c)\"\t\t(will be prompted for source and destination)\n";
		returnString += "\"cmsmove push -l staging update\"\t\t(pushes the system/ee and app/themes/ee directories as well as the system/user/config/config.php file)\n";
		returnString += "\n[ Pull examples ]\n";
		returnString += "\"cmsmove pull -l production addons(-a)\"\t\t(pulls add-ons from desired environment)\n";
		returnString += "\"cmsmove pull -l staging templates(-t)\"\t\t(pulls templates from desired environment)\n";
		returnString += "\"cmsmove pull -l production uploads(-u)\"\t(pulls uploads from desired environment)\n";
		returnString += "\"cmsmove pull -l staging public\"\t\t(pulls public_html directory from desired environment)\n";
		returnString += "\"cmsmove pull -l staging database(-d)\"\t\t(pulls database from desired environment)\n\n" + Extras.ANSI_RESET;
		return returnString;
	}

}
