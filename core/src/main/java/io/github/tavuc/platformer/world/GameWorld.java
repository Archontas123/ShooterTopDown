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
    private static final float DEATH_HEIGHT = -50f; // Lower death height to allow for more falling
    
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
        
        // Create platform model - back to original color
        platformModel = modelBuilder.createBox(
            PlatformerGame.TILE_SIZE,
            PlatformerGame.TILE_SIZE / 2, // Keep thicker platform for visibility
            PlatformerGame.TILE_SIZE,
            new Material(ColorAttribute.createDiffuse(new Color(0.38f, 0.38f, 0.7f, 1f))), // Original color
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        
        // Keep some ambient light for visibility
        platformModel.materials.get(0).set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.6f, 1f));
        
        // Disable face culling for the platform model
        platformModel.materials.get(0).set(new com.badlogic.gdx.graphics.g3d.attributes.IntAttribute(
            com.badlogic.gdx.graphics.g3d.attributes.IntAttribute.CullFace, 
            com.badlogic.gdx.graphics.GL20.GL_NONE));
            
        // Create key shard model - small gold sphere with enhanced lighting
        keyShardModel = modelBuilder.createSphere(
            PlatformerGame.TILE_SIZE / 4, 
            PlatformerGame.TILE_SIZE / 4, 
            PlatformerGame.TILE_SIZE / 4, 
            16, 16, // Higher resolution for smoother look
            new Material(ColorAttribute.createDiffuse(Color.GOLD)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        
        // Add ambient light to key shards
        keyShardModel.materials.get(0).set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.7f, 0.2f, 1f));
        
        logger.info("World models created with enhanced visibility");
    }
    
    /**
     * Generates the initial world layout.
     */
    private void generateWorld() {
        // Create a much larger platform for movement testing
        // Let's make it even larger for good visibility
        float platformWidth = PlatformerGame.PLAYER_SIZE * 40;  // Increased from 30
        float platformDepth = PlatformerGame.PLAYER_SIZE * 40;  // Increased from 30
        
        // Convert to tile counts (how many tiles wide and deep)
        int tileWidth = (int)(platformWidth / PlatformerGame.TILE_SIZE);
        int tileDepth = (int)(platformDepth / PlatformerGame.TILE_SIZE);
        
        // Create the main platform
        createPlatform(0, 0, tileWidth, tileDepth);
        
        // Add some key shards to test collection - spread them out more
        addKeyShard(15, 2, 15);
        addKeyShard(-15, 2, -15);
        addKeyShard(15, 2, -15);
        addKeyShard(-15, 2, 15);
        
        logger.info("World generated with a large platform of size " + tileWidth + "x" + tileDepth + " tiles");
    }
    
    /**
     * Creates a platform at the specified position with the given width and depth.
     * 
     * @param x X position (center)
     * @param y Y position (center)
     * @param width Width in tiles
     * @param depth Depth in tiles
     */
    private void createPlatform(float x, float y, int width, int depth) {
        Platform platform = new Platform(x, y, 0, width, depth, platformModel);
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
        
        // Check platform collisions
        boolean onPlatform = false;
        for (Platform platform : platforms) {
            if (platform.isPlayerOnPlatform(playerPos)) {
                onPlatform = true;
                
                // Stop vertical velocity if player is falling onto platform
                if (player.getVelocity().y < 0) {
                    player.getVelocity().y = 0;
                    // Adjust player position to be exactly on top of platform
                    playerPos.y = platform.getTopY() + (PlatformerGame.PLAYER_SIZE / 2);
                    player.setPosition(playerPos);
                }
                break;
            }
        }
        
        // Handle falling if not on any platform
        if (!onPlatform && player.getCurrentState() != Player.AnimationState.JUMPING) {
            // Set to jumping state to indicate player is in the air
            player.setAnimationState(Player.AnimationState.JUMPING);
        }
        
        // Check key shard collisions
        Array<KeyShard> shardsToRemove = new Array<>();
        for (KeyShard shard : keyShards) {
            if (shard.isCollidingWithPlayer(playerPos)) {
                logger.info("Player collected key shard at: " + shard.getPosition());
                shardsToRemove.add(shard);
            }
        }
        
        // Remove collected shards
        keyShards.removeAll(shardsToRemove, true);
    }
    
    /**
     * Renders the world.
     * 
     * @param modelBatch The model batch to render with
     * @param environment The lighting environment
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        // Render platforms
        for (Platform platform : platforms) {
            platform.render(modelBatch, environment);
        }
        
        // Render key shards
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
        return spawnPosition;
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
    
    /**
     * Represents a platform in the game world.
     */
    private static class Platform {
        private final Vector3 position;
        private final float width;
        private final float depth;
        private final ModelInstance modelInstance;
        private final float topY;
        
        /**
         * Creates a new platform.
         * 
         * @param x X position (center)
         * @param y Y position (center)
         * @param z Z position (center)
         * @param widthTiles Width in tiles
         * @param depthTiles Depth in tiles
         * @param model The model to use
         */
        public Platform(float x, float y, float z, int widthTiles, int depthTiles, Model model) {
            this.position = new Vector3(x, y, z);
            this.width = widthTiles * PlatformerGame.TILE_SIZE;
            this.depth = depthTiles * PlatformerGame.TILE_SIZE;
            this.topY = y + (PlatformerGame.TILE_SIZE / 4); // Top of the platform (half thickness)
            
            // Create model instance with proper scaling
            this.modelInstance = new ModelInstance(model);
            
            // Set position to center of platform
            modelInstance.transform.setToTranslation(position);
            
            // Scale based on tile counts (width and depth)
            float scaleX = width / PlatformerGame.TILE_SIZE;
            float scaleZ = depth / PlatformerGame.TILE_SIZE;
            modelInstance.transform.scale(scaleX, 1, scaleZ);
        }
        
        /**
         * Renders the platform.
         * 
         * @param modelBatch The model batch to render with
         * @param environment The lighting environment
         */
        public void render(ModelBatch modelBatch, Environment environment) {
            // Always render the platform, regardless of player position
            modelBatch.render(modelInstance, environment);
        }
        
        /**
         * Checks if the player is on this platform.
         * 
         * @param playerPos The player's position
         * @return True if the player is on this platform
         */
        public boolean isPlayerOnPlatform(Vector3 playerPos) {
            float halfWidth = width / 2;
            float halfDepth = depth / 2;
            float playerHalfSize = PlatformerGame.PLAYER_SIZE / 2;
            
            // Check if player is within platform bounds
            boolean withinX = playerPos.x > position.x - halfWidth - playerHalfSize && 
                              playerPos.x < position.x + halfWidth + playerHalfSize;
            boolean withinZ = playerPos.z > position.z - halfDepth - playerHalfSize && 
                              playerPos.z < position.z + halfDepth + playerHalfSize;
            
            // Check if player is at the right height to be on platform
            boolean atCorrectHeight = playerPos.y - playerHalfSize < topY + 0.2f && 
                                     playerPos.y - playerHalfSize > topY - 0.5f;
            
            return withinX && withinZ && atCorrectHeight;
        }
        
        /**
         * Gets the Y position of the top of the platform.
         * 
         * @return The top Y position
         */
        public float getTopY() {
            return topY;
        }
    }
    
    /**
     * Represents a key shard collectible in the game world.
     */
    private static class KeyShard {
        private final Vector3 position;
        private final ModelInstance modelInstance;
        private float rotationAngle;
        private float bounceOffset;
        private float bounceTime;
        
        /**
         * Creates a new key shard.
         * 
         * @param x X position
         * @param y Y position
         * @param z Z position
         * @param model The model to use
         */
        public KeyShard(float x, float y, float z, Model model) {
            this.position = new Vector3(x, y, z);
            this.modelInstance = new ModelInstance(model);
            this.rotationAngle = 0;
            this.bounceTime = 0;
            this.bounceOffset = 0;
        }
        
        /**
         * Checks if the key shard is colliding with the player.
         * 
         * @param playerPos The player's position
         * @return True if the key shard is colliding with the player
         */
        public boolean isCollidingWithPlayer(Vector3 playerPos) {
            float collisionDistance = 2.0f; // Increased collision distance for easier pickup
            return position.dst(playerPos) < collisionDistance;
        }
        
        /**
         * Gets the position of the key shard.
         * 
         * @return The position
         */
        public Vector3 getPosition() {
            return position;
        }
        
        /**
         * Renders the key shard.
         * 
         * @param modelBatch The model batch to render with
         * @param environment The lighting environment
         */
        public void render(ModelBatch modelBatch, Environment environment) {
            // Update animation
            rotationAngle += 1.5f; // Faster rotation
            bounceTime += 0.05f;
            bounceOffset = (float) Math.sin(bounceTime) * 0.3f; // Gentle hover animation
            
            // Set position with bounce offset
            modelInstance.transform.setToTranslation(
                position.x,
                position.y + bounceOffset,
                position.z
            );
            
            // Apply rotation
            modelInstance.transform.rotate(0, 1, 0, rotationAngle);
            
            // Render the key shard
            modelBatch.render(modelInstance, environment);
        }
    }
}