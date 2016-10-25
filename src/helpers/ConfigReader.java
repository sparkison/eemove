package helpers;

import java.io.File;
import java.util.HashMap;

public abstract class ConfigReader {
	
	private final String CONF;
	private File keyfile;
	private String keyPass;
	private String sysDir;
	private String appDir;
	private Integer cmsVer;
	private String upDir;
	private String sshPassPath;
	private String mysqlPath;
	private boolean useKeyAuth;
	private boolean aboveRoot;
	
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
	
	public abstract HashMap<String, Config> getConfig();
	public abstract void confInit();
	
	protected String getConf() {
		return CONF;
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
	
	public Integer getCmsVer() {
		return cmsVer;
	}
	
	/*
	 * Setters
	 */
	public void setSysDir(String sysDir) {
		this.sysDir = sysDir;
	}

	public void setAppDir(String appDir) {
		this.appDir = appDir;
	}

	public void setUpDir(String upDir) {
		this.upDir = upDir;
	}

	public void setIsAboveRoot(boolean aboveRoot) {
		this.aboveRoot = aboveRoot;
	}

	public void setKeyfile(File keyfile) {
		this.keyfile = keyfile;
	}

	public void setKeyPass(String keyPass) {
		this.keyPass = keyPass;
	}

	public void setIsUseKeyAuth(boolean useKeyAuth) {
		this.useKeyAuth = useKeyAuth;
	}
	
	public void setSshPassPath(String sshPassPath) {
		this.sshPassPath = sshPassPath;
	}

	public void setMysqlPath(String mysqlPath) {
		this.mysqlPath = mysqlPath;
	}
	
	public void setCmsVer(Integer cmsVer) {
		this.cmsVer = cmsVer;
	}
}
