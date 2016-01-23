/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 * 
 */

package helpers;

public class EEconfig {

	private String environment;
	private String host;
	private String directory;
	private String sshUser;
	private String sshPass;
	private String sshPort;
	private String database;
	private String dbUser;
	private String dbPass;
	private String dbHost;
	private String dbPort;

	public EEconfig(String environment, String host, String directory, String sshUser, String sshPass, String sshPort, String database,
			String dbUser, String dbPass, String dbHost, String dbPort) {
		this.environment = environment;
		this.host = host;
		this.directory = directory;
		this.sshUser = sshUser;
		this.sshPass = sshPass;
		this.sshPort = sshPort;
		this.database = database;
		this.dbUser = dbUser;
		this.dbPass = dbPass;
		this.dbHost = dbHost;
		this.dbPort = dbPort;
	}

	/* Config file format [use same for toString to make things consistent]:
	 * env=ENVIRONMENT
	 * env_host=HOST
	 * env_dir=DIRECTORY
	 * env_user=USER 
	 * env_pass=PASS
	 * env_db=ENV_DB 
	 * env_dbuser=DB_USER 
	 * env_dbpass=DB_PASS
	 * env_dbhost=DB_HOST
	 * env_dbport=D_BPORT
	 */
	@Override
	public String toString() {
		return "#\n# Config for environment: " + getEnvironment() + "\n#" + "\nenv=" + getEnvironment() + "\nenv_host=" + getHost() + "\nenv_dir=" + getDirectory() + "\nenv_user=" + getSshUser()
				+ "\nenv_pass=" + getSshPass() + "\nenv_port=" + getSshPort() + "\nenv_db=" + getDatabase() + "\nenv_dbuser=" + getDbUser() + "\nenv_dbpass=" + getDbPass()
				+ "\nenv_dbhost=" + getDbHost() + "\nenv_dbport=" + getDbPort() + "\n\n";
	}

	public String getEnvironment() {
		return new String(environment);
	}

	public String getHost() {
		return new String(host);
	}
	
	public String getDirectory() {
		return new String(directory);
	}

	public String getSshUser() {
		return new String(sshUser);
	}

	public String getSshPass() {
		return new String(sshPass);
	}
	
	public String getSshPort() {
		return new String(sshPort);
	}

	public String getDatabase() {
		return new String(database);
	}

	public String getDbUser() {
		return new String(dbUser);
	}

	public String getDbPass() {
		return new String(dbPass);
	}

	public String getDbHost() {
		return new String(dbHost);
	}

	public String getDbPort() {
		return new String(dbPort);
	}

}
