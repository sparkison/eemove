package craft;

import java.util.HashMap;

import helpers.Config;
import helpers.ConfigReader;

public class CRAFTSync {

	// Class variables
	private HashMap<String, Config> config;

	// Folder structure configuration, set some defaults just in case
	private String uploadDir;
	private ConfigReader cr;

	// Constructor
	public CRAFTSync(String[] arguments, HashMap<String, Config> config, ConfigReader cr, String cmsApp, String cmsSystem, String uploadDir, boolean appAboveRoot) {

		this.config = config;
		this.uploadDir = uploadDir;
		this.cr = cr;
		
	}

}
