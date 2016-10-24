/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 * 
 */

package craft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.google.common.base.Strings;

import ch.ethz.ssh2.StreamGobbler;
import helpers.Config;
import helpers.ConfigReader;
import util.Extras;

public class CRAFTPermissionsFixer {

	private ConfigReader cr;
	private Config config;

	// Constructor
	public CRAFTPermissionsFixer(ConfigReader cr, Config config) {
		this.cr = cr;
		this.config = config;
		try {
			this.fixPermissions();
		} catch (IOException e) {
			System.out.println("Error setting permissions: " + e.getMessage()); 
		}
	}

	/*
	 * If uploading files to server, ensure proper permissions set
	 */
	public void fixPermissions() throws IOException {
		// Notify user of our intent
		String consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_CYAN + "Updating permissions " + Extras.ANSI_RESET, 80, '▬');
		System.out.println(consolMsg);
		// Grab the system and app directories
		String sysDest = "";
		String appDest = config.getDirectory();
		String envDirParts[] = config.getDirectory().split("/");
		// Determine if system is above root or not
		if (cr.isAboveRoot() == true) {
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
		if(!cr.getUpDir().equals("")) 
			uploadDirPerms = " && chmod -R 777 " + appDest + "/" + cr.getUpDir();

		// Build the command
		String command = "find " + appDest + " -type f -exec chmod 644 {} \\;"
				+ " && find " + appDest + " -type d -exec chmod 755 {} \\;";

		// If system in separate directory from main app, fix it permissions as well
		if(!sysDest.equals(appDest)) {
			command += " && find " + sysDest + " -type f -exec chmod 644 {} \\;"
					+ " && find " + sysDest + " -type d -exec chmod 755 {} \\;";
		}

		if(cr.getCmsVer() == 3) {
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
		} else if(cr.getCmsVer() == 2) {
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
			System.out.println("ExpressionEngine version " + cr.getCmsVer() + " not supported, please update config and try again.");
			System.exit(1);
		}

		// Create the session and execute command on desired environment
		String ssh = "";
		if( cr.isUseKeyAuth() ) {
			ssh = "ssh -i " + cr.getKeyfile() + " " + config.getSshUser() + "@" + config.getHost();
		} else {
			ssh = cr.getSshPassPath() + "sshpass -e ssh " + config.getSshUser() + "@" + config.getHost();
		}

		String commandWithAuth = ssh + " '" + command + "'";

		// Show user the command we're sending
		System.out.println(Extras.ANSI_PURPLE + "\tremote | " + Extras.ANSI_RESET + commandWithAuth);

		if( ! executeCommand( commandWithAuth ) ) {
			consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_RED + "Error: unable to execute CHMOD command " + Extras.ANSI_RESET, 80, '▬');
			System.out.println(consolMsg);
			System.out.println(Extras.ANSI_YELLOW + "Please double check your credentials and eemove config file and try again" + Extras.ANSI_RESET);
			System.exit(-1);
		}

		consolMsg = Strings.padEnd(
				"▬▬ ✓ " + Extras.ANSI_CYAN + "Complete! " + Extras.ANSI_RESET, 80, '▬');
		System.out.println(consolMsg);
	}

	/*
	 * Create temp file, execute command, delete and return delete status
	 */
	private boolean executeCommand(String command) throws IOException {
		/*
		 * Create temp file so we can use exec command Not ideal, but best I
		 * could come up with for now Uses underlying system commands, not
		 * relying on external libraries this way. File will be deleted at end
		 * of execution. In the case of program error, will also be removed upon
		 * JVM termination (if it still persists).
		 */
		File tempBashCmd = File.createTempFile("tmp", ".sh", new File("/tmp"));
		FileWriter bashFile = new FileWriter(tempBashCmd);
		tempBashCmd.setExecutable(true);
		tempBashCmd.deleteOnExit();

		/*
		 * Write out expect command to tmp shell script (if using password authentication
		 */
		bashFile.write("#!/bin/bash");
		bashFile.write("\n");
		bashFile.write("export SSHPASS='"+config.getSshPass()+"'");
		bashFile.write("\n\n");
		bashFile.write(command);
		bashFile.write("\n");
		bashFile.close();

		Runtime rt = Runtime.getRuntime();
		Process proc = null;
		try {
			// Using underlying 'sh' command to pass password for rsync
			proc = rt.exec("sh " + tempBashCmd.getAbsolutePath());

			// Use StreamGobbler to for err/stdout to prevent blocking
			InputStream stdout = new StreamGobbler(proc.getInputStream());
			InputStream stderr = new StreamGobbler(proc.getErrorStream());
			OutputStream stdin = proc.getOutputStream();
			InputStreamReader isrErr = new InputStreamReader(stderr);
			BufferedReader brErr = new BufferedReader(isrErr);

			// Print output to stdout
			String val = null;
			InputStreamReader isrStd = new InputStreamReader(stdout);
			BufferedReader brStd = new BufferedReader(isrStd);
			int lineCount = 0;
			while ((val = brStd.readLine()) != null) {
				// Print out loading animation
				if(lineCount % 5 == 0) System.out.print("*** -- ***\r");
				else if(lineCount % 5 == 1) System.out.print("*** \\  ***\r");
				else if(lineCount % 5 == 2) System.out.print("*** |  ***\r");
				else if(lineCount % 5 == 3) System.out.print("*** /  ***\r");
				else if(lineCount % 5 == 4) System.out.print("*** -- ***\r");
				lineCount++;
			}
			// Clean up the loading animation line
			System.out.print("          \r");

			// Print errors stdout so user knows what went wrong
			while ((val = brErr.readLine()) != null) {
				if(!val.contains("stdin: is not a tty"))
					System.err.println(Extras.ANSI_RED + ">>[Error]: " + val + Extras.ANSI_RESET);
			}
			int exitVal = proc.waitFor();

			if (exitVal != 0) {
				System.out.println(Extras.ANSI_YELLOW
						+ ">>[Warning]: There might have been a problem executing the command. Please double check everything worked as expected."
						+ Extras.ANSI_RESET);
			}

			// Clean up
			brStd.close();
			brErr.close();
			stdin.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		// Remove the file, print completion message
		boolean deleteStatus = tempBashCmd.delete();
		return( deleteStatus );
	}

}
