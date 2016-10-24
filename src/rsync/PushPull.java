/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 * 
 */

package rsync;

import com.google.common.base.Strings;

import expressionengine.EEConfigReader;
import helpers.CommandExecuter;
import helpers.Config;
import util.Extras;

public class PushPull implements Extras {

	private String src;
	private String dest;
	private Config config;
	private String host;
	private String user;
	private String type;
	private EEConfigReader cr;
	private boolean isDryRun = true;
	private CommandExecuter ce;

	public PushPull(String src, String dest, String type, boolean dryRun, Config config, EEConfigReader cr) {
		this.src = Extras.CWD + "/" + src;
		this.dest = dest;
		this.isDryRun = dryRun;
		this.type = type;
		this.config = config;
		this.user = config.getSshUser();
		this.host = config.getHost();
		this.cr = cr;
		this.ce = new CommandExecuter(config, false);
		try {
			push(this.config);
		} catch (Exception e) {
			System.out.println("Error pushing directory/files: ");
			System.out.println(e.getMessage());
		}
	}

	private void push(Config config) throws Exception {
		// Currently uses passwordless SSH keys to login, will be prompted for
		// password if not set
		String dryRun = "";
		if (isDryRun)
			dryRun = " --dry-run";
		String rsyncCommand = "";

		String ssh = "";
		String rsyncSsh = "";
		if( cr.useKeyAuth ) {
			rsyncSsh = " -e 'ssh -i " + cr.getKeyfile() + "'";
		} else {
			ssh = cr.getSshPassPath() + "sshpass -e";
		}

		if (type.equals("push"))
			rsyncCommand = "rsync --progress" + rsyncSsh + " -rlpt --compress --omit-dir-times --delete" + dryRun + " --exclude-from="
					+ Extras.CWD + "/eemove.ignore " + src + " " + user + "@" + host + ":" + dest;
		else
			rsyncCommand = "rsync --progress" + rsyncSsh + " -rlpt --compress --omit-dir-times --delete" + dryRun + " --exclude-from="
					+ Extras.CWD + "/eemove.ignore " + user + "@" + host + ":" + dest + " " + src;


		String commandWithAuth = ssh + " " + rsyncCommand;

		String consolMsg = "";
		if( ! this.ce.executeCommand( commandWithAuth ) ) {
			consolMsg = Strings.padEnd("▬▬ ✓ " + Extras.ANSI_RED + "Error: unable to execute MYSQL command " + Extras.ANSI_RESET, 80, '▬');
			System.out.println(consolMsg);
			System.out.println(Extras.ANSI_YELLOW + "Please double check your credentials and eemove config file and try again" + Extras.ANSI_RESET);
			System.exit(-1);
		}

		consolMsg = Strings.padEnd(
				"▬▬ ✓ " + Extras.ANSI_CYAN + "Complete! " + Extras.ANSI_RESET, 80, '▬');
		System.out.println(consolMsg);

	}

}
