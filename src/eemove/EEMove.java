/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 *
 */

package eemove;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import com.google.common.base.Strings;

import db.DBPushPull;
import helpers.ConfigReader;
import helpers.EEconfig;
import helpers.PermissionsFixer;
import rsync.EEPushPull;
import util.EEExtras;

public class EEMove implements EEExtras {

	// Our primary variables
	private HashMap<String, EEconfig> config;
	private File rsyncIgnore = new File("eemove.ignore");
	// EE folder structure configuration, set some defaults just in case
	private String eeApp;
	private String eeSystem;
	private String uploadDir;
	private ConfigReader cr;
	private boolean eeAboveRoot = true;

	// Since this program will be run from command line, add main method
	public static void main(String[] args) {
		System.out.println(
				EEExtras.ANSI_YELLOW + "****************************************************" + EEExtras.ANSI_RESET);
		System.out.println(EEExtras.ANSI_YELLOW + "Welcome to eemove, starting engines!..." + EEExtras.ANSI_RESET);
		// Start 'er up!!
		new EEMove();
	}

	// Constructor
	public EEMove() {
		try {
			// Attempt to create eemove ignore file, if not created
			eemoveIgnore();
			// Instantiate the ConfigReader class for reading and creating our
			// config file
			cr = new ConfigReader("eemove.config");
			System.out.print(EEExtras.ANSI_YELLOW + "Loading config file..." + EEExtras.ANSI_RESET);
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
			this.eeApp = cr.getAppDir();
			this.eeSystem = cr.getSysDir();
			this.uploadDir = cr.getUpDir();
			this.eeAboveRoot = cr.isAboveRoot();
			/*
			 * Done loading config and getting needed startup info
			 */
			System.out.print(EEExtras.ANSI_YELLOW + "all done!\n" + EEExtras.ANSI_RESET);
			// Show user the environment we've loaded
			System.out
					.print(EEExtras.ANSI_YELLOW + "Your currently configured environments are: " + EEExtras.ANSI_RESET);
			String configurations = EEExtras.ANSI_YELLOW + "";
			for (String key : config.keySet()) {
				configurations += "\"" + key + "\", ";
			}
			configurations += EEExtras.ANSI_RESET;
			System.out.print(configurations.substring(0, configurations.length() - 2) + "\n");
			configurations.substring(0, configurations.length() - 2);
			System.out.println(EEExtras.ANSI_YELLOW + "****************************************************"
					+ EEExtras.ANSI_RESET);
		} catch (Exception e) { // Catch generic exception
			System.out.println(
					"There was an error creating the config file, please ensure the directory eemove is writeable.");
			e.printStackTrace();
			System.exit(0);
		}
		// If here, config file successfully wrote, or read.
		syncItUp();
	}

