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
import helpers.EEconfig;
import rsync.EEPushPull;
import util.EEExtras;

public class EEMove implements EEExtras {

	// Our primary variables
	private FileWriter outFile;
	private HashMap<String, EEconfig> config = new HashMap<String, EEconfig>();
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
			// Load config if exists, else create it
			if (checkForConfigFile()) {
				System.out.print(EEExtras.ANSI_YELLOW + "Loading config file..." + EEExtras.ANSI_RESET);
				readConfigFile();
				System.out.print(EEExtras.ANSI_YELLOW + "all done!\n" + EEExtras.ANSI_RESET);
				System.out.print(
						EEExtras.ANSI_YELLOW + "Your currently configured environments are: " + EEExtras.ANSI_RESET);
				String configurations = EEExtras.ANSI_YELLOW + "";
				for (String key : config.keySet()) {
					configurations += "\"" + key + "\", ";
				}
				configurations += EEExtras.ANSI_RESET;
				System.out.print(configurations.substring(0, configurations.length() - 2) + "\n");
				configurations.substring(0, configurations.length() - 2);
				System.out.println(EEExtras.ANSI_YELLOW + "****************************************************"
						+ EEExtras.ANSI_RESET);
			} else {
				System.out.println(EEExtras.ANSI_YELLOW + "No configuration file found, let's create one now."
						+ EEExtras.ANSI_RESET);
				System.out.println(EEExtras.ANSI_YELLOW + "****************************************************"
						+ EEExtras.ANSI_RESET);
				createConfigFile();
			}
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
		returnString += EEExtras.ANSI_YELLOW + "[Note] the flags [-d, -l] represent dry and live runs repsectively.\n"
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
		returnString += "\"pull -l staging database\"\n" + EEExtras.ANSI_RESET;
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

	/*
	 * Create the configuration file
	 * 
	 * format: env=ENVIRONMENT env_dir=DIRECTORY env_user=USER env_pass=PASS
	 * env_db=eemove_staging_db env_dbuser=USER x env_dbpass=PASS
	 * env_dbhost=HOST
	 */
	private void createConfigFile() throws IOException {
		Scanner scan = new Scanner(System.in);
		outFile = new FileWriter(configFile);
		outFile.write("#####################################\n");
		outFile.write("# eemove configuration file. Auto-generated by eemove\n");
		outFile.write("# Add as many environments as needed\n");
		outFile.write("# or delete this file and run eemove to run the automated config generator again\n");
		outFile.write("#####################################\n");
		outFile.write("# Global EE settings\n");
		outFile.write("ee_system=" + eeSystem + "\n");
		outFile.write("ee_app=" + eeApp + "\n");
		outFile.write("# Upload directory path, relative to ee_app\n");
		outFile.write("up_dir=" + uploadDir + "\n");
		outFile.write("above_root=" + eeAboveRoot + "\n");
		outFile.write("#####################################\n");
		outFile.write("# Begin environment specific configuration(s)\n\n");
		boolean done = false;
		while (!done) {
			System.out.print("\nEnter a name for the environment: ");
			String env = scan.nextLine();
			System.out.print(
					"\nEnter hostname/IP address for environment\n(if this is for local dev environment setup can leave blank): ");
			String env_host = scan.nextLine();
			System.out.print(
					"\nEnter the application directory\n(if this is for local dev environment setup, and eemove is in same directory, leave blank): ");
			String env_dir = scan.nextLine().trim();
			System.out.print("\nEnter user name\n(used for SSH connection, leave blank for local dev environment): ");
			String env_user = scan.nextLine().trim();
			System.out
					.print("\nEnter user password\n(used for SSH connection, leave blank for local dev environment): ");
			String env_pass = scan.nextLine().trim();
			System.out.print("\nEnter port number\n(used for SSH connection, leave blank for local dev environment): ");
			String env_port = scan.nextLine().trim();
			System.out.print("\nEnter the database name: ");
			String env_db = scan.nextLine().trim();
			System.out.print("\nEnter the database user name: ");
			String env_dbuser = scan.nextLine().trim();
			System.out.print("\nenter the database user password: ");
			String env_dbpass = scan.nextLine().trim();
			System.out.print("\nEnter the database host (e.g. localhost): ");
			String env_dbhost = scan.nextLine().trim();
			System.out.print("\nEnter the SQL port (e.g. 3306): ");
			String env_dbport = scan.nextLine().trim();
			if (config.containsKey(env)) {
				System.out.println("Envirment already exist, would you like to update it, or change the name?");
				System.out.print("[Enter \"R|replace\" or \"N|rename\"");
				String updateReplace = scan.nextLine().trim();
				if (updateReplace.equalsIgnoreCase("r") || updateReplace.equalsIgnoreCase("replace")) {
					config.put(env, new EEconfig(env, env_host, env_dir, env_user, env_pass, env_port, env_db,
							env_dbuser, env_dbpass, env_dbhost, env_dbport));
				} else {
					System.out.print("Please enter a new name for the environment: ");
					env = scan.nextLine().trim();
					config.put(env, new EEconfig(env, env_host, env_dir, env_user, env_pass, env_port, env_db,
							env_dbuser, env_dbpass, env_dbhost, env_dbport));
				}
			} else {
				config.put(env, new EEconfig(env, env_host, env_dir, env_user, env_pass, env_port, env_db, env_dbuser,
						env_dbpass, env_dbhost, env_dbport));
			}
			outFile.write(config.get(env).toString());
			System.out.println("\nSuccessfully added environemnt \"" + env + "\"");
			System.out.print("Would you like to add more? [Enter \"Y|yes\" or \"N|no\"] ");
			String keepAdding = scan.nextLine().trim();
			if (keepAdding.equalsIgnoreCase("no") || keepAdding.equalsIgnoreCase("n")) {
				done = true;
			}
		}
		outFile.flush();
		outFile.close();
		scan.close();
	}

