/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 * 
 */

package helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import util.Extras;

public class ConfigReader implements Extras {

	private final String CONF;

	public File keyfile;
	public String keyPass;
	public Integer eeVer;
	public String sysDir;
	public String appDir;
	public String upDir;
	public String sshPassPath;
	public String mysqlPath;
	public boolean useKeyAuth;
	public boolean aboveRoot;

	/*
	 * Constructor
	 */
	public ConfigReader(String configFile) {
		// Preset some defaults
		this.sshPassPath = "/usr/local/bin/";
		this.mysqlPath = "/usr/local/bin/";
		// Set the config
		this.CONF = configFile;
	}

	/*
	 * Parse the config file and return a HashMap of the environment variables
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap<String, Config> getConfig() {

		// Create the config file first, if it doesn't exist
		this.confInit();

		// The item we're going to return
		HashMap<String, Config> configVars = new HashMap<String, Config>();

		// Instantiate the Yaml parser
		Yaml yaml = new Yaml();
		String environment = "", host = "", directory = "", user = "", pass = "", port = "22", db = "", dbUser = "",
				dbPass = "", dbHost = "", dbPort = "3306";

		// Grab values form the config file
		try {
			yaml.load(new FileInputStream(new File(CONF)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// Parse the values received
		Map<String, Map<?, ?>> values;
		try {
			values = (Map<String, Map<?, ?>>) yaml.load(new FileInputStream(new File(CONF)));

			for (String key : values.keySet()) {
				Map<?, ?> subValues = values.get(key);

				environment = key;

				if (environment.toString().equals("globals")) {
					for (Object subValueKey : subValues.keySet()) {
						// Get the key
						String keyItem = subValueKey.toString();
						if (keyItem.equalsIgnoreCase("ee_system")) {
							this.sysDir = subValues.get(subValueKey).toString();
						} else if (keyItem.equalsIgnoreCase("ee_version")) {
							this.eeVer = Integer.parseInt(subValues.get(subValueKey).toString());
						} else if (keyItem.equalsIgnoreCase("ee_app")) {
							this.appDir = subValues.get(subValueKey).toString();
						} else if (keyItem.equalsIgnoreCase("upload_dir")) {
							this.upDir = subValues.get(subValueKey).toString();
						} else if (keyItem.equalsIgnoreCase("above_root")) {
							this.aboveRoot = Boolean.valueOf(subValues.get(subValueKey).toString());
						} else if (keyItem.equalsIgnoreCase("authentication")) {
							Map authentication = (LinkedHashMap) subValues.get(subValueKey);
							for (Object authItem : authentication.keySet()) {
								if (authItem.toString().equals("type")) {
									boolean useKey = false;
									if (authentication.get(authItem).toString().equals("key"))
										useKey = true;
									this.useKeyAuth = useKey;
								} else if (authItem.toString().equals("keyfile")) {
									this.keyfile = new File(authentication.get(authItem).toString());
								} else if (authItem.toString().equals("keypass")) {
									this.keyPass = authentication.get(authItem).toString();
								}
							}
						} else if (keyItem.equalsIgnoreCase("executables")) {
							Map executables = (LinkedHashMap) subValues.get(subValueKey);
							for (Object pathItem : executables.keySet()) {
								if (pathItem.toString().equals("sshpass")) {
									this.sshPassPath = executables.get(pathItem).toString();
								} else if (pathItem.toString().equals("mysql")) {
									this.mysqlPath = executables.get(pathItem).toString();
								}
							}
						}
					}
				} else {
					for (Object subValueKey : subValues.keySet()) {
						// Get the key
						String keyItem = subValueKey.toString();
						// Set variables based on the values
						if (keyItem.equalsIgnoreCase("vhost")) {
							host = subValues.get(subValueKey).toString();
						}
						// Grab the ssh info
						else if (keyItem.equalsIgnoreCase("ee_path")) {
							directory = subValues.get(subValueKey).toString();
						}
						// Grab the database info
						else if (keyItem.equalsIgnoreCase("database")) {
							Map database = (LinkedHashMap) subValues.get(subValueKey);
							for (Object dbItem : database.keySet()) {
								if (dbItem.toString().equals("name")) {
									db = database.get(dbItem).toString();
								} else if (dbItem.toString().equals("user")) {
									dbUser = database.get(dbItem).toString();
								} else if (dbItem.toString().equals("password")) {
									dbPass = database.get(dbItem).toString();
								} else if (dbItem.toString().equals("host")) {
									dbHost = database.get(dbItem).toString();
								} else if (dbItem.toString().equals("port")) {
									dbPort = database.get(dbItem).toString();
								}
							}
						} else if (keyItem.equalsIgnoreCase("ssh")) {
							Map ssh = (LinkedHashMap) subValues.get(subValueKey);
							for (Object sshItem : ssh.keySet()) {
								if (sshItem.toString().equals("host")) {
									host = ssh.get(sshItem).toString();
								} else if (sshItem.toString().equals("user")) {
									user = ssh.get(sshItem).toString();
								} else if (sshItem.toString().equals("password")) {
									pass = ssh.get(sshItem).toString();
								} else if (sshItem.toString().equals("port")) {
									port = ssh.get(sshItem).toString();
								}
							}
						}
					}
					/*
					 * We've got all of our config vars, create the EEconfig
					 * object and add to hashmap
					 */
					configVars.put(environment, new Config(environment, host, directory, user, pass, port, db, dbUser,
							dbPass, dbHost, dbPort));

