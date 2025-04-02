package io.github.tavuc.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    
    // Add respawn UI elements
    private BitmapFont font;
    private SpriteBatch batch;
    private Vector3 spawnPosition;
    
    /**
     * Creates a new game screen.
     * 
     * @param game The main game instance
     */
    public GameScreen(PlatformerGame game) {
        this.game = game;
        this.logger = new Logger("GameScreen");
        this.modelBatch = new ModelBatch();
        
        // Initialize UI components for respawn message
        this.font = new BitmapFont();
        this.font.getData().setScale(2f); // Larger text
        this.batch = new SpriteBatch();
        
        gameWorld = new GameWorld(logger);

        spawnPosition = gameWorld.getSpawnPosition();
        spawnPosition.y += 2.5f; 

        initializeWorld();
        
        logger.info("Game screen initialized");
    }
    
    /**
     * Initializes the game world and player.
     */
    private void initializeWorld() {
        

        player = new Player(spawnPosition, logger);
        
        // Initial camera update to focus on player
        updateCamera();
        
        logger.info("World and player initialized at position: " + spawnPosition);
    }
    
    @Override
    public void show() {
        logger.info("Game screen shown");
    }
    
    @Override
    public void render(float delta) {
        // Clear the screen with a better visible color
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        // Configure proper depth testing
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        
        // Disable face culling to ensure all faces are rendered
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        
        // Only handle input if not respawning
        if (!respawning) {
            handleInput(delta);
        }
        
        // Update game logic
        update(delta);
        
        // Render the world
        renderWorld();
        
        // Draw respawn message if respawning
        if (respawning) {
            renderRespawnMessage();
        }
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
    
    // Flag to track respawn status
    private boolean respawning = false;
    private float respawnTimer = 0f;
    private static final float RESPAWN_DELAY = 1.5f; // Delay before respawning
    
    /**
     * Updates the game logic.
     * 
     * @param delta Time since the last frame
     */
    private void update(float delta) {
        // Handle respawning sequence if active
        if (respawning) {
            respawnTimer += delta;
            
            // Wait for delay before actually respawning
            if (respawnTimer >= RESPAWN_DELAY) {
                completeRespawn();
                respawning = false;
                respawnTimer = 0f;
            }
            
            // Skip the rest of the update while respawning
            return;
        }
        
        // Update player
        player.update(delta);
        
        // Check if player has fallen off the map
        Vector3 playerPos = player.getPosition();
        if (playerPos.y < gameWorld.getDeathHeight()) {
            startRespawn();
            // Skip the rest of the update to ensure clean respawn
            return;
        }
        
        // Update camera to follow player
        updateCamera();
        
        // Check for collisions with the world
        gameWorld.checkCollisions(player);
    }
    
    /**
     * Starts the respawn sequence.
     * Freezes player input and prepares for teleportation.
     */
    private void startRespawn() {
        if (!respawning) {
            respawning = true;
            respawnTimer = 0f;
            
            // Freeze player in place
            player.setVelocity(new Vector3(0, 0, 0));
            player.setAnimationState(Player.AnimationState.INVISIBLE);
            
            // Set player to safe position to prevent continuing to fall
            Vector3 safePosition = new Vector3(0, 100, 0); // High above the map
            player.setPosition(safePosition);
            
            logger.info("Player death detected. Starting respawn sequence.");
        }
    }
    
    /**
     * Completes the respawn process after delay.
     * Places player at spawn point and resets state.
     */
    private void completeRespawn() {
        Vector3 spawnPosition = gameWorld.getSpawnPosition();
        spawnPosition.y += 2.5f; // Elevate player on respawn
        
        // Reset the player's state completely
        player.setPosition(spawnPosition);
        player.resetVelocity();
        player.setAnimationState(Player.AnimationState.IDLE);
        
        // Update camera immediately to follow respawned player
        updateCamera();
        
        logger.info("Player respawned at: " + spawnPosition);
    }
    
    // Keep a fixed camera height to prevent jarring during jumps
    private float cameraFixedHeight = 100f;
    
    /**
     * Updates the camera to follow the player.
     */
    private void updateCamera() {
        Vector3 playerPos = player.getPosition();
        
        // Position camera with fixed height and closer view
        game.getCamera().position.set(
            playerPos.x + 50, // Reduced offset for closer view
            cameraFixedHeight, // Fixed height regardless of player's y position
            playerPos.z + 50  // Reduced offset for closer view
        );
        
        // Always look at player's horizontal position but at a fixed height
        // This prevents the camera from moving up and down during jumps
        Vector3 lookTarget = new Vector3(
            playerPos.x,
            0, // Look at ground level for stability
            playerPos.z
        );
        
        game.getCamera().lookAt(lookTarget);
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
        
        // Render world first
        gameWorld.render(modelBatch, game.getEnvironment());
        
        // Render player after world to ensure visibility
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
    
    /**
     * Renders the respawn message when player is respawning.
     */
    private void renderRespawnMessage() {
        batch.begin();
        String message = "Respawning...";
        
        // Calculate time remaining as a percentage
        float percentComplete = respawnTimer / RESPAWN_DELAY;
        
        // Draw progress bar
        int barWidth = 200;
        int barHeight = 20;
        int x = Gdx.graphics.getWidth() / 2 - barWidth / 2;
        int y = Gdx.graphics.getHeight() / 2 - 100;
        
        // Draw text
        font.setColor(1, 1, 1, 1);
        float textWidth = font.draw(batch, message, 
                           x + barWidth / 2 - font.getData().scaleX * message.length() * 5, 
                           y + 50).width;
        
        batch.end();
    }
    
    @Override
    public void dispose() {
        logger.info("Disposing game screen resources");
        modelBatch.dispose();
        gameWorld.dispose();
        player.dispose();
        font.dispose();
        batch.dispose();
    }
}