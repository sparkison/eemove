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
	 * Set the path variable to the mysql/mysqldump commands
	 * 
	 * TODO find a better way to do this? Currently will need to compile for
	 * 		different OS (Mac vs Linux) could reside in multiple different paths
	 * 		depending on end user system
	 */
	public static final String PATH = OS.indexOf("Mac") >= 0 ? "/Applications/MAMP/Library/bin" : "/usr/local/bin";
	
}
