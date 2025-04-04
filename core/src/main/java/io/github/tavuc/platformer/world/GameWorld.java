package io.github.tavuc.platformer.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.GL20;
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
 * Manages the game world including platforms, walls, obstacles, and collectibles.
 */
public class GameWorld implements Disposable {
    private static final float DEATH_HEIGHT = -50f;
    private static final float WALL_RUNNING_MARGIN = 1.0f; // extra margin added to the player's hitbox for wall collisions

    private final Logger logger;
    private final Array<Platform> platforms;
    private final Array<KeyShard> keyShards;
    private final Array<Wall> walls;
    private final Vector3 spawnPosition;
    
    private Model platformModel;
    private Model keyShardModel;
    private Model wallModel;
    
    /**
     * Creates a new game world.
     *
     * @param logger The logger instance.
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
        platformModel = modelBuilder.createBox(
            PlatformerGame.TILE_SIZE,
            PlatformerGame.TILE_SIZE / 2, 
            PlatformerGame.TILE_SIZE,
            new Material(ColorAttribute.createDiffuse(new Color(0.38f, 0.38f, 0.7f, 1f))),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        platformModel.materials.get(0).set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.6f, 1f));
        platformModel.materials.get(0).set(new com.badlogic.gdx.graphics.g3d.attributes.IntAttribute(
            com.badlogic.gdx.graphics.g3d.attributes.IntAttribute.CullFace, GL20.GL_NONE));
            
        keyShardModel = modelBuilder.createSphere(
            PlatformerGame.TILE_SIZE / 4,
            PlatformerGame.TILE_SIZE / 4,
            PlatformerGame.TILE_SIZE / 4,
            16, 16,
            new Material(ColorAttribute.createDiffuse(Color.GOLD)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        keyShardModel.materials.get(0).set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.7f, 0.2f, 1f));
        
        wallModel = modelBuilder.createBox(
            1f, 1f, 0.2f,
            new Material(ColorAttribute.createDiffuse(new Color(0.5f, 0.5f, 0.5f, 1f))),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        
        logger.info("World models created");
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
        createPlatform(0, 10, 0, tileWidth / 2, tileDepth / 2);
        createWall(0, 5, -20, 200, 200, new Vector3(0, 0, 1));
        
        logger.info("World generated");
    }
    
    /**
     * Creates a platform.
     *
     * @param x X position.
     * @param y Y position.
     * @param z Z position.
     * @param width Width in tiles.
     * @param depth Depth in tiles.
     */
    private void createPlatform(float x, float y, float z, int width, int depth) {
        Platform platform = new Platform(x, y, z, width, depth, platformModel);
        platforms.add(platform);
    }
    
    /**
     * Creates a wall.
     *
     * @param x X position.
     * @param y Y position.
     * @param z Z position.
     * @param width Width of the wall.
     * @param height Height of the wall.
     * @param normal Normal vector of the wall.
     */
    private void createWall(float x, float y, float z, float width, float height, Vector3 normal) {
        Wall wall = new Wall(x, y, z, width, height, normal, wallModel);
        walls.add(wall);
    }
    
    /**
     * Adds a key shard.
     *
     * @param x X position.
     * @param y Y position.
     * @param z Z position.
     */
    private void addKeyShard(float x, float y, float z) {
        KeyShard keyShard = new KeyShard(x, y, z, keyShardModel);
        keyShards.add(keyShard);
    }
    
    /**
     * Checks for collisions between the player and world elements.
     *
     * @param player The player.
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
                    case NONE:
                        break;
                }
                break;
            }
        }
        if (!onPlatform) {
            boolean wallCollision = false;
            float extendedRadius = (PlatformerGame.PLAYER_SIZE / 2) + WALL_RUNNING_MARGIN;
            for (Wall wall : walls) {
                if (wall.isCollidingWithPlayer(playerPos, extendedRadius)) {
                    // Only change to wall sliding if not jumping
                    if (player.getCurrentState() != Player.AnimationState.JUMPING) {
                        if (player.getCurrentState() != Player.AnimationState.WALL_SLIDING) {
                            player.setAnimationState(Player.AnimationState.WALL_SLIDING);
                            //logger.info("Player state set to WALL_SLIDING");
                        }
                    }
                    float distance = playerPos.cpy().sub(wall.getPosition()).dot(wall.getNormal());
                    float penetration = (PlatformerGame.PLAYER_SIZE / 2) - Math.abs(distance);
                    Vector3 correctionVec = new Vector3(wall.getNormal());
                    if (distance < 0) {
                        correctionVec.scl(-penetration);
                    } else {
                        correctionVec.scl(penetration);
                    }
                    playerPos.add(correctionVec);
                    player.setPosition(playerPos);
                    wallCollision = true;
                    break;
                }
            }
            if (!wallCollision && player.getCurrentState() != Player.AnimationState.JUMPING) {
                player.setAnimationState(Player.AnimationState.JUMPING);
            }
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
     * Gets the side collision correction vector.
     *
     * @param playerPos The player's position.
     * @param platform The platform.
     * @return Correction vector.
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
     * @param modelBatch The model batch.
     * @param environment The lighting environment.
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        for (Platform platform : platforms) {
            platform.render(modelBatch, environment);
        }
        for (Wall wall : walls) {
            wall.render(modelBatch, environment);
        }
        for (KeyShard shard : keyShards) {
            shard.render(modelBatch, environment);
        }
    }
    
    /**
     * Gets the spawn position.
     *
     * @return The spawn position.
     */
    public Vector3 getSpawnPosition() {
        return new Vector3(spawnPosition);
    }
    
    /**
     * Gets the death height.
     *
     * @return The death height.
     */
    public float getDeathHeight() {
        return DEATH_HEIGHT;
    }
    
    /**
     * Disposes resources.
     */
    @Override
    public void dispose() {
        platformModel.dispose();
        keyShardModel.dispose();
        wallModel.dispose();
        logger.info("Game world disposed");
    }
}
