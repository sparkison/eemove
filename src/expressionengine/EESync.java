package expressionengine;

import java.util.HashMap;
import java.util.Scanner;

import com.google.common.base.Strings;

import db.DBPushPull;
import helpers.Config;
import helpers.ConfigReader;
import rsync.PushPull;
import util.Extras;

public class EESync {

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
	public EESync(String[] arguments, HashMap<String, Config> config, ConfigReader cr, String cmsApp, String cmsSystem, String uploadDir, boolean appAboveRoot) {

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
				new EEPermissionsFixer(cr, config.get(parts[1]));
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
				} else if (directory.equalsIgnoreCase("addons") || directory.equalsIgnoreCase("-a")) {
					// Push/Pull plugin directories to environment
					syncAddons(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				}else if (directory.equalsIgnoreCase("update")) {
					// Push/Pull update related directories to environment
					syncUpdate(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("templates") || directory.equalsIgnoreCase("-t")) {
					// Push/Pull theme directory to environment
					syncTemplates(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("uploads") || directory.equalsIgnoreCase("-u")) {
					// Push/Pull upload directories to environment
					syncUploads(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("ee")) {
					// Push/Pull ExpressionEngine core directory to environment
					syncEE(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("public")) {
					// Push/Pull "app" directory to environment
					syncPublic(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
				} else if (directory.equalsIgnoreCase("custom") || directory.equalsIgnoreCase("-c")) {
					// Push/Pull user specified folder/file
					syncCustom(pushPull, appSrc, appDest, sysSrc, sysDest, type, isDryRun, thisConfig);
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
	private void syncEE(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig) {
		String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + pushPull + Extras.ANSI_RESET + " EE core ", 80, '▬');
		System.out.println(consolMsg);
		sysSrc += "/";
		sysDest += "/";
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
	private void syncAddons(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig) {
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
	}

	/**
	 * Sync the system update related directories/files
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
	private void syncUpdate(String pushPull, String appSrc, String appDest, String sysSrc, String sysDest, String type, boolean isDryRun, Config thisConfig) {
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
