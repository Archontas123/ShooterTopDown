package io.github.tavuc;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;

/**
 * Represents an enemy in the game.
 */
public class Enemy {
    // Position and movement
    private Vector2 position;
    private float rotation;
    
    // Stats
    private int health;
    private float speed;
    private String type;
    private int scoreValue;
    
    // 3D model
    private ModelInstance modelInstance;

    /**
     * Create a new enemy
     */
    public Enemy(float x, float y, boolean isFast) {
        // Initialize position
        position = new Vector2(x, y);
        rotation = 0;
        
        // Set stats based on type
        type = isFast ? "fast" : "normal";
        health = isFast ? 50 : 100;
        speed = isFast ? 0.08f : 0.05f;
        scoreValue = isFast ? 15 : 10;
        
        // Note: Model instance will be set externally by the renderer
    }
    
    /**
     * Update enemy movement and behavior
     */
    public void update(float delta, Player player, GameState gameState) {
        // Calculate direction to player
        Vector2 playerPos = player.getPosition();
        float dx = playerPos.x - position.x;
        float dy = playerPos.y - position.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        // Skip if too far
        if (distance > ShooterGame.ARENA_RADIUS * 2) return;
        
        // Move toward player
        if (distance > 1) {
            // Calculate normalized direction
            float normalizedDx = dx / distance;
            float normalizedDy = dy / distance;
            
            // Calculate potential new position
            float newX = position.x + normalizedDx * speed;
            float newY = position.y + normalizedDy * speed;
            
            boolean canMove = true;
            
            // Check for collisions with buildings
            for (Building building : gameState.getBuildings()) {
                if (checkCollision(newX, newY, 0.4f, building)) {
                    canMove = false;
                    break;
                }
            }
            
            if (canMove) {
                position.x = newX;
                position.y = newY;
            } else {
                // Try to navigate around obstacle
                float perpX = -normalizedDy * speed;
                float perpY = normalizedDx * speed;
                
                position.x += perpX;
                position.y += perpY;
            }
            
            // Update rotation to face player
            rotation = (float) Math.atan2(dy, dx);
        } else {
            // Attack player if close
            if (!player.isImmune() && Math.random() < 0.03) {
                player.takeDamage(10);
            }
        }
    }
    
    /**
     * Check collision with a building
     */
    private boolean checkCollision(float x, float y, float radius, Building building) {
        // Find closest point on rectangle to circle center
        float closestX = Math.max(building.getPosition().x - building.getSize()/2, 
                               Math.min(x, building.getPosition().x + building.getSize()/2));
        float closestY = Math.max(building.getPosition().y - building.getSize()/2, 
                               Math.min(y, building.getPosition().y + building.getSize()/2));
        
        // Calculate distance between closest point and circle center
        float distanceX = x - closestX;
        float distanceY = y - closestY;
        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
        
        return distanceSquared <= (radius * radius);
    }
    
    /**
     * Take damage from a projectile
     * @return true if enemy was killed
     */
    public boolean takeDamage(int amount) {
        health -= amount;
        return health <= 0;
    }
    
    // Getters and setters
    
    public Vector2 getPosition() {
        return position;
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public int getHealth() {
        return health;
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public String getType() {
        return type;
    }
    
    public int getScoreValue() {
        return scoreValue;
    }
    
    public void setModelInstance(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }
    
    public ModelInstance getModelInstance() {
        return modelInstance;
    }
}