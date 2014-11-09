package com.ntechinternational.slap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogUtil {
	public static Logger logger = LogManager.getRootLogger();
	
	public static void debug(Object message){
		logger.debug(message);
	}
	
	public static void error(Object message){
		logger.error(message);
	}
	
	public static void trace(Object message){
		logger.trace(message);
	}
}
