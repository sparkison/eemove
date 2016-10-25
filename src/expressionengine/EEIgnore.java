package expressionengine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class EEIgnore {

	private final File rsyncIgnore;
	
	public EEIgnore(File rsyncIgnore) {
		this.rsyncIgnore = rsyncIgnore;
	}
	
	/**
	 * See if EE ignore file exists, and create it if not
	 * 
	 * @param rsyncIgnore
	 * @throws IOException
	 */
	public void init() throws IOException {
		if (!rsyncIgnore.exists()) {
			FileWriter ignoreFile = new FileWriter(rsyncIgnore);
			ignoreFile.write("*.sql");
			ignoreFile.write("\n");
			ignoreFile.write("*.swp");
			ignoreFile.write("\n");
			ignoreFile.write(".git");
			ignoreFile.write("\n");
			ignoreFile.write(".sass-cache");
			ignoreFile.write("\n");
			ignoreFile.write(".DS_Store");
			ignoreFile.write("\n");
			ignoreFile.write("npm-debug.log");
			ignoreFile.write("\n");
			ignoreFile.write("db_backups");
			ignoreFile.write("\n");
			ignoreFile.write("node_modules");
			ignoreFile.write("\n");
			ignoreFile.write("bower_components");
			ignoreFile.write("\n");
			ignoreFile.write("sized/");
			ignoreFile.write("\n");
			ignoreFile.write("thumbs/");
			ignoreFile.write("\n");
			ignoreFile.write("_thumbs/");
			ignoreFile.write("\n");
			ignoreFile.write("# ignore system in the case it's in same directory as app");
			ignoreFile.write("\n");
			ignoreFile.write("system");
			ignoreFile.write("\n");
			ignoreFile.write("/user/cache/");
			ignoreFile.write("\n");
			ignoreFile.write("eemove.config");
			ignoreFile.write("\n");
			ignoreFile.write("eemove.ignore");
			ignoreFile.write("\n");
			ignoreFile.close();
		}
		rsyncIgnore.setExecutable(true);
		rsyncIgnore.setReadable(true);
	}
	
}