					// Clear out the variables just to be safe
					environment = "";
					directory = "";
					user = "";
					pass = "";
					port = "22"; // Set a default port
					db = "";
					dbUser = "";
					dbPass = "";
					dbHost = "";
					dbPort = "3306"; // Set a default port
				}

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return configVars;

	} // END getConfig

	/*
	 * The init for our primary configuration file
	 */
	public void confInit() {
		// Create an example config file if not exist
		FileWriter outFile;
		File eemoveConfig = new File(CONF);
		if (!eemoveConfig.exists()) {
			System.out.println(Extras.ANSI_YELLOW
					+ "\n It looks like you don't have a config file created yet. Creating one for you now...\n"
					+ Extras.ANSI_RESET);
			try {
				outFile = new FileWriter(eemoveConfig);
				StringBuilder sb = new StringBuilder();
				Formatter formatter = new Formatter(sb);
				String line;
				formatter.format("%s",
						"#############################################################################\n");
				formatter.format("%s", "# eemove configuration file. Auto-generated by eemove\n");
				formatter.format("%s", "# Add as many environments as needed\n");
				formatter.format("%s",
						"# or delete this file and run eemove to intiate the automated config generator\n");
				formatter.format("%s", "# For more information visit: https://github.com/sparkison/eemove\n");
				formatter.format("%s",
						"#############################################################################\n");
				formatter.format("%s", "# Global EE settings\n\n");

				formatter.format("%s", "globals:\n");

				line = "ee_version: \"3\" # ExpressionEngine version, 2 or 3 are currently supported";
				formatter.format("%" + (line.length() + 3) + "s", line + "\n");
				
				line = "ee_system: \"system\"";
				formatter.format("%" + (line.length() + 3) + "s", line + "\n");

				line = "ee_app: \"app\"";
				formatter.format("%" + (line.length() + 3) + "s", line + "\n");

				line = "upload_dir: \"uploads\" # optional, if using custom upload directory/ies";
				formatter.format("%" + (line.length() + 3) + "s", line + "\n");

				line = "above_root: \"true\" # use true or false to signify whether the system folder is above root or not";
				formatter.format("%" + (line.length() + 4) + "s", line + "\n\n");

				line = "executables:";
				formatter.format("%" + (line.length() + 3) + "s", line + "\n");
				
				line = "# sshpass: \"\" # optional, specify path to sshpass, will default to /usr/local/bin/. Type `which sshpass` in terminal to determin where the executable is located";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");
				
				line = "# mysql: \"\" # optiona, base path to mysql and mysqldump, will default to /usr/local/bin/. Type `which mysql` in terminal to determin where the executable is located";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");
			
				line = "authentication:";
				formatter.format("%" + (line.length() + 3) + "s", line + "\n");

				line = "type: \"key\" # use either 'key' for public key authentication or 'password' for password";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "keyfile: \"/user/john/.ssh/id_rsa\" # optional, only needed if using 'key' for type. Use an absolute path here";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "keypass: \"password\" # optional, only needed if using 'key' for type and the key file is password protected";
				formatter.format("%" + (line.length() + 6) + "s", line + "\n\n");

				formatter.format("%s", "# Begin environment specific configuration(s)\n\n");

				formatter.format("%s", "local:\n");
				line = "vhost: \"http://yoursite.dev\"";
				formatter.format("%" + (line.length() + 3) + "s", line + "\n\n");

				line = "database:";
				formatter.format("%" + (line.length() + 3) + "s", line + "\n");

				line = "name: \"database_name\"";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "user: \"root\"";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "password: \"root\"";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "host: \"127.0.0.1\"";
				formatter.format("%" + (line.length() + 6) + "s", line + "\n\n");

				formatter.format("%s", "staging:\n");
				line = "vhost: \"http://example.com\"";
				formatter.format("%" + (line.length() + 3) + "s", line + "\n");

				line = "ee_path: \"/var/www/your_site\" # use an absolute path here";
				formatter.format("%" + (line.length() + 4) + "s", line + "\n\n");

				line = "database:";
				formatter.format("%" + (line.length() + 3) + "s", line + "\n");

				line = "name: \"database_name\"";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "user: \"user\"";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "password: \"password\"";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "host: \"host\"";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "port: 3306 # port is optional, will default to 3306, use to overwrite default";
				formatter.format("%" + (line.length() + 6) + "s", line + "\n\n");

				line = "ssh:";
				formatter.format("%" + (line.length() + 3) + "s", line + "\n");

				line = "host: \"host\"";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "user: \"user\"";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "password: \"password\" # only sent if not using public/private key authentication";
				formatter.format("%" + (line.length() + 5) + "s", line + "\n");

				line = "port: 22 # port is optional, will default to 22, use to overwrite default";
				formatter.format("%" + (line.length() + 6) + "s", line + "\n\n");

				formatter.format("%s", "# production: # multiple environments can be specified\n");
				formatter.format("%s", "#  [...]\n");
				formatter.flush();
				formatter.close();

				outFile.write(sb.toString());
				outFile.flush();
				outFile.close();

				// Bootstrap config file created
				// Prompt user to edit the details and re-run eemove
				System.out.println(Extras.ANSI_YELLOW + "Config file created." + Extras.ANSI_RESET);
				System.out.println(Extras.ANSI_YELLOW
						+ "Please add environments and make adjustments as needed, then run eemove again to get started!\n"
						+ Extras.ANSI_RESET);
				System.out.println(Extras.ANSI_YELLOW + "****************************************************"
						+ Extras.ANSI_RESET);
				// Exit eemove, can't use the auto-generated config as it
				// doesn't contain actual environment variables
				System.exit(0);

			} catch (IOException e) {
				System.out.println(
						Extras.ANSI_RED + "Error wrting config file: " + e.getMessage() + Extras.ANSI_RESET);
				System.exit(0);
			}
			
		}

	}

	/*
	 * Getters
	 */
	public String getSysDir() {
		return sysDir;
	}

	public String getAppDir() {
		return appDir;
	}

	public String getUpDir() {
		return upDir;
	}

	public boolean isAboveRoot() {
		return aboveRoot;
	}

	public File getKeyfile() {
		return keyfile;
	}

	public String getKeyPass() {
		return keyPass;
	}

	public boolean isUseKeyAuth() {
		return useKeyAuth;
	}
	
	public String getSshPassPath() {
		return sshPassPath;
	}

	public String getMysqlPath() {
		return mysqlPath;
	}
	
}
