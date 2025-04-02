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
        // Use a better-sized frustum for visibility
        camera.setToOrtho(false, WORLD_WIDTH * 1.2f, WORLD_HEIGHT * 1.2f);
        
        // Set a lower camera angle for more side view
        camera.rotate(15, 1, 0, 0); // Lowered to 15-degree elevation for more side view
        camera.rotate(45, 0, 1, 0); // 45-degree rotation
        
        // Adjust zoom to focus more on the player and immediate surroundings
        camera.zoom = 0.5f; // Decreased to 0.5f for a closer view
        
        // Set a larger far plane to prevent clipping
        camera.far = 1000f;
        
        camera.update();
        
        logger.info("Camera initialized with lower angle for side-view isometric perspective");
    }
    
    /**
     * Initializes the lighting environment for 3D rendering.
     */
    private void initEnvironment() {
        environment = new Environment();
        // Increased ambient light for better visibility
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.6f, 1f));
        // Made directional light brighter and adjusted direction
        environment.add(new DirectionalLight().set(0.9f, 0.9f, 0.9f, -1f, -0.8f, -0.2f));
        
        logger.info("Environment and lighting initialized");
    }
    
    /**
     * Called when the game is rendered.
     * Clears the screen and delegates rendering to the active screen.
     */
    @Override
    public void render() {
        // Set slightly lighter clear color for better contrast
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1); // Slightly lighter than pure black
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