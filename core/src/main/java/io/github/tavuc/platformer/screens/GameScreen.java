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
import io.github.tavuc.platformer.effects.EffectsManager;

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
    
    private EffectsManager effectsManager;
    
    private BitmapFont font;
    private SpriteBatch batch;
    
    /**
     * Creates a new game screen.
     * 
     * @param game The main game instance
     */
    public GameScreen(PlatformerGame game) {
        this.game = game;
        this.logger = new Logger("GameScreen");
        this.modelBatch = new ModelBatch();
        
        this.effectsManager = new EffectsManager(logger);
        
        this.font = new BitmapFont();
        this.font.getData().setScale(2f); 
        this.batch = new SpriteBatch();
        
        initializeWorld();
        
        logger.info("Game screen initialized");
    }
    
    /**
     * Initializes the game world and player.
     */
    private void initializeWorld() {
        gameWorld = new GameWorld(logger);
        
        Vector3 spawnPosition = gameWorld.getSpawnPosition();
        spawnPosition.y += 2.5f; 
        
        player = new Player(spawnPosition, logger, effectsManager);
        
        updateCamera();
        
        logger.info("World and player initialized at position: " + spawnPosition);
    }
    
    @Override
    public void show() {
        logger.info("Game screen shown");
    }
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        
        if (!respawning) {
            handleInput(delta);
        }
        
        update(delta);
        
        renderWorld();
        
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
        boolean wPressed = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean sPressed = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean aPressed = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean dPressed = Gdx.input.isKeyPressed(Input.Keys.D);
        
        player.setMovementFlags(wPressed, sPressed, aPressed, dPressed);
        
        boolean moved = false;
        
        if (sPressed) {
            player.move(0, 1, delta);
            moved = true;
        }
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
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            player.jump();
        }
        
        if (moved) {
            player.setAnimationState(Player.AnimationState.WALKING);
        } else {
            player.setAnimationState(Player.AnimationState.IDLE);
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
            float dirX = 0;
            float dirY = 0;
            
            if (sPressed) dirY = 1;
            if (wPressed) dirY = -1;
            if (aPressed) dirX = -1;
            if (dPressed) dirX = 1;
            
            if (dirX == 0 && dirY == 0) dirY = 1;
            
            player.dash(dirX, dirY);
        }
    }
    
    // Flag to track respawn status
    private boolean respawning = false;
    private float respawnTimer = 0f;
    private static final float RESPAWN_DELAY = 1.5f; 
    
    /**
     * Updates the game logic.
     * 
     * @param delta Time since the last frame
     */
    private void update(float delta) {
        if (respawning) {
            respawnTimer += delta;
            
            if (respawnTimer >= RESPAWN_DELAY) {
                completeRespawn();
                respawning = false;
                respawnTimer = 0f;
            }
            
            return;
        }
        
        player.update(delta);
        
        effectsManager.update(delta);
        
        Vector3 playerPos = player.getPosition();
        if (playerPos.y < gameWorld.getDeathHeight()) {
            startRespawn();
            return;
        }
        
        updateCamera();
        
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
            
            player.setVelocity(new Vector3(0, 0, 0));
            player.setAnimationState(Player.AnimationState.INVISIBLE);
            
            Vector3 safePosition = new Vector3(0, 100, 0); 
            player.setPosition(safePosition);
            
            logger.info("Player death detected. Starting respawn sequence.");
        }
    }
    
    /**
     * Completes the respawn process after delay.
     * Places player at spawn point and resets state.
     */
    private void completeRespawn() {
        Vector3 spawnPosition = new Vector3(gameWorld.getSpawnPosition());
        spawnPosition.y += 2.5f; 
        
        player.setPosition(new Vector3(spawnPosition));
        player.resetVelocity();
        player.setAnimationState(Player.AnimationState.IDLE);
        
        updateCamera();
        
        logger.info("Player respawned at: " + spawnPosition);
    }
    
    private float cameraFixedHeight = 100f;
    
    /**
     * Updates the camera to follow the player.
     */
    private void updateCamera() {
        Vector3 playerPos = player.getPosition();
        
        game.getCamera().position.set(
            playerPos.x + 50, 
            cameraFixedHeight,
            playerPos.z + 50  
        );
        
  
        Vector3 lookTarget = new Vector3(
            playerPos.x,
            0, 
            playerPos.z
        );
        
        game.getCamera().lookAt(lookTarget);
        game.getCamera().update();
    }
    
    /**
     * Renders the game world.
     */
    private void renderWorld() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        
        modelBatch.begin(game.getCamera());
        
        gameWorld.render(modelBatch, game.getEnvironment());
        
        if (player != null) {
            player.render(modelBatch, game.getEnvironment());
            logger.debug("Rendering player at position: " + player.getPosition());
        } else {
            logger.error("Player object is null during rendering!");
        }
        
        effectsManager.render(modelBatch, game.getEnvironment());
        
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
        
        float percentComplete = respawnTimer / RESPAWN_DELAY;
        
        int barWidth = 200;
        int barHeight = 20;
        int x = Gdx.graphics.getWidth() / 2 - barWidth / 2;
        int y = Gdx.graphics.getHeight() / 2 - 100;
        
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
        effectsManager.dispose();
        font.dispose();
        batch.dispose();
    }
}