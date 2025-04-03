package io.github.tavuc.platformer.world;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

/**
 * Represents a key shard collectible in the game world.
 */
public class KeyShard {
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
        float collisionDistance = 4.0f; 
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
        rotationAngle += 1.5f; 
        bounceTime += 0.05f;
        bounceOffset = (float) Math.sin(bounceTime) * 0.3f; 
        
        modelInstance.transform.setToTranslation(
            position.x,
            position.y + bounceOffset,
            position.z
        );
        
        modelInstance.transform.rotate(0, 1, 0, rotationAngle);
        
        modelBatch.render(modelInstance, environment);
    }
}