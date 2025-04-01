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
    private boolean isGrenade; // Whether this is a grenade explosion (3x more powerful)
    
    // 3D model
    private ModelInstance modelInstance;
    
    /**
     * Create a new explosion
     * @param isGrenade Whether this is a grenade explosion (more powerful)
     */
    public Explosion(float x, float y, boolean isGrenade) {
        position = new Vector2(x, y);
        radius = 0.5f;
        this.isGrenade = isGrenade;
        
        // Grenades are 3x more powerful with larger radius
        if (isGrenade) {
            maxRadius = 15.0f;
            growth = 0.6f;
            life = 30;
            damage = 300;  // Triple damage
        } else {
            maxRadius = 5.0f; // Launcher explosions are larger than before
            growth = 0.3f;
            life = 20;
            damage = 150;  // Buffed from 100
        }
        
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
        return (float) life / (isGrenade ? 30.0f : 20.0f);
    }
    
    public boolean isGrenade() {
        return isGrenade;
    }
    
    public void setModelInstance(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }
    
    public ModelInstance getModelInstance() {
        return modelInstance;
    }
}