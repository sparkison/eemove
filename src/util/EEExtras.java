/**
 * @author Shaun Parkison
 * @info Was tired of manually creating rsync commands and fussing with
 * Capistrano trying to automate the ExpressionEngine deployment process, so I created this simple
 * Java program to handle the heavy lifting and tedious tasks associated with that process.
 * 
 */

package util;

public interface EEExtras {

	/*
	 * Some colors, just for fun! ;) Using to colorize the outputs of eemove
	 * tasks
	 */
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	/*
	 * Get the current working directory; will be used in a couple of places
	 */
	public static final String CWD = System.getProperty("user.dir");

	/*
	 * Determine OS
	 */
	public static final String OS = System.getProperty("os.name");
	
	/*
	 * Directories
	 * Directories for EE2 and EE3 (currently used in main EEMove class)
	 * 
	 */
	
	// Add-on directories, used for "addons" command
	public static String EE3_ADDONS_THEMES = "/themes/user/";
	public static String EE2_ADDONS_THEMES = "/themes/third_party/";
	public static String EE3_ADDONS_FILES = "/user/addons/";
	public static String EE2_ADDONS_FILES = "/expressionengine/third_party/";
	
	// Config file location, used for "update" command
	public static String EE3_CONFIG_FILE = "/user/config/config.php";
	public static String EE2_CONFIG_FILE = "/expressionengine/config/config.php";
	
	// System directories, used for "update" command
	public static String EE3_SYSTEM_FILES = "/ee/"; // EE3 places system specific files in sub-directory
	public static String EE2_SYSTEM_FILES = "/"; // Just the system directory, no sub-directory
	public static String EE3_SYSTEM_THEMES = "/themes/ee/"; // EE3 places system specific theme resources in sub-directory
	public static String EE2_SYSTEM_THEMES = "/themes/"; // Just the root themes folder, no sub-directory
	
	// Image directory
	public static String EE3_IMAGE_UPLOADS = "/images/uploads/";
	public static String EE2_IMAGE_UPLOADS = "/images/uploads/";
	
	// Template directories
	public static String EE3_TEMPLATES = "/user/templates/";
	public static String EE2_TEMPLATES = "/expressionengine/templates/";
	public static String EE3_TEMPLATE_RESOURCES = "/dist/"; // The template resources; css, js, etc.
	public static String EE2_TEMPLATE_RESOURCES = "/dist/"; // The template resources; css, js, etc.
	
}

