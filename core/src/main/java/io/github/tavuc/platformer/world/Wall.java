package io.github.tavuc.platformer.world;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

/**
 * Represents a wall in the game world.
 */
public class Wall {
    private final Vector3 position;
    private final float width;
    private final float height;
    private final ModelInstance modelInstance;
    private final Vector3 normal;

    /**
     * Creates a new wall.
     *
     * @param x X position of the wall center
     * @param y Y position of the wall center
     * @param z Z position of the wall center
     * @param width The width of the wall
     * @param height The height of the wall
     * @param normal The normal vector of the wall
     * @param model The model to use for the wall
     */
    public Wall(float x, float y, float z, float width, float height, Vector3 normal, Model model) {
        this.position = new Vector3(x, y, z);
        this.width = width;
        this.height = height;
        this.normal = normal.nor();
        this.modelInstance = new ModelInstance(model);
        modelInstance.transform.setToTranslation(position);
        modelInstance.transform.scale(width, height, 0.2f);
        if (Math.abs(this.normal.x) > Math.abs(this.normal.z)) {
            modelInstance.transform.rotate(0, 1, 0, 90);
        }
    }

    /**
     * Renders the wall.
     *
     * @param modelBatch The model batch to render with.
     * @param environment The lighting environment.
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(modelInstance, environment);
    }

    /**
     * Checks if the player is colliding with the wall.
     *
     * @param playerPos The player's position.
     * @param playerSize The approximate radius of the player.
     * @return True if colliding.
     */
    public boolean isCollidingWithPlayer(Vector3 playerPos, float playerSize) {
        float distance = playerPos.cpy().sub(position).dot(normal);
        return Math.abs(distance) < playerSize;
    }

    /**
     * Gets the wall's normal vector.
     *
     * @return The normal vector.
     */
    public Vector3 getNormal() {
        return normal;
    }

    /**
     * Gets the wall's position.
     * 
     * @return The position.
     */

     public Vector3 getPosition() {
        return position;
     }
}
