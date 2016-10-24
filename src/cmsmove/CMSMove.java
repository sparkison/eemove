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

import db.DBPushPull;
import expressionengine.EEConfigReader;
import expressionengine.EEPermissionsFixer;
import helpers.Config;
import rsync.PushPull;
import util.Extras;

public class CMSMove implements Extras {

	// Our primary variables
	private HashMap<String, Config> config;
	private File rsyncIgnore = new File("move.ignore");
	// EE folder structure configuration, set some defaults just in case
	private String cmsApp;
	private String cmsSystem;
	private String uploadDir;
	private EEConfigReader cr;
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
			try {
				// Attempt to create move ignore file, if not created
				eemoveIgnore();
				// Instantiate the ConfigReader class for reading and creating our
				// config file
				cr = new EEConfigReader("move.config");
				// System.out.print(EEExtras.ANSI_YELLOW + "Loading config file..." + EEExtras.ANSI_RESET);
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
			// If here, config file successfully wrote, or read.
			syncItUp();
		}

	}

	/*
	 * Start working!
	 * TODO break this up into separate methods to make things
	 * 		cleaner and easier to read!
	 */
	private void syncItUp() {
		Scanner scan = new Scanner(System.in);
		String parts[] = this.arguments;
		String pushPull = "";
		String runType = "";
		String environment = "";
		String directory = "";

		// Determine what the command is
		if (parts.length == 2 && parts[0].equalsIgnoreCase("fixperms")) {
			if (config.get(parts[1]) == null) {
				System.out.println("Unable to find environment entered in config file, please try again.");
			} else {
				new EEPermissionsFixer(this.cr, config.get(parts[1]));
			}
		}
		// If not fix perms, need 4 arguments
		else if (parts.length < 4) {
			System.out.println("The command you entered is invalid, please try again.");
		} else {
			// Set our variables based on arguments passed
			pushPull = parts[0];
			runType = parts[1];
			environment = parts[2];
			directory = parts[3];
			// Make sure we can get the config (is it valid)
			if (config.get(environment) == null) {
				System.out.println("Unable to find environment entered in config file, please try again.");
			} else {
				// Get the configuration
				Config thisConfig = config.get(environment);
				// Get the directory parts
				String envDirParts[] = thisConfig.getDirectory().split("/");
				// Determine app and system destination directories
				String sysDest = "";
				String appDest = thisConfig.getDirectory();
				if (appAboveRoot == true) {
					for (int i = 0; i < envDirParts.length - 1; i++) {
						if (envDirParts[i].equals("")) {
							sysDest += "/";
						} else {
							sysDest += envDirParts[i] + "/";
						}
					}
				} else {
					sysDest = thisConfig.getDirectory() + "/";
				}
				sysDest += cmsSystem;
				String appSrc = cmsApp;
				String sysSrc = cmsSystem;
				String type = "";
				boolean isDryRun = true;

				// Are we pushing or pulling
				if (pushPull.equalsIgnoreCase("push")) {
					type = "push";
					pushPull = "Pushing";
				} else {
					type = "pull";
					pushPull = "Pulling";
				}

				// Is it a "dry-run" or not
				if (runType.equalsIgnoreCase("-d"))
					isDryRun = true;
				else
					isDryRun = false;

				// Start working
				if (directory.equalsIgnoreCase("all")) {
					// Push all contents of app and system
					// directories to environment
					String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " All ", 80, '▬');
					System.out.println(consolMsg);
					appSrc += "/";
					appDest += "/";
					sysSrc += "/";
					sysDest += "/";
					new PushPull(appSrc, appDest, type, isDryRun, thisConfig, cr);
					new PushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
				} else if (directory.equalsIgnoreCase("addons") || directory.equalsIgnoreCase("-a")) {
					// Push plugin directories to environment
					String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Add-ons ", 80, '▬');
					System.out.println(consolMsg);
					if(cr.getCmsVer() == 3) {
						appSrc += Extras.EE3_ADDONS_THEMES;
						appDest += Extras.EE3_ADDONS_THEMES;
						sysSrc += Extras.EE3_ADDONS_FILES;
						sysDest += Extras.EE3_ADDONS_FILES;
					} else if(cr.getCmsVer() == 2) {
						appSrc += Extras.EE2_ADDONS_THEMES;
						appDest += Extras.EE2_ADDONS_THEMES;
						sysSrc += Extras.EE2_ADDONS_FILES;
						sysDest += Extras.EE2_ADDONS_FILES;
					} else {
						System.out.println("ExpressionEngine version " + cr.getCmsVer() + " not supported, please update config and try again.");
						System.exit(1);
					}
					new PushPull(appSrc, appDest, type, isDryRun, thisConfig, cr);
					new PushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
				}else if (directory.equalsIgnoreCase("update")) {
					// Push plugin directories to environment
					String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Update files ", 80, '▬');
					System.out.println(consolMsg);
					String configSrc = "";
					String configDest = "";
					if(cr.getCmsVer() == 3) {
						configSrc = sysSrc + Extras.EE3_CONFIG_FILE;
						configDest = sysDest + Extras.EE3_CONFIG_FILE;
						appSrc += Extras.EE3_SYSTEM_THEMES;
						appDest += Extras.EE3_SYSTEM_THEMES;
						sysSrc += Extras.EE3_SYSTEM_FILES;
						sysDest += Extras.EE3_SYSTEM_FILES;
					} else if(cr.getCmsVer() == 2) {
						configSrc = sysSrc + Extras.EE2_CONFIG_FILE;
						configDest = sysDest + Extras.EE2_CONFIG_FILE;
						appSrc += Extras.EE2_SYSTEM_THEMES;
						appDest += Extras.EE2_SYSTEM_THEMES;
						sysSrc += Extras.EE2_SYSTEM_FILES;
						sysDest += Extras.EE2_SYSTEM_FILES;
					} else {
						System.out.println("ExpressionEngine version " + cr.getCmsVer() + " not supported, please update config and try again.");
						System.exit(1);
					}

					new PushPull(appSrc, appDest, type, isDryRun, thisConfig, cr);
					new PushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
					new PushPull(configSrc, configDest, type, isDryRun, thisConfig, cr);
				} else if (directory.equalsIgnoreCase("templates") || directory.equalsIgnoreCase("-t")) {
					// Push theme directory to environment
					String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Templates ", 80, '▬');
					System.out.println(consolMsg);
					if(cr.getCmsVer() == 3) {
						appSrc += Extras.EE3_TEMPLATE_RESOURCES;
						appDest += Extras.EE3_TEMPLATE_RESOURCES;
						sysSrc += Extras.EE3_TEMPLATES;
						sysDest += Extras.EE3_TEMPLATES;
					} else if(cr.getCmsVer() == 2) {
						appSrc += Extras.EE2_TEMPLATE_RESOURCES;
						appDest += Extras.EE2_TEMPLATE_RESOURCES;
						sysSrc += Extras.EE2_TEMPLATES;
						sysDest += Extras.EE2_TEMPLATES;
					} else {
						System.out.println("ExpressionEngine version " + cr.getCmsVer() + " not supported, please update config and try again.");
						System.exit(1);
					}

					new PushPull(appSrc, appDest, type, isDryRun, thisConfig, cr);
					new PushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
				} else if (directory.equalsIgnoreCase("uploads") || directory.equalsIgnoreCase("-u")) {
					// Push upload directories to environment
					String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Uploads ", 80, '▬');
					System.out.println(consolMsg);
					String uploadSrc = "";
					String uploadDest = "";
					if(cr.getCmsVer() == 3) {
						uploadSrc = appSrc + Extras.EE3_IMAGE_UPLOADS;
						uploadDest = appDest + Extras.EE3_IMAGE_UPLOADS;
					} else if(cr.getCmsVer() == 2) {
						uploadSrc = appSrc + Extras.EE2_IMAGE_UPLOADS;
						uploadDest = appDest + Extras.EE2_IMAGE_UPLOADS;
					} else {
						System.out.println("ExpressionEngine version " + cr.getCmsVer() + " not supported, please update config and try again.");
						System.exit(1);
					}

					new PushPull(uploadSrc, uploadDest, type, isDryRun, thisConfig, cr);
					if (!uploadDir.equals("")) {
						consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Custom Uploads Directory ", 80, '▬');
						System.out.println(consolMsg);
						String customUploadSrc = appSrc + "/" + uploadDir + "/";
						String customUploadDest = appDest + "/" + uploadDir + "/";
						new PushPull(customUploadSrc, customUploadDest, type, isDryRun, thisConfig, cr);
					}
				} else if (directory.equalsIgnoreCase("system")) {
					// Push system directory to environment
					String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " System ", 80, '▬');
					System.out.println(consolMsg);
					sysSrc += "/";
					sysDest += "/";
					new PushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
				} else if (directory.equalsIgnoreCase("app")) {
					// Push app directory to environment
					String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " App ", 80, '▬');
					System.out.println(consolMsg);
					appSrc += "/";
					appDest += "/";
					new PushPull(appSrc, appDest, type, isDryRun, thisConfig, cr);
				} else if (directory.equalsIgnoreCase("custom")) {
					// Push app directory to environment
					String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Custom Directory ", 80, '▬');
					System.out.println(consolMsg);
					String source, destination;

					System.out.println(Extras.ANSI_YELLOW + "[Note: for syncing directories recursivley be sure to include trailing slash (\"/\")]" + Extras.ANSI_RESET);
					System.out.print(Extras.ANSI_GREEN + "Enter the local path (relative to " + Extras.CWD + "): " + Extras.ANSI_RESET);
					source = scan.nextLine();
					System.out.print(Extras.ANSI_GREEN + "Enter the remote path (enter an absolute path here): " + Extras.ANSI_RESET);
					destination = scan.nextLine();
					System.out.println();

					if (type.equalsIgnoreCase("push")) {
						System.out.println(Extras.ANSI_CYAN + pushPull + " \"" + Extras.CWD + source + "\" to \"" + thisConfig.getSshUser() + "@" + thisConfig.getHost() + ":" + destination + "\"" + Extras.ANSI_RESET);
					} else {
						System.out.println(Extras.ANSI_CYAN + pushPull + " \"" + thisConfig.getSshUser() + "@" + thisConfig.getHost() + ":" + destination + "\" to \"" + Extras.CWD + source + "\"" + Extras.ANSI_RESET);
					}

					System.out.print("\n" + Extras.ANSI_YELLOW + "Is this correct? (Y/N): " + Extras.ANSI_RESET);
					String proceed = scan.nextLine();

					if( proceed.equalsIgnoreCase("y") || proceed.equalsIgnoreCase("yes") ) {
						new PushPull(source, destination, type, isDryRun, thisConfig, cr);
					} else {
						System.out.println(Extras.ANSI_YELLOW + "Operation canceled.\n" + Extras.ANSI_RESET);
					}

				} else if (directory.equalsIgnoreCase("database") || directory.equalsIgnoreCase("-d")) {
					// Database push/pull doesn't support dry-run, tell
					// user
					if (isDryRun) {
						System.out.println(Extras.ANSI_RED
								+ "Database push/pull does not support \"dry\" runs, please use the \"-l\" flag instead."
								+ Extras.ANSI_RESET);
					} else {
						if (config.get("local") == null) {
							System.out.println(Extras.ANSI_RED
									+ "You do not have an environment for \"local\", please add one to your \"move.config\" file and try again."
									+ Extras.ANSI_RESET);
						} else {
							String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Database ", 80, '▬');
							System.out.println(consolMsg);
							new DBPushPull(thisConfig, config.get("local"), type, cr);
						}
					}
				} else {
					System.out.println("The command you entered is invalid, please try again.");
				}

			}
		}
		scan.close();
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
	private void eemoveIgnore() throws IOException {
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
	private void craftmoveIgnore() throws IOException {
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