	/*
	 * Start working!
	 * TODO break this up into separate methods to make things
	 * 		cleaner and easier to read!
	 */
	private void syncItUp() {
		Scanner scan = new Scanner(System.in);
		boolean done = false;
		String parts[];
		String pushPull = "";
		String runType = "";
		String environment = "";
		String directory = "";
		while (!done) {
			System.out.print(EEExtras.ANSI_GREEN
					+ "Please enter a command [example: \"push -d staging uploads\"] or type \"help\" for examples: "
					+ EEExtras.ANSI_RESET);
			String command = scan.nextLine().trim();
			if (command.equalsIgnoreCase("help")) {
				System.out.println(exampleCmd());
			} else {
				// Get the command pieces
				parts = command.split(" ");
				
				// Determine what the command is
				if (parts.length == 2 && parts[0].equalsIgnoreCase("fixperms")) {
					if (config.get(parts[1]) == null) {
						System.out.println("Unable to find environment entered in config file, please try again.");
					} else {
						new PermissionsFixer(this.cr, config.get(parts[1]));
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
						EEconfig thisConfig = config.get(environment);
						// Get the directory parts
						String envDirParts[] = thisConfig.getDirectory().split("/");
						// Determine app and system destination directories
						String sysDest = "";
						String appDest = thisConfig.getDirectory();
						if (eeAboveRoot == true) {
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
						sysDest += eeSystem;
						String appSrc = eeApp;
						String sysSrc = eeSystem;
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
							String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + pushPull + EEExtras.ANSI_RESET + " All ", 80, '▬');
							System.out.println(consolMsg);
							appSrc += "/";
							appDest += "/";
							sysSrc += "/";
							sysDest += "/";
							new EEPushPull(appSrc, appDest, type, isDryRun, thisConfig, cr);
							new EEPushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
						} else if (directory.equalsIgnoreCase("addons")) {
							// Push plugin directories to environment
							String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + pushPull + EEExtras.ANSI_RESET + " Add-ons ", 80, '▬');
							System.out.println(consolMsg);
							if(cr.eeVer == 3) {
								appSrc += EEExtras.EE3_ADDONS_THEMES;
								appDest += EEExtras.EE3_ADDONS_THEMES;
								sysSrc += EEExtras.EE3_ADDONS_FILES;
								sysDest += EEExtras.EE3_ADDONS_FILES;
							} else if(cr.eeVer == 2) {
								appSrc += EEExtras.EE2_ADDONS_THEMES;
								appDest += EEExtras.EE2_ADDONS_THEMES;
								sysSrc += EEExtras.EE2_ADDONS_FILES;
								sysDest += EEExtras.EE2_ADDONS_FILES;
							} else {
								System.out.println("ExpressionEngine version " + cr.eeVer + " not supported, please update config and try again.");
								System.exit(1);
							}
							new EEPushPull(appSrc, appDest, type, isDryRun, thisConfig, cr);
							new EEPushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
						}else if (directory.equalsIgnoreCase("update")) {
							// Push plugin directories to environment
							String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + pushPull + EEExtras.ANSI_RESET + " Update files ", 80, '▬');
							System.out.println(consolMsg);
							String configSrc = "";
							String configDest = "";
							if(cr.eeVer == 3) {
								configSrc = sysSrc + EEExtras.EE3_CONFIG_FILE;
								configDest = sysDest + EEExtras.EE3_CONFIG_FILE;
								appSrc += EEExtras.EE3_SYSTEM_THEMES;
								appDest += EEExtras.EE3_SYSTEM_THEMES;
								sysSrc += EEExtras.EE3_SYSTEM_FILES;
								sysDest += EEExtras.EE3_SYSTEM_FILES;
							} else if(cr.eeVer == 2) {
								configSrc = sysSrc + EEExtras.EE2_CONFIG_FILE;
								configDest = sysDest + EEExtras.EE2_CONFIG_FILE;
								appSrc += EEExtras.EE2_SYSTEM_THEMES;
								appDest += EEExtras.EE2_SYSTEM_THEMES;
								sysSrc += EEExtras.EE2_SYSTEM_FILES;
								sysDest += EEExtras.EE2_SYSTEM_FILES;
							} else {
								System.out.println("ExpressionEngine version " + cr.eeVer + " not supported, please update config and try again.");
								System.exit(1);
							}
							
							new EEPushPull(appSrc, appDest, type, isDryRun, thisConfig, cr);
							new EEPushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
							new EEPushPull(configSrc, configDest, type, isDryRun, thisConfig, cr);
						} else if (directory.equalsIgnoreCase("templates")) {
							// Push theme directory to environment
							String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + pushPull + EEExtras.ANSI_RESET + " Templates ", 80, '▬');
							System.out.println(consolMsg);
							if(cr.eeVer == 3) {
								appSrc += EEExtras.EE3_TEMPLATE_RESOURCES;
								appDest += EEExtras.EE3_TEMPLATE_RESOURCES;
								sysSrc += EEExtras.EE3_TEMPLATES;
								sysDest += EEExtras.EE3_TEMPLATES;
							} else if(cr.eeVer == 2) {
								appSrc += EEExtras.EE2_TEMPLATE_RESOURCES;
								appDest += EEExtras.EE2_TEMPLATE_RESOURCES;
								sysSrc += EEExtras.EE2_TEMPLATES;
								sysDest += EEExtras.EE2_TEMPLATES;
							} else {
								System.out.println("ExpressionEngine version " + cr.eeVer + " not supported, please update config and try again.");
								System.exit(1);
							}
							
							new EEPushPull(appSrc, appDest, type, isDryRun, thisConfig, cr);
							new EEPushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
						} else if (directory.equalsIgnoreCase("uploads")) {
							// Push upload directories to environment
							String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + pushPull + EEExtras.ANSI_RESET + " Uploads ", 80, '▬');
							System.out.println(consolMsg);
							String uploadSrc = "";
							String uploadDest = "";
							if(cr.eeVer == 3) {
								uploadSrc = appSrc + EEExtras.EE3_IMAGE_UPLOADS;
								uploadDest = appDest + EEExtras.EE3_IMAGE_UPLOADS;
							} else if(cr.eeVer == 2) {
								uploadSrc = appSrc + EEExtras.EE2_IMAGE_UPLOADS;
								uploadDest = appDest + EEExtras.EE2_IMAGE_UPLOADS;
							} else {
								System.out.println("ExpressionEngine version " + cr.eeVer + " not supported, please update config and try again.");
								System.exit(1);
							}
							
							new EEPushPull(uploadSrc, uploadDest, type, isDryRun, thisConfig, cr);
							if (!uploadDir.equals("")) {
								consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + pushPull + EEExtras.ANSI_RESET + " Custom Uploads Directory ", 80, '▬');
								System.out.println(consolMsg);
								String customUploadSrc = appSrc + "/" + uploadDir + "/";
								String customUploadDest = appDest + "/" + uploadDir + "/";
								new EEPushPull(customUploadSrc, customUploadDest, type, isDryRun, thisConfig, cr);
							}
						} else if (directory.equalsIgnoreCase("system")) {
							// Push system directory to environment
							String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + pushPull + EEExtras.ANSI_RESET + " System ", 80, '▬');
							System.out.println(consolMsg);
							sysSrc += "/";
							sysDest += "/";
							new EEPushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
						} else if (directory.equalsIgnoreCase("app")) {
							// Push app directory to environment
							String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + pushPull + EEExtras.ANSI_RESET + " App ", 80, '▬');
							System.out.println(consolMsg);
							appSrc += "/";
							appDest += "/";
							new EEPushPull(appSrc, appDest, type, isDryRun, thisConfig, cr);
						} else if (directory.equalsIgnoreCase("custom")) {
							// Push app directory to environment
							String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + pushPull + EEExtras.ANSI_RESET + " Custom Directory ", 80, '▬');
							System.out.println(consolMsg);
							String source, destination;
							
							System.out.println(EEExtras.ANSI_YELLOW + "[Note: for syncing directories recursivley be sure to include trailing slash (\"/\")]" + EEExtras.ANSI_RESET);
							System.out.print(EEExtras.ANSI_GREEN + "Enter the local path (relative to " + EEExtras.CWD + "): " + EEExtras.ANSI_RESET);
							source = scan.nextLine();
							System.out.print(EEExtras.ANSI_GREEN + "Enter the remote path (enter an absolute path here): " + EEExtras.ANSI_RESET);
							destination = scan.nextLine();
							System.out.println();
							
							if (type.equalsIgnoreCase("push")) {
								System.out.println(EEExtras.ANSI_CYAN + pushPull + " \"" + EEExtras.CWD + source + "\" to \"" + thisConfig.getSshUser() + "@" + thisConfig.getHost() + ":" + destination + "\"" + EEExtras.ANSI_RESET);
							} else {
								System.out.println(EEExtras.ANSI_CYAN + pushPull + " \"" + thisConfig.getSshUser() + "@" + thisConfig.getHost() + ":" + destination + "\" to \"" + EEExtras.CWD + source + "\"" + EEExtras.ANSI_RESET);
							}
							
							System.out.print("\n" + EEExtras.ANSI_YELLOW + "Is this correct? (Y/N): " + EEExtras.ANSI_RESET);
							String proceed = scan.nextLine();
							
							if( proceed.equalsIgnoreCase("y") || proceed.equalsIgnoreCase("yes") ) {
								new EEPushPull(source, destination, type, isDryRun, thisConfig, cr);
							} else {
								System.out.println(EEExtras.ANSI_YELLOW + "Operation canceled.\n" + EEExtras.ANSI_RESET);
							}
							
						} else if (directory.equalsIgnoreCase("database")) {
							// Database push/pull doesn't support dry-run, tell
							// user
							if (isDryRun) {
								System.out.println(EEExtras.ANSI_RED
										+ "Database push/pull does not support \"dry\" runs, please use the \"-l\" flag instead."
										+ EEExtras.ANSI_RESET);
							} else {
								if (config.get("local") == null) {
									System.out.println(EEExtras.ANSI_RED
											+ "You do not have an environment for \"local\", please add one to your \"eemove.config\" file and try again."
											+ EEExtras.ANSI_RESET);
								} else {
									String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + pushPull + EEExtras.ANSI_RESET + " Database ", 80, '▬');
									System.out.println(consolMsg);
									new DBPushPull(thisConfig, config.get("local"), type, cr);
								}
							}
						} else {
							System.out.println("The command you entered is invalid, please try again.");
						}

					}
				}
			}
		}
		scan.close();
	}

	/*
	 * Example commands
	 */
	private String exampleCmd() {
		String returnString = "";
		returnString += EEExtras.ANSI_YELLOW + "\n[Note] the flags [-d, -l] represent dry and live runs repsectively.\n"
				+ "To see what will be transfered without actually transfering anything use the -d flag.\n"
				+ "[Note] must use the -l flag if doing a database push/pull.\n\n";
		returnString += "[ Helper examples ]\n";
		returnString += "\"fixperms staging\"\t\t(attempts to fix permissions on selected enviroonment using \"chmod\" command)\n\n";
		returnString += "[ Push examples ]\n";
		returnString += "\"push -l staging all\"\t\t(pushes app and system directories to desired environment)\n";
		returnString += "\"push -l staging addons\"\t(pushes add-ons to desired environment)\n";
		returnString += "\"push -l production templates\"\t(pushes templates to desired environment)\n";
		returnString += "\"push -l production uploads\"\t(pushes uploads to desired environment)\n";
		returnString += "\"push -l production system\"\t(pushes system directory to desired environment)\n";
		returnString += "\"push -l production database\"\t(pushes database to desired environment)\n";
		returnString += "\"push -l production custom\"\t(will be prompted for source and destination)\n";
		returnString += "\"push -l staging update\"\t(pushes the system/ee and app/themes/ee directories as well as the system/user/config/config.php file)\n";
		returnString += "\n[ Pull examples ]\n";
		returnString += "\"pull -l production addons\"\t(pulls add-ons from desired environment)\n";
		returnString += "\"pull -l staging templates\"\t(pulls templates from desired environment)\n";
		returnString += "\"pull -l production uploads\"\t(pulls uploads from desired environment)\n";
		returnString += "\"pull -l staging app\"\t\t(pulls app directory from desired environment)\n";
		returnString += "\"pull -l staging database\"\t(pulls database from desired environment)\n\n" + EEExtras.ANSI_RESET;
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

}
