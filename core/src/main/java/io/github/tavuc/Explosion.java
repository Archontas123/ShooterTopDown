package io.github.tavuc;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;

/**
 * Represents an explosion effect in the game.
 */
public class Explosion {
    // Position and size
    private Vector2 position;
    private float radius;
    private float maxRadius;
    private float growth;
    
    // Properties
    private int life;
    private int damage;
    private boolean damageDealt;
    
    // 3D model
    private ModelInstance modelInstance;
    
    /**
     * Create a new explosion
     */
    public Explosion(float x, float y) {
        position = new Vector2(x, y);
        radius = 0.5f;
        maxRadius = 5.0f;
        growth = 0.3f;
        life = 20;
        damage = 100;
        damageDealt = false;
        
        // Note: Model instance will be set externally by the renderer
    }
    
    /**
     * Update explosion state
     * @return true if explosion should be removed
     */
    public boolean update(float delta, GameState gameState) {
        // Grow explosion
        if (radius < maxRadius) {
            radius += growth;
            
            // Deal damage once at maximum size
            if (!damageDealt && radius >= maxRadius * 0.7f) {
                dealDamage(gameState);
                damageDealt = true;
            }
        }
        
        // Reduce life
        life--;
        
        // Remove dead explosions
        return life <= 0;
    }
    
    /**
     * Deal damage to enemies and player in explosion radius
     */
    private void dealDamage(GameState gameState) {
        // Damage enemies
        for (Enemy enemy : gameState.getEnemies()) {
            float dx = position.x - enemy.getPosition().x;
            float dy = position.y - enemy.getPosition().y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (distance < radius) {
                // Calculate damage based on distance
                float damageMultiplier = 1 - (distance / radius);
                boolean killed = enemy.takeDamage((int)(damage * damageMultiplier));
                
                // Add score if enemy was killed
                if (killed) {
                    gameState.addScore(enemy.getScoreValue());
                }
            }
        }
        
        // Damage player
        Player player = gameState.getPlayer();
        if (!player.isImmune()) {
            float dx = position.x - player.getPosition().x;
            float dy = position.y - player.getPosition().y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (distance < radius) {
                // Calculate damage based on distance (reduced for player)
                float damageMultiplier = 1 - (distance / radius);
                player.takeDamage((int)(damage * damageMultiplier * 0.5f));
            }
        }
    }
    
    // Getters and setters
    
    public Vector2 getPosition() {
        return position;
    }
    
    public float getRadius() {
        return radius;
    }
    
    public float getMaxRadius() {
        return maxRadius;
    }
    
    public int getLife() {
        return life;
    }
    
    public float getOpacity() {
        return (float) life / 20.0f;
    }
    
    public void setModelInstance(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }
    
    public ModelInstance getModelInstance() {
        return modelInstance;
    }
}