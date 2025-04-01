package io.github.tavuc;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the current state of the game.
 * This class stores all the game's data.
 */
public class GameState {
    // Game state
    private boolean active;
    private int score;
    
    // Player state
    private Player player;
    
    // Game objects
    private Array<Projectile> projectiles;
    private Array<Enemy> enemies;
    private Array<Building> buildings;
    private Array<Explosion> explosions;
    
    // Input state
    private Map<Integer, Boolean> keys;
    private Vector2 mousePos;
    private boolean mouseDown;

    public GameState() {
        // Initialize game state
        active = false;
        score = 0;
        
        // Initialize player
        player = new Player();
        
        // Initialize game objects
        projectiles = new Array<>();
        enemies = new Array<>();
        buildings = new Array<>();
        explosions = new Array<>();
        
        // Initialize input state
        keys = new HashMap<>();
        mousePos = new Vector2();
        mouseDown = false;
    }
    
    /**
     * Start a new game
     */
    public void startGame() {
        // Reset game state
        active = true;
        score = 0;
        
        // Reset player
        player.reset();
        
        // Clear game objects
        projectiles.clear();
        enemies.clear();
        explosions.clear();
        
        // Clear and recreate buildings
        buildings.clear();
        generateBuildings();
        
        // Spawn initial enemies
        for (int i = 0; i < 3; i++) {
            spawnEnemy();
        }
    }
    
    /**
     * Generate buildings in the game arena
     */
    private void generateBuildings() {
        // Create buildings
        buildings.clear();
        int attempts = 0;
        int maxAttempts = 100;
        
        for (int i = 0; i < 15 && attempts < maxAttempts; attempts++) {
            // Random position within arena (with margin)
            float angle = (float) (Math.random() * Math.PI * 2);
            float distance = (float) (Math.random() * (ShooterGame.ARENA_RADIUS * 0.7f));
            float x = (float) (Math.cos(angle) * distance);
            float y = (float) (Math.sin(angle) * distance);
            
            float size = (float) (Math.random() * 3 + 1);
            float height = (float) (Math.random() * 3 + 2);
            
            // Check if this building would overlap with player spawn point
            if (Math.sqrt(x*x + y*y) < 5) {
                // Too close to center, try again
                continue;
            }
            
            // Check if this building overlaps with existing buildings
            boolean overlaps = false;
            for (Building existing : buildings) {
                float dx = existing.getPosition().x - x;
                float dy = existing.getPosition().y - y;
                float minDistance = (existing.getSize() + size) / 2 + 1f;
                
                if (Math.sqrt(dx*dx + dy*dy) < minDistance) {
                    overlaps = true;
                    break;
                }
            }
            
            if (!overlaps) {
                buildings.add(new Building(x, y, size, height));
                i++; // Only increment i if we successfully added a building
            }
        }
    }
    
    /**
     * Helper method to get a point on the edge of the hexagon
     * Takes into account the border width to spawn enemies inside the playable area
     */
    private Vector2 getPointOnHexagonEdge(float angle) {
        // For a regular hexagon
        float sideAngle = (float) (Math.PI / 3); // 60 degrees
        float adjustedAngle = angle;
        
        // Find which side of the hexagon this angle corresponds to
        int side = (int) (adjustedAngle / sideAngle);
        if (side >= 6) side = 0;
        
        // Calculate the two vertices of that side
        float angle1 = side * sideAngle;
        float angle2 = (side + 1) * sideAngle;
        
        float borderWidth = ShooterGame.ARENA_BORDER_WIDTH; // Must match the border width in createHexagonModel
        float effectiveRadius = ShooterGame.ARENA_RADIUS * 0.8f - borderWidth;
        
        float x1 = (float) (effectiveRadius * Math.cos(angle1));
        float y1 = (float) (effectiveRadius * Math.sin(angle1));
        float x2 = (float) (effectiveRadius * Math.cos(angle2));
        float y2 = (float) (effectiveRadius * Math.sin(angle2));
        
        // Calculate how far along the side this angle is
        float t = (adjustedAngle - angle1) / sideAngle;
        if (t > 1) t = 1;
        if (t < 0) t = 0;
        
        // Interpolate between the two vertices
        float x = x1 + t * (x2 - x1);
        float y = y1 + t * (y2 - y1);
        
        return new Vector2(x, y);
    }
    
    /**
     * Spawn a new enemy at a random position on the hexagon edge
     * Takes into account the border width
     */
    public void spawnEnemy() {
        // Generate position away from player (on edge of arena)
        float angle = (float) (Math.random() * Math.PI * 2);
        
        // Get point on hexagon edge, inside the border
        Vector2 position = getPointOnHexagonEdge(angle);
        
        // Check for collision with buildings
        boolean collides = false;
        for (Building building : buildings) {
            float dx = building.getPosition().x - position.x;
            float dy = building.getPosition().y - position.y;
            float minDistance = building.getSize() / 2 + 0.8f; // 0.8 is enemy size
            
            if (Math.sqrt(dx*dx + dy*dy) < minDistance) {
                collides = true;
                break;
            }
        }
        
        // If collision detected, try another position
        if (collides) {
            spawnEnemy(); // Recursive call to try again
            return;
        }
        
        // Choose enemy type
        boolean isFast = Math.random() > 0.7;
        
        // Create and add enemy
        Enemy enemy = new Enemy(position.x, position.y, isFast);
        enemies.add(enemy);
    }
    
    // Getters and setters
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public int getScore() {
        return score;
    }
    
    public void addScore(int amount) {
        score += amount;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Array<Projectile> getProjectiles() {
        return projectiles;
    }
    
    public Array<Enemy> getEnemies() {
        return enemies;
    }
    
    public Array<Building> getBuildings() {
        return buildings;
    }
    
    public Array<Explosion> getExplosions() {
        return explosions;
    }
    
    public Map<Integer, Boolean> getKeys() {
        return keys;
    }
    
    public Vector2 getMousePos() {
        return mousePos;
    }
    
    public void setMousePos(float x, float y) {
        mousePos.set(x, y);
    }
    
    public boolean isMouseDown() {
        return mouseDown;
    }
    
    public void setMouseDown(boolean mouseDown) {
        this.mouseDown = mouseDown;
    }
}