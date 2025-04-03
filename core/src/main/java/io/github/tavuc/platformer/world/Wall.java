package io.github.tavuc.platformer.world;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import io.github.tavuc.platformer.PlatformerGame;

/**
 * Represents a wall in the game world.
 */
public class Wall {
    private final Vector3 position;
    private final Vector3 normal = new Vector3(); 
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
     * Gets the position of the wall.
     *
     * @return The wall's position
     */
    public Vector3 getPosition() {
        return position;
    }

    /**
     * Gets the half-width of the wall.
     *
     * @return The wall's half-width
     */
    public float getHalfWidth() {
        return halfWidth;
    }

    /**
     * Gets the half-height of the wall.
     *
     * @return The wall's half-height
     */
    public float getHalfHeight() {
        return halfHeight;
    }

    /**
     * Gets the half-depth of the wall.
     *
     * @return The wall's half-depth
     */
    public float getHalfDepth() {
        return halfDepth;
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
     * This is used for wall sliding and wall running mechanics.
     *
     * @param playerPos The player's position
     * @param playerVel The player's velocity
     * @return Direction of collision: 0=none, 1=left, 2=right, 3=front, 4=back
     */
    public int isPlayerAgainstWall(Vector3 playerPos, Vector3 playerVel) {
        float playerHalfSize = PlatformerGame.PLAYER_SIZE / 2;
        float wallCheckDistance = playerHalfSize + 0.3f; // Increased distance for easier wall detection
        
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
        
        // Create a slightly smaller collision area to prevent triggering during wall sliding
        float collisionBuffer = 0.1f;
        float adjustedHalfSize = playerHalfSize - collisionBuffer;
        
        // Simple AABB collision check with adjusted size
        boolean withinX = playerPos.x + adjustedHalfSize > position.x - halfWidth && 
                          playerPos.x - adjustedHalfSize < position.x + halfWidth;
        boolean withinY = playerPos.y + adjustedHalfSize > position.y - halfHeight && 
                          playerPos.y - adjustedHalfSize < position.y + halfHeight;
        boolean withinZ = playerPos.z + adjustedHalfSize > position.z - halfDepth && 
                          playerPos.z - adjustedHalfSize < position.z + halfDepth;
        
        // If we're inside the wall's bounds, we need to push out
        if (withinX && withinY && withinZ) {
            // Determine which side has the least penetration
            float penetrationX1 = playerPos.x + adjustedHalfSize - (position.x - halfWidth); // Penetration from left
            float penetrationX2 = (position.x + halfWidth) - (playerPos.x - adjustedHalfSize); // Penetration from right
            float penetrationZ1 = playerPos.z + adjustedHalfSize - (position.z - halfDepth); // Penetration from front
            float penetrationZ2 = (position.z + halfDepth) - (playerPos.z - adjustedHalfSize); // Penetration from back
            
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
        
        // Add a small buffer to prevent getting stuck on walls
        float buffer = 0.2f;
        
        switch (direction) {
            case 1: // Left wall
                // Push player left, away from wall
                correction.x = (position.x - halfWidth) - (playerPos.x + playerHalfSize) - buffer;
                break;
            case 2: // Right wall
                // Push player right, away from wall
                correction.x = (position.x + halfWidth) - (playerPos.x - playerHalfSize) + buffer;
                break;
            case 3: // Front wall
                // Push player forward, away from wall
                correction.z = (position.z - halfDepth) - (playerPos.z + playerHalfSize) - buffer;
                break;
            case 4: // Back wall
                // Push player backward, away from wall
                correction.z = (position.z + halfDepth) - (playerPos.z - playerHalfSize) + buffer;
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