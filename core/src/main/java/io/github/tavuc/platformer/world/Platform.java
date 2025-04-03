package io.github.tavuc.platformer.world;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import io.github.tavuc.platformer.PlatformerGame;

/**
* Represents a platform in the game world.
*/
public class Platform {
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
        this.topY = y + (PlatformerGame.TILE_SIZE / 4); 
        
        this.modelInstance = new ModelInstance(model);
        
        modelInstance.transform.setToTranslation(position);
        
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
        
        boolean withinX = playerPos.x > position.x - halfWidth - playerHalfSize && 
                            playerPos.x < position.x + halfWidth + playerHalfSize;
        boolean withinZ = playerPos.z > position.z - halfDepth - playerHalfSize && 
                            playerPos.z < position.z + halfDepth + playerHalfSize;
        
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


    public float getBottomY() {
        return position.y - (PlatformerGame.TILE_SIZE / 4); 
    }

    public float getWidth() {
        return width;
    }
    
    public float getDepth() {
        return depth;
    }
    
    public Vector3 getPosition() {
        return position;
    }

    /**
     * Checks for collision between player and platform from any direction
     * 
     * @param playerPos The player's position
     * @return CollisionType indicating the type of collision (NONE, TOP, BOTTOM, SIDE)
     */
    public CollisionType checkCollision(Vector3 playerPos) {
        float halfWidth = width / 2;
        float halfDepth = depth / 2;
        float halfHeight = PlatformerGame.TILE_SIZE / 4; 
        float playerHalfSize = PlatformerGame.PLAYER_SIZE / 2;

        boolean withinX = playerPos.x > position.x - halfWidth - playerHalfSize && 
                         playerPos.x < position.x + halfWidth + playerHalfSize;
        boolean withinZ = playerPos.z > position.z - halfDepth - playerHalfSize && 
                         playerPos.z < position.z + halfDepth + playerHalfSize;

        if (!withinX || !withinZ) {
            return CollisionType.NONE;
        }

        float playerBottom = playerPos.y - playerHalfSize;
        float playerTop = playerPos.y + playerHalfSize;
        float platformBottom = position.y - halfHeight;
        float platformTop = position.y + halfHeight;

        if (playerBottom <= platformTop && playerTop >= platformTop) {
            return CollisionType.TOP;
        } else if (playerTop >= platformBottom && playerBottom <= platformBottom) {
            return CollisionType.BOTTOM;
        } else if (playerBottom > platformBottom && playerTop < platformTop) {
            return CollisionType.SIDE;
        }

        return CollisionType.NONE;
    }

    public enum CollisionType {
        NONE,
        TOP,
        BOTTOM,
        SIDE
    }
}