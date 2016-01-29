/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 * 
 */

package db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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
		timestamp = new SimpleDateFormat("MM.dd.yyyy_dd.HH.mm.ss").format(new Date());
		// Initiate a null connection
		connection = null;
		try {
			// Try to connect to server
			connection = connectTo();
			/*
			 * If connection successful, make backups of local and remote
			 * databases and save them to the projects db_backup folder
			 */
			File remoteBackup = makeRemoteDbBackup(connection);
			File localBackup = localDbBackup();

			/*
			 * See what we're doing, pushing or pulling the database
			 */
			if (type.equalsIgnoreCase("push")) {
				/*
				 * Push local dump to remote, import the dump, then remove the
				 * dump file from the server
				 */
				importLocalDbBackup(connection, localBackup);
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
	 * writing it to local file. This measn we don't need to write the output to
	 * a file and download it and remove it later, much cleaner
	 */
	private File makeRemoteDbBackup(Connection connection) throws IOException {
		File backupFile = new File(
				EEExtras.CWD + "/db_backups/" + destConfig.getEnvironment() + "_db_" + timestamp + ".sql");
		String command = "mysqldump --opt --add-drop-table --no-create-db --verbose --user=" + destConfig.getDbUser()
				+ " --password=" + destConfig.getDbPass() + " --port=" + destConfig.getDbPort() + " --databases "
				+ destConfig.getDatabase();

		System.out.println(EEExtras.ANSI_GREEN + "\tlocal | create file: " + EEExtras.ANSI_RESET + backupFile);
		System.out
				.println(EEExtras.ANSI_PURPLE + "\tremote | pipe output to local file: " + EEExtras.ANSI_RESET + command);

		OutputStream out = new FileOutputStream(backupFile);
		Session session = null;
		try {
			session = connection.openSession();
			session.execCommand(command);
			InputStream stdout = new StreamGobbler(session.getStdout());
			try (BufferedReader br = new BufferedReader(new InputStreamReader(stdout))) {
				String line = br.readLine();
				String lineReplace = null, lineReplace2 = null, lineReplace3 = null;
				while (line != null) {
					/*
					 * Don't include the USE `database` name since they will
					 * likely differ from local to remote sources, also ignore
					 * comments
					 */
					if (!(line.startsWith("USE") || line.startsWith("--"))) {
						// If this dump will be imported from remote server to local need to change upload prefs
						if(type.equals("pull")) {
							if(line.matches("INSERT INTO `integro_upload_prefs`(.*)")) {
								lineReplace = line.replace(destConfig.getHost(), localConfig.getHost());
								lineReplace2 = lineReplace.replace(destConfig.getDirectory() + "/images", EEExtras.CWD + "/" + cr.getAppDir() + "/images");
								if(!cr.getUpDir().equals(""))
									lineReplace3 = lineReplace2.replace(destConfig.getDirectory() + "/" + cr.getUpDir(), EEExtras.CWD + "/" + cr.getAppDir() + "/" + cr.getUpDir());
								else
									lineReplace3 = lineReplace2;
								// Point the original 'line' to the updated string
								line = lineReplace3;
							}
						}
						// Write out the bytes to the file
						out.write(line.getBytes());
						out.write("\n".getBytes());
						
					}
					line = br.readLine();
				}
			}
		} finally {
			if (session != null) {
				session.close();
			}
			out.flush();
			out.close();
		}

		return backupFile;
	}

	/*
	 * Make local backup of database To do this we're going to exec a process
	 * and grabe the ouput of the mysql dump from the local machine and save to
	 * file in db_backups folder
	 */
	private File localDbBackup() throws IOException {
		File localDbBackup = new File(
				EEExtras.CWD + "/db_backups/" + localConfig.getEnvironment() + "_db_" + timestamp + ".sql");
		OutputStream out = new FileOutputStream(localDbBackup);
		String[] command = { "/bin/sh", "-c",
				EEExtras.PATH + "/mysqldump --opt --add-drop-table --no-create-db --user=" + localConfig.getDbUser()
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
			String lineReplace = null, lineReplace2 = null, lineReplace3 = null;
			InputStreamReader isrStd = new InputStreamReader(stdout);
			BufferedReader brStd = new BufferedReader(isrStd);
			while ((line = brStd.readLine()) != null) {
				/*
				 * Don't include the USE `database` name since they will likely
				 * differ from local to remote sources, also ignore comments
				 */
				if (!(line.startsWith("USE") || line.startsWith("--"))) {
					// If this dump will be uploaded to remote server from local need to change upload prefs
					if(type.equals("push")) {
						if(line.matches("INSERT INTO `integro_upload_prefs`(.*)")) {
							lineReplace = line.replace(localConfig.getHost(), destConfig.getHost());
							lineReplace2 = lineReplace.replace(EEExtras.CWD + "/" + cr.getAppDir() + "/images", destConfig.getDirectory() + "/images");
							if(!cr.getUpDir().equals(""))
								lineReplace3 = lineReplace2.replace(EEExtras.CWD + "/" + cr.getAppDir() + "/" + cr.getUpDir(), destConfig.getDirectory() + "/" + cr.getUpDir());
							else
								lineReplace3 = lineReplace2;
							// Point the original 'line' to the updated string
							line = lineReplace3;
						} 
					}
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
				EEExtras.PATH + "/mysql --verbose --user=" + localConfig.getDbUser() + " --password="
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
				if(lineCount % 5 == 0) System.out.print("--\r");
				else if(lineCount % 5 == 1) System.out.print("\\ \r");
				else if(lineCount % 5 == 2) System.out.print("| \r");
				else if(lineCount % 5 == 3) System.out.print("/ \r");
				else if(lineCount % 5 == 4) System.out.print("--\r");
				lineCount++;
			}

			// Print errors stdout so user knows what went wrong
			while ((val = brErr.readLine()) != null) {
				System.err.println(EEExtras.ANSI_RED + ">> [Error]: " + val + EEExtras.ANSI_RESET);
			}

			int exitVal = proc.waitFor();
			
			// Clean up the loading animation line
			System.out.print("  \r");

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
	 * Push the local database up to the server
	 */
	private void importLocalDbBackup(Connection connection, File file) throws IOException {
		SCPClient scp = new SCPClient(connection);
		scp.put(file.getAbsolutePath(), "/tmp/");
		// Pass things off to the next method to import the database and delete
		// the sql dump file
		System.out.println(EEExtras.ANSI_PURPLE + "\tremote | " + EEExtras.ANSI_RESET + "scp " + file.getAbsolutePath()
				+ " " + destConfig.getSshUser() + "@" + destConfig.getHost() + ":/tmp/" + file.getName());
		importLocalDbBackupToRemote(file);
	}

	/*
	 * Import the local db to the remote server, then remove it
	 */
	private void importLocalDbBackupToRemote(File file) {
		String command = "mysql --verbose --user=" + destConfig.getDbUser() + " --password=" + destConfig.getDbPass()
				+ " --port=" + destConfig.getDbPort() + " --database=" + destConfig.getDatabase() + " < " + "/tmp/"
				+ file.getName();
		
		System.out.println(EEExtras.ANSI_PURPLE + "\tremote | " + EEExtras.ANSI_RESET + command);
		
		List<String> result = new LinkedList<>();
		Session session = null;
		try {
			session = connection.openSession();
			session.execCommand(command);
			InputStream stdout = new StreamGobbler(session.getStdout());
			try (BufferedReader br = new BufferedReader(new InputStreamReader(stdout))) {
				String line = br.readLine();
				int lineCount = 0;
				while (line != null) {
					// Print out loading animation
					if(lineCount % 5 == 0) System.out.print("--\r");
					else if(lineCount % 5 == 1) System.out.print("\\ \r");
					else if(lineCount % 5 == 2) System.out.print("| \r");
					else if(lineCount % 5 == 3) System.out.print("/ \r");
					else if(lineCount % 5 == 4) System.out.print("--\r");
					lineCount++;
					result.add(line);
					line = br.readLine();
				}
			}
		} catch (IOException e) {
			System.out.println("Error importing dabase to server: " + e.getMessage());
		} finally {
			if (session != null) {
				session.close();
			}
		}
		
		// Clean up the loading animation line
		System.out.print("  \r");
		
		// Remove the file after importing it
		try {
			removeTempFile("/tmp/" + file.getName());
		} catch (IOException e) {
			System.out.println("Error removing temporary dump from server: " + e.getMessage());
		}
	}

	/*
	 * Remove the dump from the server
	 */
	private void removeTempFile(String filename) throws IOException {
		String command = "rm " + filename;
		
		System.out.println(EEExtras.ANSI_PURPLE + "\tremote | " + EEExtras.ANSI_RESET + command);
		
		List<String> result = new LinkedList<>();
		Session session = null;
		try {
			session = connection.openSession();
			session.execCommand(command);
			InputStream stdout = new StreamGobbler(session.getStdout());

			try (BufferedReader br = new BufferedReader(new InputStreamReader(stdout))) {
				String line = br.readLine();
				while (line != null) {
					result.add(line);
					line = br.readLine();
				}
			}
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	/*
	 * Creates a connection to host and returns it
	 */
	public Connection connectTo() throws IOException {
		Connection connection = new Connection(destConfig.getHost());
		connection.connect();
		String consolMsg = "";
		if (cr.isUseKeyAuth()) {
			connection.authenticateWithPublicKey(destConfig.getSshUser(), cr.getKeyfile(), cr.getKeyPass());
			consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + "Connecting to "
					+ destConfig.getEnvironment() + " host using PKA " + EEExtras.ANSI_RESET, 80, '▬');
		} else {
			connection.authenticateWithPassword(destConfig.getSshUser(), destConfig.getSshPass());
			consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + "Connecting to "
					+ destConfig.getEnvironment() + " host using PASSWORD " + EEExtras.ANSI_RESET, 80, '▬');
		}
		System.out.println(consolMsg);
		return connection;
	}
}
