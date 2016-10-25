package craft;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CRAFTIgnore {

	private final File rsyncIgnore;
	
	public CRAFTIgnore(File rsyncIgnor) {
		this.rsyncIgnore = rsyncIgnor;
	}
	
	/**
	 * See if Craft ignore file exists, and create it if not
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
			ignoreFile.write("app");
			ignoreFile.write("\n");
			ignoreFile.write("craftmove.config");
			ignoreFile.write("\n");
			ignoreFile.write("craftmove.ignore");
			ignoreFile.write("\n");
			ignoreFile.close();
		}
		rsyncIgnore.setExecutable(true);
		rsyncIgnore.setReadable(true);
	}
	
}
