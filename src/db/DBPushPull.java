/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 * 
 */

package db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Strings;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import helpers.ConfigReader;
import helpers.EEconfig;
import util.EEExtras;

public class DBPushPull implements EEExtras {

	// Class variables
	private EEconfig destConfig, localConfig;
	private Connection connection;
	private String timestamp;
	private ConfigReader cr;
	private String type;

	// Make sure backup folder exists, if not create it
	@SuppressWarnings("unused")
	private boolean dbBackupFolder = new File(EEExtras.CWD + "/db_backups").mkdir();

	/*
	 * Constructor
	 */
	public DBPushPull(EEconfig destConfig, EEconfig localConfig, String type, ConfigReader cr) {
		// Set the configurations
		this.destConfig = destConfig;
		this.localConfig = localConfig;
		this.type = type;
		// Grab the config file info
		this.cr = cr;
		// Grab a timestamp
		timestamp = new SimpleDateFormat("MM.dd.yyyy_HH.mm.ss").format(new Date());
		// Initiate a null connection
		connection = null;
		try {
			// Try to connect to server
			//connection = connectTo();
			/*
			 * If connection successful, make backups of local and remote
			 * databases and save them to the projects db_backup folder
			 */
			File remoteBackup = makeRemoteDbBackup();
			File localBackup = localDbBackup();

			/*
			 * See what we're doing, pushing or pulling the database
			 */
			if (this.type.equalsIgnoreCase("push")) {
				/*
				 * Push local dump to remote, import the dump, then remove the
				 * dump file from the server
				 */
				importLocalDbBackup(localBackup);
				/*
				 * Remove the local dump as we're done with it and it contains updated database info only relevant to remote
				 */
				System.out.println(EEExtras.ANSI_GREEN + "\tlocal | delete file: " + EEExtras.ANSI_RESET + localBackup);
				localBackup.delete();
			} else {
				/*
				 * Import the remote dump, simple!
				 */
				importRemoteDbBackup(remoteBackup);
				/*
				 * Remove the remote dump as we're done with it and it contains updated database info only relevant to local
				 */
				System.out.println(EEExtras.ANSI_GREEN + "\tlocal | delete file: " + EEExtras.ANSI_RESET + remoteBackup);
				remoteBackup.delete();
			}

			/*
			 * Database push/pull complete!
			 */
			String consolMsg = Strings.padEnd(
					"▬▬ ✓ " + EEExtras.ANSI_CYAN + "Database Transfer complete! " + EEExtras.ANSI_RESET, 80, '▬');
			System.out.println(consolMsg);

		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.close();
			}
		}

	}

	/*
	 * Make a dump of the remote database. To do this we're executing the
	 * 'mysqldump' command on the remote server and grabbing its output and
	 * writing it to local file. This means we don't need to write the output to
	 * a file and download it and remove it later, much cleaner
	 * 
	 * Need to wrap password in single quotes ['], and escape any single quotes within password [Str.replace("'", "\'")]
	 */
	private File makeRemoteDbBackup() throws IOException {
		String fileName = destConfig.getEnvironment() + "_db_" + timestamp + ".sql";
		
		String command = "mysqldump --opt --quick --add-drop-table --skip-comments --no-create-db --user=" + destConfig.getDbUser()
				+ " --password=\"" + destConfig.getDbPass().replace("'", "\'") + "\" --port=" + destConfig.getDbPort() + " --databases "
				+ destConfig.getDatabase() + " --result-file=/tmp/" + fileName;

		System.out.println(EEExtras.ANSI_PURPLE + "\tremote | " + EEExtras.ANSI_RESET + command);

		String ssh = "";
		if( cr.useKeyAuth ) {
			ssh = "ssh -i " + cr.getKeyfile() + " " + destConfig.getSshUser() + "@" + destConfig.getHost();
		} else {
			ssh = EEExtras.SSHPASSPATH + "sshpass -e ssh " + destConfig.getSshUser() + "@" + destConfig.getHost();
		}
				
		String commandWithAuth = ssh + " '" + command + "'";
		
		if( ! executeCommand( commandWithAuth ) ) {
			String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_RED + "Error: unable to execute MYSQL command " + EEExtras.ANSI_RESET, 80, '▬');
			System.out.println(consolMsg);
			System.out.println(EEExtras.ANSI_YELLOW + "Please double check your credentials and eemove config file and try again" + EEExtras.ANSI_RESET);
			System.exit(-1);
		}
		
		// Get the backup file, adapt it, and return it
		File backupFile = importRemoteDbBackup(fileName);
		return backupFile;
	}

	/*
	 * Make local backup of database To do this we're going to exec a process
	 * and grab the ouput of the mysql dump from the local machine and save to
	 * file in db_backups folder
	 */
	private File localDbBackup() throws IOException {
		File localDbBackup = new File(
				EEExtras.CWD + "/db_backups/" + localConfig.getEnvironment() + "_db_" + timestamp + ".sql");
		OutputStream out = new FileOutputStream(localDbBackup);
		String[] command = { "/bin/sh", "-c",
				EEExtras.PATH + "/mysqldump --opt --add-drop-table --skip-comments --no-create-db --user=" + localConfig.getDbUser()
						+ " --password=" + localConfig.getDbPass() + " --port=" + localConfig.getDbPort()
						+ " --databases " + localConfig.getDatabase() };

		System.out.println(EEExtras.ANSI_GREEN + "\tlocal | " + EEExtras.ANSI_RESET + Arrays.toString(command));

		Runtime rt = Runtime.getRuntime();
		Process proc = null;
		try {
			// Using underlying mysqldump for dba backup
			proc = rt.exec(command);
			// Use StreamGobbler to for err/stdout to prevent blocking
			InputStream stdout = new StreamGobbler(proc.getInputStream());
			InputStream stderr = new StreamGobbler(proc.getErrorStream());
			OutputStream stdin = proc.getOutputStream();
			InputStreamReader isrErr = new InputStreamReader(stderr);
			BufferedReader brErr = new BufferedReader(isrErr);

			// Print output to stdout
			String line = null;
			InputStreamReader isrStd = new InputStreamReader(stdout);
			BufferedReader brStd = new BufferedReader(isrStd);
			while ((line = brStd.readLine()) != null) {
				/*
				 * Don't include the USE `database` name since they will likely
				 * differ from local to remote sources, also ignore comments
				 */
				if (!(line.startsWith("USE") || line.startsWith("--"))) {
					// Write out the bytes to the file
					out.write(line.getBytes());
					out.write("\n".getBytes());
				}
					
			}

			// Print errors stdout so user knows what went wrong
			while ((line = brErr.readLine()) != null) {
				System.err.println(EEExtras.ANSI_RED + ">>[Error]: " + line + EEExtras.ANSI_RESET);
			}

			int exitVal = proc.waitFor();

			if (exitVal != 0) {
				System.out.println(EEExtras.ANSI_YELLOW
						+ ">>[Warning]: There might have been a problem executing the command. Please double check everything worked as expected."
						+ EEExtras.ANSI_RESET);
			}

			// Clean up
			brStd.close();
			brErr.close();
			stdin.close();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			out.flush();
			out.close();
		}

		return localDbBackup;

	}

	/*
	 * Import the remote dump into the local database
	 */
	private void importRemoteDbBackup(File file) {
		String[] command = { "/bin/sh", "-c",
				EEExtras.PATH + "/mysql --user=" + localConfig.getDbUser() + " --password="
						+ localConfig.getDbPass() + " --port=" + localConfig.getDbPort() + " --database="
						+ localConfig.getDatabase() + " < " + file.getAbsolutePath() };

		System.out.println(EEExtras.ANSI_GREEN + "\tlocal | " + EEExtras.ANSI_RESET + Arrays.toString(command));

		Runtime rt = Runtime.getRuntime();
		Process proc = null;

		try {
			// Using underlying mysqldump for dba backup
			proc = rt.exec(command);
			// Use StreamGobbler to for err/stdout to prevent blocking
			InputStream stdout = new StreamGobbler(proc.getInputStream());
			InputStream stderr = new StreamGobbler(proc.getErrorStream());
			OutputStream stdin = proc.getOutputStream();
			InputStreamReader isrErr = new InputStreamReader(stderr);
			BufferedReader brErr = new BufferedReader(isrErr);

			// Print status to stdout
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

			// Print errors stdout so user knows what went wrong
			while ((val = brErr.readLine()) != null) {
				System.err.println(EEExtras.ANSI_RED + ">> [Error]: " + val + EEExtras.ANSI_RESET);
			}

			int exitVal = proc.waitFor();
			
			// Clean up the loading animation line
			System.out.print("          \r");

			if (exitVal != 0) {
				System.out.println(EEExtras.ANSI_YELLOW
						+ ">>[Warning]: There might have been a problem executing the command. Please double check everything worked as expected."
						+ EEExtras.ANSI_RESET);
			}

			// Clean up
			brStd.close();
			brErr.close();
			stdin.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Pull the remote database from the remote server
	 * Once downloaded, remove it from the server
	 */
	private File importRemoteDbBackup(String remoteFile) throws IOException {
		String ssh = "";
		if( !cr.useKeyAuth ) {
			ssh = EEExtras.SSHPASSPATH + "sshpass -e ";
		}
		String command = ssh + "scp " + destConfig.getSshUser() + "@" + destConfig.getHost() + ":/tmp/" + remoteFile + " " + EEExtras.CWD + "/db_backups/";
		
		if( ! executeCommand( command ) ) {
			String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_RED + "Error: unable to execute SCP command " + EEExtras.ANSI_RESET, 80, '▬');
			System.out.println(consolMsg);
			System.out.println(EEExtras.ANSI_YELLOW + "Please double check your credentials and eemove config file and try again" + EEExtras.ANSI_RESET);
			System.exit(-1);
		}
		// Pass things off to the next method to import the database and delete
		// the sql dump file
		System.out.println(EEExtras.ANSI_PURPLE + "\tremote | " + EEExtras.ANSI_RESET + command);
		// Remove the file from the remote server
		removeTempFile("/tmp/" + remoteFile);
		// The name of the remote dump and the lines to remove
		String localBackupOfRemoteDump = EEExtras.CWD + "/db_backups/" + remoteFile;
		String[] removeLines = {"--", "USE"};
		// Adapt the file for local import
		adaptDump(localBackupOfRemoteDump, removeLines);
		// Grab the adapted dump file
		File remoteDbDump = new File(localBackupOfRemoteDump);
		// And return it
		return remoteDbDump;
	}


	/*
	 * Push the local database up to the server
	 */
	private void importLocalDbBackup(File file) throws IOException {
		String ssh = "";
		if( !cr.useKeyAuth ) {
			ssh = EEExtras.SSHPASSPATH + "sshpass -e ";
		}
		String command = ssh + "scp " + file.getAbsolutePath() + " " + destConfig.getSshUser() + "@" + destConfig.getHost() + ":/tmp/";
		
		if( ! executeCommand( command ) ) {
			String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_RED + "Error: unable to execute SCP command " + EEExtras.ANSI_RESET, 80, '▬');
			System.out.println(consolMsg);
			System.out.println(EEExtras.ANSI_YELLOW + "Please double check your credentials and eemove config file and try again" + EEExtras.ANSI_RESET);
			System.exit(-1);
		}
		
		// Pass things off to the next method to import the database and delete
		// the sql dump file
		System.out.println(EEExtras.ANSI_PURPLE + "\tremote | " + EEExtras.ANSI_RESET + command);
		importLocalDbBackupToRemote(file);
	}

	/*
	 * Import the local db to the remote server, then remove it
	 */
	private void importLocalDbBackupToRemote(File file) {
		String command = "mysql --verbose --user=" + destConfig.getDbUser() + " --password=\"" + destConfig.getDbPass().replace("'", "\'")
				+ "\" --port=" + destConfig.getDbPort() + " --database=" + destConfig.getDatabase() + " < " + "/tmp/"
				+ file.getName();
		
		String ssh = "";
		if( cr.useKeyAuth ) {
			ssh = "ssh -i " + cr.getKeyfile() + " " + destConfig.getSshUser() + "@" + destConfig.getHost();
		} else {
			ssh = EEExtras.SSHPASSPATH + "sshpass -e ssh " + destConfig.getSshUser() + "@" + destConfig.getHost();
		}
		
		System.out.println(EEExtras.ANSI_PURPLE + "\tremote | " + EEExtras.ANSI_RESET + command);
		
		String commandWithAuth = ssh + " '" + command + "'";
		
		try {
			if( ! executeCommand( commandWithAuth ) ) {
				String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_RED + "Error: unable to execute MYSQL command " + EEExtras.ANSI_RESET, 80, '▬');
				System.out.println(consolMsg);
				System.out.println(EEExtras.ANSI_YELLOW + "Please double check your credentials and eemove config file and try again" + EEExtras.ANSI_RESET);
				System.exit(-1);
			}
		} catch (IOException e1) {
			System.out.println("Error removing temporary dump from server: " + e1.getMessage());
			e1.printStackTrace();
		}
		
		// Remove the file after importing it
		try {
			removeTempFile("/tmp/" + file.getName());
		} catch (IOException e2) {
			System.out.println("Error removing temporary dump from server: " + e2.getMessage());
			e2.printStackTrace();
		}
	}

	/*
	 * Remove the dump from the server
	 */
	private void removeTempFile(String filename) throws IOException {
		String command = "rm " + filename;
		
		String ssh = "";
		if( cr.useKeyAuth ) {
			ssh = "ssh -i " + cr.getKeyfile() + " " + destConfig.getSshUser() + "@" + destConfig.getHost();
		} else {
			ssh = EEExtras.SSHPASSPATH + "sshpass -e ssh " + destConfig.getSshUser() + "@" + destConfig.getHost();
		}
		
		System.out.println(EEExtras.ANSI_PURPLE + "\tremote | " + EEExtras.ANSI_RESET + command);
		
		String commandWithAuth = ssh + " '" + command + "'";
		
		if( ! executeCommand( commandWithAuth ) ) {
			String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_RED + "Error: unable to execute RM command " + EEExtras.ANSI_RESET, 80, '▬');
			System.out.println(consolMsg);
			System.out.println(EEExtras.ANSI_YELLOW + "Please double check your credentials and eemove config file and try again" + EEExtras.ANSI_RESET);
			System.exit(-1);
		}

	}
	
	/*
	 * Adapt dump:
	 * Currently used to remove comments and the "USE"
	 * statement from DB dump (only needed for remote dump)
	 */
	public void adaptDump(String file, String[] linesToRemove) {
		try {

			File inFile = new File(file);

			if (!inFile.isFile()) {
				System.out.println("Unable to locate file: " + file);
				return;
			}

			// Construct the new file that will later be renamed to the original
			// filename.
			File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

			BufferedReader br = new BufferedReader(new FileReader(inFile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));

			String line = null;
			boolean match = false;

			// Read from the original file and write to the new
			// unless content matches data to be removed.
			while ((line = br.readLine()) != null) {
				for (String remove : linesToRemove) {
					if (line.startsWith(remove)) {
						match = true;
						break;
					}
					match = false;
				}
				if (!match)
					bw.write(line + System.getProperty("line.separator"));
			}
			bw.flush();
			bw.close();
			br.close();

			// Delete the original file
			if (!inFile.delete()) {
				System.out.println("Could not delete file");
				return;
			}
			// Rename the new file to the filename the original file had.
			if (!tempFile.renameTo(inFile))
				System.out.println("Could not rename file");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
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
		bashFile.write("export SSHPASS='"+destConfig.getSshPass()+"'");
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
					System.err.println(EEExtras.ANSI_RED + ">>[Error]: " + val + EEExtras.ANSI_RESET);
			}
			int exitVal = proc.waitFor();

			if (exitVal != 0) {
				System.out.println(EEExtras.ANSI_YELLOW
						+ ">>[Warning]: There might have been a problem executing the command. Please double check everything worked as expected."
						+ EEExtras.ANSI_RESET);
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
