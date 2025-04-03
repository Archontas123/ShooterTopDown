package io.github.tavuc.platformer.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import io.github.tavuc.platformer.PlatformerGame;
import io.github.tavuc.platformer.entities.Player;
import io.github.tavuc.platformer.utils.Logger;

/**
 * Manages the game world including platforms, obstacles, and collectibles.
 * Handles collision detection and world generation.
 */
public class GameWorld implements Disposable {
    private static final float DEATH_HEIGHT = -50f; 
    
    private final Logger logger;
    private final Array<Platform> platforms;
    private final Array<KeyShard> keyShards;
    private final Vector3 spawnPosition;
    
    private Model platformModel;
    private Model keyShardModel;
    
    /**
     * Creates a new game world.
     * 
     * @param logger The logger instance
     */
    public GameWorld(Logger logger) {
        this.logger = logger;
        this.platforms = new Array<>();
        this.keyShards = new Array<>();
        this.spawnPosition = new Vector3(0, 5, 0);
        
        createModels();
        generateWorld();
        
        logger.info("Game world initialized");
    }
    
    /**
     * Creates the models used in the world.
     */
    private void createModels() {
        ModelBuilder modelBuilder = new ModelBuilder();
        
        platformModel = modelBuilder.createBox(
            PlatformerGame.TILE_SIZE,
            PlatformerGame.TILE_SIZE / 2, 
            PlatformerGame.TILE_SIZE,
            new Material(ColorAttribute.createDiffuse(new Color(0.38f, 0.38f, 0.7f, 1f))), 
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        
        platformModel.materials.get(0).set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.6f, 1f));
        
        platformModel.materials.get(0).set(new com.badlogic.gdx.graphics.g3d.attributes.IntAttribute(
            com.badlogic.gdx.graphics.g3d.attributes.IntAttribute.CullFace, 
            com.badlogic.gdx.graphics.GL20.GL_NONE));
            
        keyShardModel = modelBuilder.createSphere(
            PlatformerGame.TILE_SIZE / 4, 
            PlatformerGame.TILE_SIZE / 4, 
            PlatformerGame.TILE_SIZE / 4, 
            16, 16, 
            new Material(ColorAttribute.createDiffuse(Color.GOLD)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        
        keyShardModel.materials.get(0).set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.7f, 0.2f, 1f));
        
