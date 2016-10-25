package helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import ch.ethz.ssh2.StreamGobbler;
import util.Extras;

public class CommandExecuter {

	private final Config config;
	private final boolean indicator;

	public CommandExecuter(Config config, boolean indicator) {
		this.config = config;
		this.indicator = indicator;		
	}

	/*
	 * Create temp file, execute command, delete and return delete status
	 */
	public boolean executeCommand(String command) throws IOException {
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
		System.out.println(Extras.ANSI_GREEN + "Executing command: " + command + Extras.ANSI_RESET);
		
		bashFile.write("#!/bin/bash");
		bashFile.write("\n");
		bashFile.write("export SSHPASS='"+config.getSshPass()+"'");
		bashFile.write("\n\n");
		bashFile.write(escapeSepcialChars(command));
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
				if(indicator) {
					// Print out loading animation
					if(lineCount % 5 == 0) System.out.print("*** -- ***\r");
					else if(lineCount % 5 == 1) System.out.print("*** \\  ***\r");
					else if(lineCount % 5 == 2) System.out.print("*** |  ***\r");
					else if(lineCount % 5 == 3) System.out.print("*** /  ***\r");
					else if(lineCount % 5 == 4) System.out.print("*** -- ***\r");
					lineCount++;
				} else {
					System.out.println("> " + val);
				}
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
	
	/*
	 * Escape characters that cause problems with bash or sql
	 */
	private String escapeSepcialChars(String cmd) {
		return cmd
				.replace("$","\\$")
				.replace(";","\\;")
				.replace(")","\\)")
				.replace("(","\\(")
				.replace("\"","\\\"");
	}

}
