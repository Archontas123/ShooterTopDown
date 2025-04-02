package io.github.tavuc.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import io.github.tavuc.platformer.PlatformerGame;
import io.github.tavuc.platformer.entities.Player;
import io.github.tavuc.platformer.utils.Logger;
import io.github.tavuc.platformer.world.GameWorld;

/**
 * The main game screen that handles the gameplay.
 * Manages the game world, player, and input handling.
 */
public class GameScreen implements Screen {
    private final PlatformerGame game;
    private final Logger logger;
    private final ModelBatch modelBatch;
    
    private GameWorld gameWorld;
    private Player player;
    
    /**
     * Creates a new game screen.
     * 
     * @param game The main game instance
     */
    public GameScreen(PlatformerGame game) {
        this.game = game;
        this.logger = new Logger("GameScreen");
        this.modelBatch = new ModelBatch();
        
        initializeWorld();
        
        logger.info("Game screen initialized");
    }
    
    /**
     * Initializes the game world and player.
     */
    private void initializeWorld() {
        gameWorld = new GameWorld(logger);
        
        // Create player at spawn position
        Vector3 spawnPosition = gameWorld.getSpawnPosition();
        player = new Player(spawnPosition, logger);
        
        logger.info("World and player initialized at position: " + spawnPosition);
    }
    
    @Override
    public void show() {
        logger.info("Game screen shown");
    }
    
    @Override
    public void render(float delta) {
        // Clear the screen with deep space black
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        // Disable face culling to ensure all faces are rendered
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        
        // Handle input
        handleInput(delta);
        
        // Update game logic
        update(delta);
        
        // Render the world
        renderWorld();
    }
    
    /**
     * Handles player input.
     * 
     * @param delta Time since the last frame
     */
    private void handleInput(float delta) {
        // Track key states
        boolean wPressed = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean sPressed = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean aPressed = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean dPressed = Gdx.input.isKeyPressed(Input.Keys.D);
        
        // Update player's movement flags (used during jumps)
        player.setMovementFlags(wPressed, sPressed, aPressed, dPressed);
        
        // WASD movement (with W and S swapped)
        boolean moved = false;
        
        // S now moves forward (was W)
        if (sPressed) {
            player.move(0, 1, delta);
            moved = true;
        }
        // W now moves backward (was S)
        if (wPressed) {
            player.move(0, -1, delta);
            moved = true;
        }
        if (aPressed) {
            player.move(-1, 0, delta);
            moved = true;
        }
        if (dPressed) {
            player.move(1, 0, delta);
            moved = true;
        }
        
        // Jump ability - purely vertical but maintaining horizontal momentum if keys pressed
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            player.jump();
        }
        
        // Set animation state
        if (moved) {
            player.setAnimationState(Player.AnimationState.WALKING);
        } else {
            player.setAnimationState(Player.AnimationState.IDLE);
        }
        
        // Dash ability
        if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)) {
            // Get facing direction for the dash
            float dirX = 0;
            float dirY = 0;
            
            if (sPressed) dirY = 1;
            if (wPressed) dirY = -1;
            if (aPressed) dirX = -1;
            if (dPressed) dirX = 1;
            
            // If no direction, dash forward
            if (dirX == 0 && dirY == 0) dirY = 1;
            
            player.dash(dirX, dirY);
        }
    }
    
    /**
     * Updates the game logic.
     * 
     * @param delta Time since the last frame
     */
    private void update(float delta) {
        // Update player
        player.update(delta);
        
        // Check if player has fallen off the map
        Vector3 playerPos = player.getPosition();
        if (playerPos.y < gameWorld.getDeathHeight()) {
            respawnPlayer();
            // Skip the rest of the update to ensure clean respawn
            return;
        }
        
        // Update camera to follow player
        updateCamera();
        
        // Check for collisions with the world
        gameWorld.checkCollisions(player);
    }
    
    /**
     * Respawns the player at the spawn position.
     * Handles complete player reset to avoid physics issues.
     */
    private void respawnPlayer() {
        Vector3 spawnPosition = gameWorld.getSpawnPosition();
        
        // Reset the player's state completely
        player.setPosition(spawnPosition);
        player.resetVelocity();
        player.setAnimationState(Player.AnimationState.IDLE);
        
        // Only log respawn once to avoid console spam
        logger.info("Player respawned at: " + spawnPosition);
        
        // Update camera immediately to avoid view of the void
        updateCamera();
    }
    
    /**
     * Updates the camera to follow the player.
     */
    private void updateCamera() {
        Vector3 playerPos = player.getPosition();
        
        // Position camera further back to see more of the platform
        game.getCamera().position.set(
                playerPos.x + 70, // Larger offset for wider view
                playerPos.y + 70, // Higher position for better overview
                playerPos.z + 70  // Maintain isometric view
        );
        game.getCamera().lookAt(playerPos);
        game.getCamera().update();
    }
    
    /**
     * Renders the game world.
     */
    private void renderWorld() {
        // Configure rendering settings for proper visibility
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        
        modelBatch.begin(game.getCamera());
        
        // Render world
        gameWorld.render(modelBatch, game.getEnvironment());
        
        // Render player - explicitly ensure player is rendered
        if (player != null) {
            player.render(modelBatch, game.getEnvironment());
            logger.debug("Rendering player at position: " + player.getPosition());
        } else {
            logger.error("Player object is null during rendering!");
        }
        
        modelBatch.end();
    }
    
    @Override
    public void resize(int width, int height) {
        logger.info("Screen resized to " + width + "x" + height);
    }
    
    @Override
    public void pause() {
        logger.info("Game screen paused");
    }
    
    @Override
    public void resume() {
        logger.info("Game screen resumed");
    }
    
    @Override
    public void hide() {
        logger.info("Game screen hidden");
    }
    
    @Override
    public void dispose() {
        logger.info("Disposing game screen resources");
        modelBatch.dispose();
        gameWorld.dispose();
        player.dispose();
    }
}