        logger.info("World models created with enhanced visibility");
    }
    
    /**
     * Generates the initial world layout.
     */
    private void generateWorld() {
        float platformWidth = PlatformerGame.PLAYER_SIZE * 40;
        float platformDepth = PlatformerGame.PLAYER_SIZE * 40;
        
        int tileWidth = (int)(platformWidth / PlatformerGame.TILE_SIZE);
        int tileDepth = (int)(platformDepth / PlatformerGame.TILE_SIZE);
        
        createPlatform(0, 0, 0, tileWidth, tileDepth);
        createPlatform(0, 10, 0, tileWidth/2, tileDepth/2);

        float platformY = 20f; 
     
        
      
        logger.info("World generated");
    }
    
    /**
     * Creates a platform at the specified position with the given width and depth.
     * 
     * @param x X position (center)
     * @param y Y position (center)
     * @param z Z position (center)
     * @param width Width in tiles
     * @param depth Depth in tiles
     */
    private void createPlatform(float x, float y, float z, int width, int depth) {
        Platform platform = new Platform(x, y, z, width, depth, platformModel);
        platforms.add(platform);
    }
    
  
    /**
     * Adds a key shard at the specified position.
     * 
     * @param x X position
     * @param y Y position
     * @param z Z position
     */
    private void addKeyShard(float x, float y, float z) {
        KeyShard keyShard = new KeyShard(x, y, z, keyShardModel);
        keyShards.add(keyShard);
    }
    
    /**
     * Checks for collisions between the player and world elements.
     * 
     * @param player The player to check collisions for
     */
    public void checkCollisions(Player player) {
        Vector3 playerPos = player.getPosition();
        
        boolean onPlatform = false;
        for (Platform platform : platforms) {
            Platform.CollisionType collision = platform.checkCollision(playerPos);
            if (collision != Platform.CollisionType.NONE) {
                switch (collision) {
                    case TOP:
                        if (player.getVelocity().y < 0) {
                            onPlatform = true;
                            player.getVelocity().y = 0;
                            playerPos.y = platform.getTopY() + (PlatformerGame.PLAYER_SIZE / 2);
                            player.setPosition(playerPos);
                        }
                        break;
                    case BOTTOM:
                        if (player.getVelocity().y > 0) {
                            player.getVelocity().y = 0;
                            playerPos.y = platform.getBottomY() - (PlatformerGame.PLAYER_SIZE / 2);
                            player.setPosition(playerPos);
                        }
                        break;
                    case SIDE:
                        Vector3 correction = getSideCollisionCorrection(playerPos, platform);
                        if (correction != null) {
                            playerPos.add(correction);
                            player.setPosition(playerPos);
                        }
                        break;
                }
                break;
            }
        }
        
    
 
        
        if (!onPlatform && player.getCurrentState() != Player.AnimationState.JUMPING) {
            player.setAnimationState(Player.AnimationState.JUMPING);
        }
        
        Array<KeyShard> shardsToRemove = new Array<>();
        for (KeyShard shard : keyShards) {
            if (shard.isCollidingWithPlayer(playerPos)) {
                logger.info("Player collected key shard at: " + shard.getPosition());
                shardsToRemove.add(shard);
            }
        }
        
        keyShards.removeAll(shardsToRemove, true);
    }
    
    /**
     * Moves the player after the side collision
     * 
     * @param playerPos the position of the player
     * @param platform the platform the player is colliding with 
     */
    private Vector3 getSideCollisionCorrection(Vector3 playerPos, Platform platform) {
        Vector3 correction = new Vector3();
        float playerRadius = PlatformerGame.PLAYER_SIZE / 2;
        
        float platformLeft = platform.getPosition().x - (platform.getWidth() / 2);
        float platformRight = platform.getPosition().x + (platform.getWidth() / 2);
        float platformFront = platform.getPosition().z - (platform.getDepth() / 2);
        float platformBack = platform.getPosition().z + (platform.getDepth() / 2);
        
        float leftCorr = Math.abs(playerPos.x - (platformLeft - playerRadius));
        float rightCorr = Math.abs(playerPos.x - (platformRight + playerRadius));
        float frontCorr = Math.abs(playerPos.z - (platformFront - playerRadius));
        float backCorr = Math.abs(playerPos.z - (platformBack + playerRadius));
        
        float minCorr = Math.min(Math.min(leftCorr, rightCorr), Math.min(frontCorr, backCorr));
        if (minCorr == leftCorr) {
            correction.x = -(playerPos.x - (platformLeft - playerRadius));
        } else if (minCorr == rightCorr) {
            correction.x = platformRight + playerRadius - playerPos.x;
        } else if (minCorr == frontCorr) {
            correction.z = -(playerPos.z - (platformFront - playerRadius));
        } else {
            correction.z = platformBack + playerRadius - playerPos.z;
        }
        
        return correction;
    }
    
    /**
     * Renders the world.
     * 
     * @param modelBatch The model batch to render with
     * @param environment The lighting environment
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        for (Platform platform : platforms) {
            platform.render(modelBatch, environment);
        }
        

        
        for (KeyShard shard : keyShards) {
            shard.render(modelBatch, environment);
        }
    }
    
    /**
     * Gets the spawn position.
     * 
     * @return The spawn position
     */
    public Vector3 getSpawnPosition() {
        return new Vector3(spawnPosition);
    }
    
    /**
     * Gets the death height (level at which player falls to death).
     * 
     * @return The death height
     */
    public float getDeathHeight() {
        return DEATH_HEIGHT;
    }
    
    /**
     * Disposes of resources used by the world.
     */
    @Override
    public void dispose() {
        platformModel.dispose();
        keyShardModel.dispose();
        

        
        logger.info("Game world disposed");
    }
}