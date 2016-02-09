/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 * 
 */

package rsync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Strings;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import helpers.ConfigReader;
import helpers.EEconfig;
import util.EEExtras;

public class EEPushPull implements EEExtras {

	private String src;
	private String dest;
	private EEconfig config;
	private String host;
	private String user;
	private String type;
	private ConfigReader cr;
	private boolean isDryRun = true;

	public EEPushPull(String src, String dest, String type, boolean dryRun, EEconfig config, ConfigReader cr) {
		this.src = EEExtras.CWD + "/" + src;
		this.dest = dest;
		this.isDryRun = dryRun;
		this.type = type;
		this.config = config;
		this.user = config.getSshUser();
		this.host = config.getHost();
		this.cr = cr;
		try {
			push(this.config);
		} catch (Exception e) {
			System.out.println("Error pushing directory/files: ");
			System.out.println(e.getMessage());
		}
	}

	private void push(EEconfig config) throws Exception {
		// Currently uses passwordless SSH keys to login, will be prompted for
		// password if not set
		String dryRun = "";
		if (isDryRun)
			dryRun = "--dry-run";
		String rsyncCommand = "";
		if (type.equals("push"))
			rsyncCommand = "rsync -rv -e 'ssh -p " + config.getSshPort() + "' " + dryRun + " --exclude-from="
					+ EEExtras.CWD + "/eemove.ignore " + src + " " + user + "@" + host + ":" + dest;
		else
			rsyncCommand = "rsync -rv -e 'ssh -p " + config.getSshPort() + "' " + dryRun + " --exclude-from="
					+ EEExtras.CWD + "/eemove.ignore " + user + "@" + host + ":" + dest + " " + src;

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
		
		if (cr.isUseKeyAuth()) {
			bashFile.write("#!/bin/bash");
			bashFile.write("\n");
			bashFile.write(rsyncCommand);
			bashFile.write("\n");
			bashFile.close();
		} else {
			bashFile.write("#!/usr/bin/expect -f");
			bashFile.write("\n");
			bashFile.write("set timeout -1");
			bashFile.write("\n");
			bashFile.write("spawn " + rsyncCommand);
			bashFile.write("\n");
			bashFile.write("expect -re \"assword:\"");
			bashFile.write("\n");
			bashFile.write("send \"" + config.getSshPass() + "\\n\"");
			bashFile.write("\n");
			bashFile.write("expect eof");
			bashFile.write("\n");
			bashFile.close();
		}
		Runtime rt = Runtime.getRuntime();
		Process proc = null;
		try {
			// Using underlying 'expect' command to pass password for rsync
			if (cr.isUseKeyAuth()) {
				proc = rt.exec("sh " + tempBashCmd.getAbsolutePath());
			} else {
				proc = rt.exec("expect " + tempBashCmd.getAbsolutePath());
			}
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
			while ((val = brStd.readLine()) != null) {
				System.out.println(">>" + val);
				if (new String(val.getBytes()).contains("assword:")) {
					System.out.println(">>sending password ...");
				}
			}

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
		tempBashCmd.delete();
		String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + "Transfer complete " + EEExtras.ANSI_RESET, 80, '▬');
		System.out.println(consolMsg);
	}

	/*
	 * If uploading files to server, ensure proper permissions set
	 */
	private void fixPermissions(Connection connection) throws IOException {
		String consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_CYAN + "Updating permissions " + EEExtras.ANSI_RESET, 80, '▬');
		System.out.println(consolMsg);
		// TODO
	}

	/*
	 * Creates a connection to host and returns it
	 */
	public Connection connectTo() throws IOException {
		Connection connection = new Connection(config.getHost());
		connection.connect();
		if (cr.isUseKeyAuth()) {
			connection.authenticateWithPublicKey(config.getSshUser(), cr.getKeyfile(), cr.getKeyPass());
		} else {
			connection.authenticateWithPassword(config.getSshUser(), config.getSshPass());
		}
		return connection;
	}

}
