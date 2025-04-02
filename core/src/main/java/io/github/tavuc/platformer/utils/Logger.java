package io.github.tavuc.platformer.utils;

import com.badlogic.gdx.Gdx;

/**
 * Simple logging utility for tracking game events and debugging.
 */
public class Logger {
    private final String tag;
    
    /**
     * Creates a new logger with the specified tag.
     * 
     * @param tag The tag to use for this logger's messages
     */
    public Logger(String tag) {
        this.tag = tag;
    }
    
    /**
     * Logs an informational message.
     * 
     * @param message The message to log
     */
    public void info(String message) {
        Gdx.app.log(tag, message);
    }
    
    /**
     * Logs a debug message.
     * 
     * @param message The message to log
     */
    public void debug(String message) {
        Gdx.app.debug(tag, message);
    }
    
    /**
     * Logs an error message.
     * 
     * @param message The message to log
     */
    public void error(String message) {
        Gdx.app.error(tag, message);
    }
    
    /**
     * Logs an error message with an exception.
     * 
     * @param message The message to log
     * @param exception The exception to log
     */
    public void error(String message, Throwable exception) {
        Gdx.app.error(tag, message, exception);
    }
}