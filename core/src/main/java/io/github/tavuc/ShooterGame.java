package io.github.tavuc;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

/**
 * Main game class for the 2.5D top-down shooter game.
 * This class manages the screen transitions and global resources.
 */
public class ShooterGame extends Game {
    // Constants
    public static final String TITLE = "2.5D Top-Down Shooter";
    public static final float VIEWPORT_WIDTH = 30f;
    public static final float ARENA_RADIUS = 45f;
    public static final float ARENA_BORDER_WIDTH = 0.5f;
    
    // Global resources
    public SpriteBatch spriteBatch;
    public ModelBatch modelBatch;
    public AssetManager assets;
    
    // Game state
    private GameState gameState;
    
    @Override
    public void create() {
        // Initialize resources
        spriteBatch = new SpriteBatch();
        modelBatch = new ModelBatch();
        assets = new AssetManager();
        
        // Load assets
        loadAssets();
        
        // Create game state
        gameState = new GameState();
        
        // Set initial screen
        setScreen(new GameScreen(this));
    }
    
    /**
     * Load all game assets
     */
    private void loadAssets() {
        // TODO: Load textures, models, sounds
        assets.finishLoading(); // Block until loading is complete
    }
    
    @Override
    public void render() {
        // Clear the screen
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        // Update the active screen
        super.render();
    }
    
    @Override
    public void dispose() {
        // Dispose resources
        spriteBatch.dispose();
        modelBatch.dispose();
        assets.dispose();
        
        // Dispose current screen
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
    
    /**
     * Get the game state
     */
    public GameState getGameState() {
        return gameState;
    }
    
    /**
     * Reset the game state
     */
    public void resetGameState() {
        gameState = new GameState();
    }
}