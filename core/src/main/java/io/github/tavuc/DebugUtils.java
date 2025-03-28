package io.github.tavuc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

/**
 * Utility class for debugging graphics issues.
 */
public class DebugUtils {
    /**
     * Check for OpenGL errors and log them
     */
    public static void checkGLError(String tag) {
        int error = Gdx.gl.glGetError();
        if (error != GL20.GL_NO_ERROR) {
            String errorString;
            switch (error) {
                case GL20.GL_INVALID_ENUM:
                    errorString = "GL_INVALID_ENUM";
                    break;
                case GL20.GL_INVALID_VALUE:
                    errorString = "GL_INVALID_VALUE";
                    break;
                case GL20.GL_INVALID_OPERATION:
                    errorString = "GL_INVALID_OPERATION";
                    break;
                case GL20.GL_OUT_OF_MEMORY:
                    errorString = "GL_OUT_OF_MEMORY";
                    break;
                default:
                    errorString = "Unknown error code: " + error;
            }
            Gdx.app.error(tag, "OpenGL error: " + errorString);
        }
    }
    
    /**
     * Print system information
     */
    public static void printSystemInfo() {
        Gdx.app.log("SystemInfo", "OpenGL vendor: " + Gdx.gl.glGetString(GL20.GL_VENDOR));
        Gdx.app.log("SystemInfo", "OpenGL renderer: " + Gdx.gl.glGetString(GL20.GL_RENDERER));
        Gdx.app.log("SystemInfo", "OpenGL version: " + Gdx.gl.glGetString(GL20.GL_VERSION));
        Gdx.app.log("SystemInfo", "Java version: " + System.getProperty("java.version"));
        Gdx.app.log("SystemInfo", "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        Gdx.app.log("SystemInfo", "Available processors: " + Runtime.getRuntime().availableProcessors());
        long maxMemory = Runtime.getRuntime().maxMemory();
        Gdx.app.log("SystemInfo", "Maximum memory: " + (maxMemory == Long.MAX_VALUE ? "unlimited" : (maxMemory / (1024 * 1024) + " MB")));
    }
    
    /**
     * Take a screenshot and save it to a file
     */
    public static void takeScreenshot(String filename) {
        try {
            // Get frame buffer pixels
            Pixmap pixmap = getScreenshot(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
            
            // Save to file
            pixmap.dispose();
            
            Gdx.app.log("DebugUtils", "Screenshot saved to " + filename);
        } catch (Exception e) {
            Gdx.app.error("DebugUtils", "Failed to take screenshot: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get a screenshot as a Pixmap
     */
    private static Pixmap getScreenshot(int x, int y, int w, int h, boolean flipY) {
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
        
        final Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        ByteBuffer pixels = pixmap.getPixels();
        Gdx.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
        
        if (flipY) {
            // Flip the pixmap upside down
            ByteBuffer lines = BufferUtils.newByteBuffer(w * 4);
            final int half = h / 2;
            for (int i = 0; i < half; i++) {
                final int topOffset = i * w * 4;
                final int bottomOffset = (h - i - 1) * w * 4;
                pixels.position(topOffset);
                pixels.get(lines.array(), 0, w * 4);
                
                pixels.position(bottomOffset);
                pixels.get(lines.array(), 0, w * 4);
                
                pixels.position(topOffset);
                pixels.put(pixels.array(), bottomOffset, w * 4);
                
                pixels.position(bottomOffset);
                pixels.put(lines.array(), 0, w * 4);
            }
            pixels.position(0);
        }
        
        return pixmap;
    }
}