	/*
	 * Read the configuration file Config file
	 * 
	 * format: env=ENVIRONMENT env_dir=DIRECTORY env_user=USER env_pass=PASS
	 * env_db=eemove_staging_db env_dbuser=USER env_dbpass=PASS env_dbhost=HOST
	 */
	private void readConfigFile() throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
			String line;
			String environment = "", host = "", directory = "", user = "", pass = "", port = "22", db = "", dbUser = "",
					dbPass = "", dbHost = "", dbPort = "3306";
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#") || line.equals(""))
					continue;
				if (line.startsWith("ee_system=")) {
					eeSystem = line.substring(10);
				}
				if (line.startsWith("ee_app=")) {
					eeApp = line.substring(7);
				}
				if (line.startsWith("up_dir=")) {
					uploadDir = line.substring(7);
				}
				if (line.startsWith("above_root=")) {
					if (line.substring(11).equalsIgnoreCase("true")) {
						eeAboveRoot = true;
					} else {
						eeAboveRoot = false;
					}
				}
				if (line.startsWith("env=")) {
					environment = line.substring(4).trim();
				}
				if (line.startsWith("env_host=")) {
					host = line.substring(9).trim();
				}
				if (line.startsWith("env_dir=")) {
					directory = line.substring(8).trim();
				}
				if (line.startsWith("env_user=")) {
					user = line.substring(9).trim();
				}
				if (line.startsWith("env_port=")) {
					port = line.substring(9).trim();
				}
				if (line.startsWith("env_pass=")) {
					pass = line.substring(9).trim();
				}
				if (line.startsWith("env_db=")) {
					db = line.substring(7).trim();
				}
				if (line.startsWith("env_dbuser=")) {
					dbUser = line.substring(11).trim();
				}
				if (line.startsWith("env_dbpass=")) {
					dbPass = line.substring(11).trim();
				}
				if (line.startsWith("env_dbhost=")) {
					dbHost = line.substring(11).trim();
				}
				if (line.startsWith("env_dbport=")) {
					dbPort = line.substring(11).trim();
					// if here, have reached the end, time to build out objects
					if (environment.equals("") || db.equals("") || dbUser.equals("") || dbPass.equals("")
							|| dbHost.equals("")) {
						System.out.println(
								"There was an error reading in your config file, please check for errors and try again. Else, delete the file and re-run the configuration process.");
					} else {
						// Add each config as we go
						if (config.containsKey(environment)) {
							System.out.println("Found duplicate environment in your config file \"" + environment
									+ "\", ignoring entry.");
						} else {
							config.put(environment, new EEconfig(environment, host, directory, user, pass, port, db,
									dbUser, dbPass, dbHost, dbPort));
							// Debugging, make sure things are where they're
							// supposed to be
							// System.out.println(config.get(environment).toString());
						}
						// Clear out the variables just to be safe
						environment = "";
						directory = "";
						user = "";
						pass = "";
						port = "22";
						db = "";
						dbUser = "";
						dbPass = "";
						dbHost = "";
						dbPort = "3306";
					}
				}

			}
			// All done reading config!
			// System.out.println("System folder: " + eeSystem + "\nSystem above
			// root? " + eeAboveRoot);
		}
	}
}
