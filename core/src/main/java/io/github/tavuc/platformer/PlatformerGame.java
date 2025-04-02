package io.github.tavuc.platformer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import io.github.tavuc.platformer.screens.MainMenuScreen;
import io.github.tavuc.platformer.utils.Logger;

/**
 * Main entry point for the platformer game.
 * Sets up the game environment and manages screen transitions.
 */
public class PlatformerGame extends Game {
    
    public static final float WORLD_WIDTH = 800f;
    public static final float WORLD_HEIGHT = 450f;
    public static final float TILE_SIZE = 5f;
    public static final float PLAYER_SIZE = TILE_SIZE * 0.8f; // Player is smaller than a single tile
    
    private OrthographicCamera camera;
    private Environment environment;
    private Logger logger;
    
    /**
     * Called when the game is created.
     * Initializes core game components and sets the starting screen.
     */
    @Override
    public void create() {
        logger = new Logger("PlatformerGame");
        logger.info("Initializing game...");
        
        initCamera();
        initEnvironment();
        
        setScreen(new MainMenuScreen(this));
        
        logger.info("Game initialized successfully");
    }
    
    /**
     * Initializes the isometric camera for the game.
     */
    private void initCamera() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, WORLD_WIDTH, WORLD_HEIGHT);
        
        // Set isometric angle to match the screenshot exactly
        camera.rotate(30, 1, 0, 0); // 30-degree elevation
        camera.rotate(45, 0, 1, 0); // 45-degree rotation
        
        // Adjust zoom to better view the single platform
        camera.zoom = 0.5f;
        
        camera.update();
        
        logger.info("Camera initialized with isometric perspective");
    }
    
    /**
     * Initializes the lighting environment for 3D rendering.
     */
    private void initEnvironment() {
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.5f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        
        logger.info("Environment and lighting initialized");
    }
    
    /**
     * Called when the game is rendered.
     * Clears the screen and delegates rendering to the active screen.
     */
    @Override
    public void render() {
        // Set proper clear color and clear the screen
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.02f, 1); // Pure black for better contrast
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        // Enable depth testing for proper 3D rendering
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        
        // Disable face culling to ensure all model faces are rendered
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        
        super.render();
    }
    
    /**
     * Called when the game is disposed.
     * Releases all resources.
     */
    @Override
    public void dispose() {
        super.dispose();
        logger.info("Game disposed");
    }
    
    /**
     * Gets the main camera for the game.
     * 
     * @return The main OrthographicCamera
     */
    public OrthographicCamera getCamera() {
        return camera;
    }
    
    /**
     * Gets the lighting environment for 3D rendering.
     * 
     * @return The Environment
     */
    public Environment getEnvironment() {
        return environment;
    }
}