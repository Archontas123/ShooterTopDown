package io.github.tavuc;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Represents a projectile in the game.
 */
public class Projectile {
    // Position and movement
    private Vector2 position;
    private float angle;
    private float speed;
    
    // Properties
    private int damage;
    private int color;
    private float distance;
    private float maxDistance;
    private boolean isExplosive;
    private boolean isParticle;
    private boolean isFromGrenade; // Flag to determine if this is from a grenade (for enhanced explosions)
    
    // Bounce animation for grenades
    private float bounceHeight;
    private float bounceSpeed;
    private int bounceDirection;
    
    // Explosive properties
    private int timer; // Frames until explosion
    
    // Life for particles
    private int life;
    
    // 3D Model
    private ModelInstance modelInstance;
    
    /**
     * Create a new projectile
     */
    public Projectile(float x, float y, float angle, float speed, int damage, 
                     int color, boolean isExplosive) {
        this(x, y, angle, speed, damage, color, isExplosive, isExplosive ? 60 : 0);
    }
    
    /**
     * Create a new projectile
     * @param isFromGrenade Whether this is from a player grenade (for enhanced explosions)
     */
    public Projectile(float x, float y, float angle, float speed, int damage, 
                     int color, boolean isExplosive, boolean isFromGrenade) {
        this(x, y, angle, speed, damage, color, isExplosive, isExplosive ? 60 : 0);
        this.isFromGrenade = isFromGrenade;
    }
    
    /**
     * Create a new projectile with custom timer
     */
    public Projectile(float x, float y, float angle, float speed, int damage, 
                     int color, boolean isExplosive, int timer) {
        // Initialize position
        position = new Vector2(x, y);
        this.angle = angle;
        this.speed = speed;
        
        // Set properties
        this.damage = damage;
        this.color = color;
        this.distance = 0;
        this.maxDistance = isExplosive ? 20 : 30; // Grenades have shorter range
        this.isExplosive = isExplosive;
        this.isParticle = false;
        this.isFromGrenade = false; // Default to false
        
        // Set bounce animation for grenades
        this.bounceHeight = isExplosive ? 0.5f : 0.5f;
        this.bounceSpeed = isExplosive ? 0.1f : 0;
        this.bounceDirection = 1;
        
        // Set timer for explosives
        this.timer = timer;
        
        // Note: Model instance will be set externally by the renderer
    }
    
    /**
     * Create a particle effect (not a real projectile)
     */
    public Projectile(float x, float y, int life) {
        position = new Vector2(x, y);
        this.life = life;
        this.isParticle = true;
        
        // Unused for particles
        angle = 0;
        speed = 0;
        damage = 0;
        color = 0;
        distance = 0;
        maxDistance = 0;
        isExplosive = false;
        isFromGrenade = false;
        bounceHeight = 0;
        bounceSpeed = 0;
        bounceDirection = 0;
        timer = 0;
    }
    
    /**
     * Check if a point is inside the hexagonal arena
     * Takes into account the border width
     */
    private boolean isInsideHexagon(float x, float y, float hexRadius) {
        // For a regular hexagon, we can use this formula to determine if a point is inside
        // Math based on: distance in "hex space" <= radius
        float q = x * (2f/3f);
        float r = (-x/3f) + (float)(Math.sqrt(3f)/3f) * y;
        float s = (-x/3f) - (float)(Math.sqrt(3f)/3f) * y;
        
        float distance = Math.max(Math.abs(q), Math.max(Math.abs(r), Math.abs(s)));
        float borderWidth = ShooterGame.ARENA_BORDER_WIDTH; // Must match the border width in createHexagonModel
        float adjustedRadius = hexRadius - borderWidth;
        
        return distance <= adjustedRadius;
    }
    
    /**
     * Update projectile position and state
     * @return true if projectile should be removed
     */
    public boolean update(float delta, GameState gameState) {
        // Handle particle cleanup
        if (isParticle) {
            life--;
            return life <= 0;
        }
        
        // Move projectile
        float moveX = (float) Math.cos(angle) * speed;
        float moveY = (float) Math.sin(angle) * speed;
        position.x += moveX;
        position.y += moveY;
        
        // Special behavior for explosive projectiles
        if (isExplosive) {
            // Bounce animation
            bounceHeight += bounceSpeed * bounceDirection;
            if (bounceHeight > 1 || bounceHeight < 0.1) {
                bounceDirection *= -1;
                bounceSpeed *= 0.8f; // Reduce bounce each time
            }
            
            // Update timer
            timer--;
            
            // Explode when timer runs out
            if (timer <= 0) {
                return true; // Will be removed and an explosion will be created
            }
        }
        
        // Calculate distance traveled from player
        Vector2 playerPos = gameState.getPlayer().getPosition();
        float dx = position.x - playerPos.x;
        float dy = position.y - playerPos.y;
        distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        // Check for arena boundary using hexagon collision
        if (!isInsideHexagon(position.x, position.y, ShooterGame.ARENA_RADIUS)) {
            return true; // Will be removed and an explosion might be created for grenades
        }
        
        // Check for building collisions
        for (Building building : gameState.getBuildings()) {
            if (checkCollision(building)) {
                return true; // Will be removed and an explosion might be created
            }
        }
        
        // Remove projectiles that have traveled too far
        return distance > maxDistance;
    }
    
    /**
     * Check collision with a building
     */
    private boolean checkCollision(Building building) {
        // Find closest point on rectangle to circle center
        float closestX = Math.max(building.getPosition().x - building.getSize()/2, 
                               Math.min(position.x, building.getPosition().x + building.getSize()/2));
        float closestY = Math.max(building.getPosition().y - building.getSize()/2, 
                               Math.min(position.y, building.getPosition().y + building.getSize()/2));
        
        // Calculate distance between closest point and circle center
        float distanceX = position.x - closestX;
        float distanceY = position.y - closestY;
        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
        float radius = isExplosive ? 0.3f : 0.15f;
        
        return distanceSquared <= (radius * radius);
    }
    
    /**
     * Set the timer for this projectile
     */
    public void setTimer(int timer) {
        this.timer = timer;
    }
    
    // Getters and setters
    
    public Vector2 getPosition() {
        return position;
    }
    
    public float getAngle() {
        return angle;
    }
    
    public int getDamage() {
        return damage;
    }
    
    public int getColor() {
        return color;
    }
    
    public boolean isExplosive() {
        return isExplosive;
    }
    
    public boolean isParticle() {
        return isParticle;
    }
    
    public boolean isFromGrenade() {
        return isFromGrenade;
    }
    
    public float getBounceHeight() {
        return bounceHeight;
    }
    
    public void setModelInstance(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }
    
    public ModelInstance getModelInstance() {
        return modelInstance;
    }
}