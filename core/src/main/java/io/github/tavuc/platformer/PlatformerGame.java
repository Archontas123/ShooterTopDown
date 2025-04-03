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
    public static final float PLAYER_SIZE = TILE_SIZE * 0.8f; 
    
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
        camera.setToOrtho(false, WORLD_WIDTH * 1.2f, WORLD_HEIGHT * 1.2f);
        
        camera.rotate(15, 1, 0, 0); 
        camera.rotate(45, 0, 1, 0); 
        
        camera.zoom = 0.5f; 
        
        camera.far = 1000f;
        
        camera.update();
        
        logger.info("Camera initialized with lower angle for side-view isometric perspective");
    }
    
    /**
     * Initializes the lighting environment for 3D rendering.
     */
    private void initEnvironment() {
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.6f, 1f));
        environment.add(new DirectionalLight().set(0.9f, 0.9f, 0.9f, -1f, -0.8f, -0.2f));
        
        logger.info("Environment and lighting initialized");
    }
    
    /**
     * Called when the game is rendered.
     * Clears the screen and delegates rendering to the active screen.
     */
    @Override
    public void render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1); 
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        
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