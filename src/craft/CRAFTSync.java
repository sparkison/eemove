package craft;

import java.util.HashMap;
import java.util.Scanner;

import com.google.common.base.Strings;

import db.DBPushPull;
import helpers.Config;
import helpers.ConfigReader;
import rsync.PushPull;
import util.Extras;

public class CRAFTSync {

	// Class variables
	private HashMap<String, Config> config;

	// Folder structure configuration, set some defaults just in case
	private String uploadDir;
	private ConfigReader cr;

	/**
	 * Constructor
	 * 
	 * @param arguments
	 * @param config
	 * @param cr
	 * @param cmsApp
	 * @param cmsSystem
	 * @param uploadDir
	 * @param appAboveRoot
	 */
	public CRAFTSync(String[] arguments, HashMap<String, Config> config, ConfigReader cr, String cmsApp, String cmsSystem, String uploadDir, boolean appAboveRoot) {

		this.config = config;
		this.uploadDir = uploadDir;
		this.cr = cr;

		String parts[] = arguments;
		String pushPull = "";
		String runType = "";
		String environment = "";
		String directory = "";

		// Determine what the command is
		if (parts.length == 2 && parts[0].equalsIgnoreCase("fixperms")) {
			if (config.get(parts[1]) == null) {
				System.out.println("Unable to find environment entered in config file, please try again.");
			} else {
				new CRAFTPermissionsFixer(cr, config.get(parts[1]));
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

				/*
				 * Determine what action user is taking
				 */
				if (directory.equalsIgnoreCase("all")) {
					// Push/Pull all contents of app and system
					syncAll(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("plugins") || directory.equalsIgnoreCase("-p")) {
					// Push/Pull plugin directories to environment
					syncPlugins(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("templates") || directory.equalsIgnoreCase("-t")) {
					// Push/Pull theme directory to environment
					syncTemplates(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("uploads") || directory.equalsIgnoreCase("-u")) {
					// Push/Pull upload directories to environment
					syncUploads(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("craft")) {
					// Push/Pull Craft core directory to environment
					syncCraft(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("public")) {
					// Push/Pull "app" directory to environment
					syncPublic(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("custom") || directory.equalsIgnoreCase("-c")) {
					// Push/Pull user specified folder/file
					syncCustom(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("config")) {
					// Push/Pull Craft config folder/file
					syncConfig(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("database") || directory.equalsIgnoreCase("-d")) {
					// Database push/pull doesn't support dry-run, tell
					syncDatabase(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);				
				} else {
					System.out.println("The command you entered is invalid, please try again.");
				}

			}
		}

	}

	/**
	 * Sync it all! ...except for the database
	 * 
	 * @param pushPull
	 * @param appSrc
	 * @param appDest
	 * @param sysSrc
	 * @param sysDest
	 * @param type
	 * @param isDryRun
	 * @param thisConfig
	 */
	private void syncAll(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig) {
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
	}

	/**
	 * Sync the main "public" directory
	 * 
	 * @param pushPull
	 * @param appSrc
	 * @param appDest
	 * @param sysSrc
	 * @param sysDest
	 * @param type
	 * @param isDryRun
	 * @param thisConfig
	 */
	private void syncPublic(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig) {
		String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Public ", 80, '▬');
		System.out.println(consolMsg);
		appSrc += "/";
		appDest += "/";
		new PushPull(appSrc, appDest, type, isDryRun, thisConfig, cr);
	}

	/**
	 * Sync the main "app" directory
	 * 
	 * @param pushPull
	 * @param appSrc
	 * @param appDest
	 * @param sysSrc
	 * @param sysDest
	 * @param type
	 * @param isDryRun
	 * @param thisConfig
	 */
	private void syncCraft(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig) {
		String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Craft core ", 80, '▬');
		System.out.println(consolMsg);
		sysSrc += "/";
		sysDest += "/";
		new PushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
	}
	
	/**
	 * Sync the Craft config folder
	 * 
	 * @param pushPull
	 * @param appSrc
	 * @param appDest
	 * @param sysSrc
	 * @param sysDest
	 * @param type
	 * @param isDryRun
	 * @param thisConfig
	 */
	private void syncConfig(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig){
		String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Craft config ", 80, '▬');
		System.out.println(consolMsg);
		sysSrc += Extras.CRAFT_CONFIG;
		sysDest += Extras.CRAFT_CONFIG;
		new PushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
	}

	/**
	 * Sync the addons
	 * 
	 * @param pushPull
	 * @param appSrc
	 * @param appDest
	 * @param sysSrc
	 * @param sysDest
	 * @param type
	 * @param isDryRun
	 * @param thisConfig
	 */
	private void syncPlugins(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig) {
		String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Plugins ", 80, '▬');
		System.out.println(consolMsg);
		sysSrc += Extras.CRAFT_PLUGINS;
		sysDest += Extras.CRAFT_PLUGINS;
		new PushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
	}

	/**
	 * Sync the uploads directory
	 * 
	 * @param pushPull
	 * @param appSrc
	 * @param appDest
	 * @param sysSrc
	 * @param sysDest
	 * @param type
	 * @param isDryRun
	 * @param thisConfig
	 */
	private void syncUploads(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig) {
		String consolMsg = "";
		if (!uploadDir.equals("")) {
			consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Custom Uploads Directory ", 80, '▬');
			System.out.println(consolMsg);
			String customUploadSrc = appSrc + "/" + uploadDir + "/";
			String customUploadDest = appDest + "/" + uploadDir + "/";
			new PushPull(customUploadSrc, customUploadDest, type, isDryRun, thisConfig, cr);
		} else {
			consolMsg = Extras.ANSI_YELLOW + "Nothing to do. No custom upload directories defined." + Extras.ANSI_RESET;
			System.out.println(consolMsg);
		}
	}

	/**
	 * Sync the templates directory
	 * 
	 * @param pushPull
	 * @param appSrc
	 * @param appDest
	 * @param sysSrc
	 * @param sysDest
	 * @param type
	 * @param isDryRun
	 * @param thisConfig
	 */
	private void syncTemplates(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig) {
		String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Templates ", 80, '▬');
		System.out.println(consolMsg);
		sysSrc += Extras.CRAFT_TEMPLATES;
		sysDest += Extras.CRAFT_TEMPLATES;
		new PushPull(sysSrc, sysDest, type, isDryRun, thisConfig, cr);
	}

	/**
	 * Sync the database
	 * 
	 * @param pushPull
	 * @param appSrc
	 * @param appDest
	 * @param sysSrc
	 * @param sysDest
	 * @param type
	 * @param isDryRun
	 * @param thisConfig
	 */
	private void syncDatabase(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig) {
		if (isDryRun) {
			System.out.println(Extras.ANSI_RED
					+ "Database push/pull does not support \"dry\" runs, please use the \"-l\" flag instead."
					+ Extras.ANSI_RESET);
		} else {
			if (config.get("local") == null) {
				System.out.println(Extras.ANSI_RED
						+ "You do not have an environment for \"local\", please add one to your \"eemove.config\" file and try again."
						+ Extras.ANSI_RESET);
			} else {
				String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " Database ", 80, '▬');
				System.out.println(consolMsg);
				new DBPushPull(thisConfig, config.get("local"), type, cr);
			}
		}
	}

	/**
	 * Sync custom, user specified, source/destination directory/file
	 * 
	 * @param pushPull
	 * @param appSrc
	 * @param appDest
	 * @param sysSrc
	 * @param sysDest
	 * @param type
	 * @param isDryRun
	 * @param thisConfig
	 */
	private void syncCustom(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig) {

		Scanner scan = new Scanner(System.in);

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

		scan.close();
	}

}
