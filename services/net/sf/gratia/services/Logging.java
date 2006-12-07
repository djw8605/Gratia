package net.sf.gratia.services;

import java.util.*;
import java.text.*;
import java.io.*;
import java.util.logging.*;

public class Logging
{
		static Logger logger;
		static boolean initialized = false;
		static boolean console = false;
		static DateFormat format = new SimpleDateFormat("kk:mm:ss");

		public static void initialize(String path,String maxSize,String useConsole,String level)
		{
				if (initialized)
						return;
				try
						{
								int limit = Integer.parseInt(maxSize);
								int numLogFiles = 3;
								FileHandler fh = new FileHandler(Configuration.getCatalinaHome() + path, limit, numLogFiles);
								fh.setFormatter(new SimpleFormatter());
								// Add to logger
								logger = Logger.getLogger("gratia");
								logger.setUseParentHandlers(false);
								if (useConsole.equals("1"))
										{
												logger.addHandler(new ConsoleHandler());
												console = true;
										}
								logger.addHandler(fh);
								if (level.equals("ALL"))
										logger.setLevel(Level.ALL);
								else if (level.equals("CONFIG"))
										logger.setLevel(Level.CONFIG);
								else if (level.equals("FINE"))
										logger.setLevel(Level.FINE);
								else if (level.equals("FINER"))
										logger.setLevel(Level.FINER);
								else if (level.equals("FINEST"))
										logger.setLevel(Level.FINEST);
								else if (level.equals("INFO"))
										logger.setLevel(Level.INFO);
								else if (level.equals("OFF"))
										logger.setLevel(Level.OFF);
								else if (level.equals("SEVERE"))
										logger.setLevel(Level.SEVERE);
								else if (level.equals("WARNING"))
										logger.setLevel(Level.WARNING);
								System.out.println("\nLogging Level: " + logger.getLevel() + "\n");
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
				initialized = true;
		}

		public static void log(String message)
		{
				if (! initialized)
						{
								System.out.println("Logger Not Initialized !!");
								return;
						}
				logger.finest(message);
				if (console)
						System.out.println(format.format(new Date()) + ": " + message);
		}

		public static void info(String message)
		{
				if (! initialized)
						{
								System.out.println("Logger Not Initialized !!");
								return;
						}
				logger.info(message);
				if (console)
						System.out.println(format.format(new Date()) + ": " + message);
		}

		public static void warning(String message)
		{
				if (! initialized)
						{
								System.out.println("Logger Not Initialized !!");
								return;
						}
				logger.warning(message);
				if (console)
						System.out.println(format.format(new Date()) + ": " + message);
		}

		public static void debug(String message)
		{
				if (! initialized)
						{
								System.out.println("Logger Not Initialized !!");
								return;
						}
				Logging.log(message);
				if (console)
						System.out.println(format.format(new Date()) + ": " + message);
		}

}
