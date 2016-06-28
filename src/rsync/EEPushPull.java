/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 * 
 */

package rsync;

import com.google.common.base.Strings;

import helpers.CommandExecuter;
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
	private CommandExecuter ce;

	public EEPushPull(String src, String dest, String type, boolean dryRun, EEconfig config, ConfigReader cr) {
		this.src = EEExtras.CWD + "/" + src;
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

	private void push(EEconfig config) throws Exception {
		// Currently uses passwordless SSH keys to login, will be prompted for
		// password if not set
		String dryRun = "";
		if (isDryRun)
			dryRun = " --dry-run";
		String rsyncCommand = "";
		
		String ssh = "";
		if( cr.useKeyAuth ) {
			ssh = "ssh -i " + cr.getKeyfile();
		} else {
			ssh = cr.getSshPassPath() + "sshpass -e";
		}
		
		if (type.equals("push"))
			rsyncCommand = "rsync -rv" + dryRun + " --exclude-from="
					+ EEExtras.CWD + "/eemove.ignore " + src + " " + user + "@" + host + ":" + dest;
		else
			rsyncCommand = "rsync -rv" + dryRun + " --exclude-from="
					+ EEExtras.CWD + "/eemove.ignore " + user + "@" + host + ":" + dest + " " + src;
		
		
		String commandWithAuth = ssh + " " + rsyncCommand;
		
		String consolMsg = "";
		if( ! this.ce.executeCommand( commandWithAuth ) ) {
			consolMsg = Strings.padEnd("▬▬ ✓ " + EEExtras.ANSI_RED + "Error: unable to execute MYSQL command " + EEExtras.ANSI_RESET, 80, '▬');
			System.out.println(consolMsg);
			System.out.println(EEExtras.ANSI_YELLOW + "Please double check your credentials and eemove config file and try again" + EEExtras.ANSI_RESET);
			System.exit(-1);
		}
		
		consolMsg = Strings.padEnd(
				"▬▬ ✓ " + EEExtras.ANSI_CYAN + "Complete! " + EEExtras.ANSI_RESET, 80, '▬');
		System.out.println(consolMsg);
		
	}

}
