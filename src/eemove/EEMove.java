/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 * 
 */

package eemove;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import db.DBPushPull;
import helpers.ConfigReader;
import helpers.EEconfig;
import rsync.EEPushPull;
import util.EEExtras;

public class EEMove implements EEExtras {

	// Our primary variables
	private FileWriter outFile;
	private HashMap<String, EEconfig> config;
	private File configFile = new File("eemove.config");
	private File rsyncIgnore = new File("eemove.ignore");
	// EE folder structure configuration, set some defaults just in case
	private String eeApp = "app";
	private String eeSystem = "system";
	private String uploadDir = "uploads";
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
			ConfigReader cr = new ConfigReader("eemove.config");
			System.out.print(EEExtras.ANSI_YELLOW + "Loading config file..." + EEExtras.ANSI_RESET);
			// Create or load our config file
			this.config = cr.getConfig();
			System.out.print(EEExtras.ANSI_YELLOW + "all done!\n" + EEExtras.ANSI_RESET);
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
	 * TODO break this up into seperate methods to make things
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
				parts = command.split(" ");
				if (parts.length < 4) {
					System.out.println("The command you entered is invalid, please try again.");
				} else {
					pushPull = parts[0];
					runType = parts[1];
					environment = parts[2];
					directory = parts[3];
					if (config.get(environment) == null) {
						System.out.println("Unable to find environment entered in config file, please try again.");
					} else {
						// Get the configuration
						EEconfig thisConfig = config.get(environment);
						// Get the directory parts
						String envDirParts[] = thisConfig.getDirectory().split("/");
						// Determine app and system destination directories
						boolean copyAll = false;
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
							sysDest = thisConfig.getDirectory();
						}
						sysDest += eeSystem;
						String appSrc = eeApp;
						String sysSrc = eeSystem;
						String type = "";
						boolean dryRun = true;

						if (pushPull.equalsIgnoreCase("push"))
							type = "push";
						else
							type = "pull";

						if (runType.equalsIgnoreCase("-d"))
							dryRun = true;
						else
							dryRun = false;

						if (directory.equalsIgnoreCase("all")) {
							// Push all contents of app and system
							// directories to environment
							new EEPushPull(appSrc, appDest, type, dryRun, thisConfig);
							new EEPushPull(sysSrc, sysDest, type, dryRun, thisConfig);
						} else if (directory.equalsIgnoreCase("plugins")) {
							// Push plugin directories to environment
							appSrc += "/themes/user";
							appDest += "/themes/user";
							sysSrc += "/user/addons";
							sysDest += "/user/addons";
							new EEPushPull(appSrc, appDest, type, dryRun, thisConfig);
							new EEPushPull(sysSrc, sysDest, type, dryRun, thisConfig);
						} else if (directory.equalsIgnoreCase("themes")) {
							// Push theme directory to environment
							sysSrc += "/user/templates";
							sysDest += "/user/templates";
							new EEPushPull(sysSrc, sysDest, type, dryRun, thisConfig);
						} else if (directory.equalsIgnoreCase("uploads")) {
							// Push upload directories to environment
							String uploadSrc = appSrc + "/images/uploads";
							String uploadDest = appDest + "/images/uploads";
							new EEPushPull(uploadSrc, uploadDest, type, dryRun, thisConfig);
							if (!uploadDir.equals("")) {
								String customUploadSrc = appSrc + "/" + uploadDir;
								String customUploadDest = appDest + "/" + uploadDir;
								new EEPushPull(customUploadSrc, customUploadDest, type, dryRun, thisConfig);
							}
						} else if (directory.equalsIgnoreCase("system")) {
							// Push system directory to environment
							new EEPushPull(sysSrc, sysDest, type, dryRun, thisConfig);
						} else if (directory.equalsIgnoreCase("app")) {
							// Push app directory to environment
							new EEPushPull(appSrc, appDest, type, dryRun, thisConfig);
						} else if (directory.equalsIgnoreCase("database")) {
							// Database push/pull doesn't support dry-run, tell
							// user
							if (dryRun) {
								System.out.println(EEExtras.ANSI_RED
										+ "Database push/pull does not support \"dry\" runs, please use the \"-l\" flag instead."
										+ EEExtras.ANSI_RESET);
							} else {
								if (config.get("local") == null) {
									System.out.println(EEExtras.ANSI_RED
											+ "You do not have an environment for \"local\", please add one to your \"eemove.config\" file and try again."
											+ EEExtras.ANSI_RESET);
								} else {
									new DBPushPull(thisConfig, config.get("local"), type);
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
		returnString += "[ Push examples ]\n";
		returnString += "\"push -l staging all\"\n";
		returnString += "\"push -l staging plugins\"\n";
		returnString += "\"push -l production themes\"\n";
		returnString += "\"push -l production uploads\"\n";
		returnString += "\"push -l production system\"\n";
		returnString += "\"push -l production database\"\n";
		returnString += "\n[ Pull examples ]\n";
		returnString += "\"pull -l production plugins\"\n";
		returnString += "\"pull -l staging themes\"\n";
		returnString += "\"pull -l production uploads\"\n";
		returnString += "\"pull -l staging app\"\n";
		returnString += "\"pull -l staging database\"\n\n" + EEExtras.ANSI_RESET;
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
			ignoreFile.write("/user/cache/");
			ignoreFile.write("\n");
			ignoreFile.close();
		}
		rsyncIgnore.setExecutable(true);
		rsyncIgnore.setReadable(true);
		Runtime.getRuntime().exec("chmod 777 " + rsyncIgnore.getAbsolutePath());
	}

	/*
	 * Checking to see if there is a config file already created
	 */
	private boolean checkForConfigFile() throws IOException {
		if (configFile.exists()) {
			return true;
		}
		return false;
	}

}
