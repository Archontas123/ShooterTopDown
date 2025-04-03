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
    private final Array<Wall> walls;
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
        this.walls = new Array<>();
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
        
        // Create platform model
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
    // Create a main platform for movement testing
    float platformWidth = PlatformerGame.PLAYER_SIZE * 40;
    float platformDepth = PlatformerGame.PLAYER_SIZE * 40;
    
    // Convert to tile counts (how many tiles wide and deep)
    int tileWidth = (int)(platformWidth / PlatformerGame.TILE_SIZE);
    int tileDepth = (int)(platformDepth / PlatformerGame.TILE_SIZE);
    
    // Create the main platform
    createPlatform(0, 0, tileWidth, tileDepth);
    
    // Create a second elevated platform directly above the main platform
    float platformY = 20f; // Higher than maximum jump height
    int wallPlatformWidth = 20;
    int wallPlatformDepth = 20;
    
    // Create the second platform (centered over the main platform)
    createPlatform(0, platformY, wallPlatformWidth, wallPlatformDepth);
    
    // Wall parameters
    float wallHeight = 25f;
    float wallWidth = 1f;
    
    // Create a wall on the A-side (left side looking from starting position)
    // This will be the primary wall-running wall
    createWall(
        -20f, // To the left (A direction)
        platformY / 2, // Half the height of the platform
        0,
        wallWidth,
        wallHeight,
        wallPlatformDepth
    );
    
    // Create a wall on the W-side (forward looking from starting position)
    // This creates a corner for wall-jumping
    createWall(
        0,
        platformY / 2,
        -20f, // Forward (W direction)
        wallPlatformWidth,
        wallHeight,
        wallWidth
    );
    
    // Create a small connecting wall between the two main walls
    // This helps guide the player and creates more wall-running options
    createWall(
        -15f,
        platformY / 2,
        -15f,
        10f,
        wallHeight,
        wallWidth
    );
    
    // Add stepping platforms for an alternative path
    // First stepping platform
    createPlatform(-15f, 7f, 5, 5);
    
    // Second stepping platform, higher
    createPlatform(-5f, 14f, 5, 5);
    
    // Add some key shards to test collection
    // Main platform shards
    addKeyShard(15, 2, 15);
    addKeyShard(-15, 2, -15);
    
    // Upper platform shards
    addKeyShard(0, platformY + 2, 0);
    
    // Path markers - help guide the player
    addKeyShard(-20f, 10f, 0); // On the wall-running path
    addKeyShard(-15f, 9f, -15f); // Near the corner
    addKeyShard(-15f, platformY - 5f, -15f); // Near the top of the wall
    
    // Stepping platform shards
    addKeyShard(-15f, 9f, 0); // On first stepping platform
    addKeyShard(-5f, 16f, 0); // On second stepping platform
    
    logger.info("World generated with wall-running and wall-jumping course");
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
     * Creates a vertical wall with the specified dimensions.
     *
     * @param x X position (center)
     * @param y Y position (center)
     * @param z Z position (center)
     * @param width Width in tiles
     * @param height Height in tiles
     * @param depth Depth in tiles
     */
    private void createWall(float x, float y, float z, float width, float height, float depth) {
        // Create a model for the wall
        ModelBuilder modelBuilder = new ModelBuilder();
        Model wallModel = modelBuilder.createBox(
            width * PlatformerGame.TILE_SIZE,
            height * PlatformerGame.TILE_SIZE,
            depth * PlatformerGame.TILE_SIZE,
            new Material(ColorAttribute.createDiffuse(new Color(0.6f, 0.4f, 0.2f, 1f))), // Brown wall color
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        
        // Add ambient light for visibility
        wallModel.materials.get(0).set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.3f, 0.2f, 1f));
        
        // Create a wall instance
        Wall wall = new Wall(x, y, z, width, height, depth, wallModel);
        walls.add(wall);
        
        logger.info("Created wall at position: (" + x + ", " + y + ", " + z + ") with dimensions: " + 
                width + "x" + height + "x" + depth);
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
        
        // Check wall collisions
        boolean againstWall = false;
        Wall collidingWall = null;
        int wallDirection = 0; // 0 = none, 1 = left, 2 = right, 3 = front, 4 = back
        
        for (Wall wall : walls) {
            int direction = wall.isPlayerAgainstWall(playerPos, player.getVelocity());
            if (direction > 0) {
                againstWall = true;
                collidingWall = wall;
                wallDirection = direction;
                break;
            }
        }
        
        // Update player's wall state
        player.setAgainstWall(againstWall, wallDirection, collidingWall != null ? collidingWall.getNormal() : null);
        
        // Handle falling if not on any platform
        if (!onPlatform && player.getCurrentState() != Player.AnimationState.JUMPING 
                && player.getCurrentState() != Player.AnimationState.WALL_SLIDING) {
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
        
        // Render walls
        for (Wall wall : walls) {
            wall.render(modelBatch, environment);
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
        // Return a new Vector3 to avoid reference modification
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
        
        // Dispose of all wall models
        for (Wall wall : walls) {
            wall.disposeModel();
        }
        
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
 * Represents a wall in the game world.
 */
private static class Wall {
    private final Vector3 position;
    private final Vector3 normal = new Vector3(); // Normal to the wall's surface
    private final float width;
    private final float height;
    private final float depth;
    private final ModelInstance modelInstance;
    private final Model model;
    private final float halfWidth;
    private final float halfHeight;
    private final float halfDepth;
    
    /**
     * Creates a new wall.
     *
     * @param x X position (center)
     * @param y Y position (center)
     * @param z Z position (center)
     * @param width Width in tiles
     * @param height Height in tiles
     * @param depth Depth in tiles
     * @param model The model to use
     */
    public Wall(float x, float y, float z, float width, float height, float depth, Model model) {
        this.position = new Vector3(x, y, z);
        this.width = width * PlatformerGame.TILE_SIZE;
        this.height = height * PlatformerGame.TILE_SIZE;
        this.depth = depth * PlatformerGame.TILE_SIZE;
        this.model = model;
        
        this.halfWidth = this.width / 2;
        this.halfHeight = this.height / 2;
        this.halfDepth = this.depth / 2;
        
        // Determine wall normal based on dimensions (will point outward from thinnest dimension)
        if (width < depth && width < height) {
            // Wall is thin in X direction (left/right wall)
            this.normal.set(1, 0, 0); // Assume right-facing, will be flipped if needed
        } else if (depth < width && depth < height) {
            // Wall is thin in Z direction (front/back wall)
            this.normal.set(0, 0, 1); // Assume front-facing, will be flipped if needed
        } else {
            // Wall is thin in Y direction (floor/ceiling) or all equal
            this.normal.set(0, 1, 0); // Assume upward-facing
        }
        
        // Create model instance with proper positioning
        this.modelInstance = new ModelInstance(model);
        modelInstance.transform.setToTranslation(position);
    }
    
    /**
     * Gets the normal vector of the wall.
     *
     * @return The wall's normal vector
     */
    public Vector3 getNormal() {
        return normal;
    }
    
    /**
     * Renders the wall.
     *
     * @param modelBatch The model batch to render with
     * @param environment The lighting environment
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(modelInstance, environment);
    }
    
    /**
     * Checks if the player is against this wall.
     *
     * @param playerPos The player's position
     * @param playerVel The player's velocity
     * @return Direction of collision: 0=none, 1=left, 2=right, 3=front, 4=back
     */
    public int isPlayerAgainstWall(Vector3 playerPos, Vector3 playerVel) {
        float playerHalfSize = PlatformerGame.PLAYER_SIZE / 2;
        float wallCheckDistance = playerHalfSize + 0.2f; // Slightly larger than exact contact
        
        // Check if player is within the wall's height bounds
        boolean withinY = playerPos.y > position.y - halfHeight - playerHalfSize &&
                          playerPos.y < position.y + halfHeight + playerHalfSize;
        
        // Check X-axis collision (left/right walls)
        if (width < depth) { // This is a wall thin in X direction
            boolean withinZ = playerPos.z > position.z - halfDepth - playerHalfSize &&
                              playerPos.z < position.z + halfDepth + playerHalfSize;
            
            if (withinY && withinZ) {
                // Check left side of wall
                if (Math.abs(playerPos.x - (position.x - halfWidth)) <= wallCheckDistance) {
                    normal.set(-1, 0, 0); // Normal pointing left
                    return 1; // Left side collision
                }
                // Check right side of wall
                if (Math.abs(playerPos.x - (position.x + halfWidth)) <= wallCheckDistance) {
                    normal.set(1, 0, 0); // Normal pointing right
                    return 2; // Right side collision
                }
            }
        }
        // Check Z-axis collision (front/back walls)
        else if (depth < width) { // This is a wall thin in Z direction
            boolean withinX = playerPos.x > position.x - halfWidth - playerHalfSize &&
                              playerPos.x < position.x + halfWidth + playerHalfSize;
            
            if (withinY && withinX) {
                // Check front side of wall
                if (Math.abs(playerPos.z - (position.z - halfDepth)) <= wallCheckDistance) {
                    normal.set(0, 0, -1); // Normal pointing front
                    return 3; // Front side collision
                }
                // Check back side of wall
                if (Math.abs(playerPos.z - (position.z + halfDepth)) <= wallCheckDistance) {
                    normal.set(0, 0, 1); // Normal pointing back
                    return 4; // Back side collision
                }
            }
        }
        
        return 0; // No collision
    }
    
    /**
     * Checks for penetration collision to prevent going through walls.
     * This is a more strict check than isPlayerAgainstWall.
     * 
     * @param playerPos The player's position
     * @return Direction of collision: 0=none, 1=left, 2=right, 3=front, 4=back
     */
    public int isPlayerPenetratingWall(Vector3 playerPos) {
        float playerHalfSize = PlatformerGame.PLAYER_SIZE / 2;
        
        // Simple AABB collision check
        boolean withinX = playerPos.x + playerHalfSize > position.x - halfWidth && 
                          playerPos.x - playerHalfSize < position.x + halfWidth;
        boolean withinY = playerPos.y + playerHalfSize > position.y - halfHeight && 
                          playerPos.y - playerHalfSize < position.y + halfHeight;
        boolean withinZ = playerPos.z + playerHalfSize > position.z - halfDepth && 
                          playerPos.z - playerHalfSize < position.z + halfDepth;
        
        // If we're inside the wall's bounds, we need to push out
        if (withinX && withinY && withinZ) {
            // Determine which side has the least penetration
            float penetrationX1 = playerPos.x + playerHalfSize - (position.x - halfWidth); // Penetration from left
            float penetrationX2 = (position.x + halfWidth) - (playerPos.x - playerHalfSize); // Penetration from right
            float penetrationZ1 = playerPos.z + playerHalfSize - (position.z - halfDepth); // Penetration from front
            float penetrationZ2 = (position.z + halfDepth) - (playerPos.z - playerHalfSize); // Penetration from back
            
            // Find the smallest penetration
            float minPenetration = Math.min(Math.min(penetrationX1, penetrationX2), Math.min(penetrationZ1, penetrationZ2));
            
            if (minPenetration == penetrationX1) {
                normal.set(-1, 0, 0); // Push left
                return 1;
            } else if (minPenetration == penetrationX2) {
                normal.set(1, 0, 0); // Push right
                return 2;
            } else if (minPenetration == penetrationZ1) {
                normal.set(0, 0, -1); // Push forward
                return 3;
            } else if (minPenetration == penetrationZ2) {
                normal.set(0, 0, 1); // Push backward
                return 4;
            }
        }
        
        return 0; // No penetration
    }
    
    /**
     * Calculates the position correction needed to prevent wall clipping.
     *
     * @param playerPos The player's position
     * @param direction The collision direction (1=left, 2=right, 3=front, 4=back)
     * @return The correction vector to apply to the player
     */
    public Vector3 getCollisionCorrection(Vector3 playerPos, int direction) {
        float playerHalfSize = PlatformerGame.PLAYER_SIZE / 2;
        Vector3 correction = new Vector3();
        
        switch (direction) {
            case 1: // Left wall
                // Push player left, away from wall
                correction.x = (position.x - halfWidth) - (playerPos.x + playerHalfSize) - 0.1f;
                break;
            case 2: // Right wall
                // Push player right, away from wall
                correction.x = (position.x + halfWidth) - (playerPos.x - playerHalfSize) + 0.1f;
                break;
            case 3: // Front wall
                // Push player forward, away from wall
                correction.z = (position.z - halfDepth) - (playerPos.z + playerHalfSize) - 0.1f;
                break;
            case 4: // Back wall
                // Push player backward, away from wall
                correction.z = (position.z + halfDepth) - (playerPos.z - playerHalfSize) + 0.1f;
                break;
            default:
                return null;
        }
        
        return correction;
    }
    
    /**
     * Disposes of the wall's model.
     */
    public void disposeModel() {
        if (model != null) {
            model.dispose();
        }
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