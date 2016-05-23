package helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import util.EEExtras;

public class CommandExecuter {

	private final EEconfig config;
	
	public CommandExecuter(EEconfig config) {
		this.config = config;
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
			
			int exitVal = proc.waitFor();

			if (exitVal != 0) {
				System.out.println(EEExtras.ANSI_YELLOW
						+ ">>[Warning]: There might have been a problem executing the command. Please double check everything worked as expected."
						+ EEExtras.ANSI_RESET);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		// Remove the file, print completion message
		boolean deleteStatus = tempBashCmd.delete();
		return( deleteStatus );
	}
	
}
