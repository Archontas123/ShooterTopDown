package io.github.tavuc;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;

/**
 * Represents a building obstacle in the game.
 */
public class Building {
    // Position and size
    private Vector2 position;
    private float size;
    private float height;
    
    // 3D model
    private ModelInstance modelInstance;
    
    /**
     * Create a new building
     */
    public Building(float x, float y, float size, float height) {
        position = new Vector2(x, y);
        this.size = size;
        this.height = height;
        
        // Note: Model instance will be set externally by the renderer
    }
    
    // Getters and setters
    
    public Vector2 getPosition() {
        return position;
    }
    
    public float getSize() {
        return size;
    }
    
    public float getHeight() {
        return height;
    }
    
    public void setModelInstance(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }
    
    public ModelInstance getModelInstance() {
        return modelInstance;
    }
}