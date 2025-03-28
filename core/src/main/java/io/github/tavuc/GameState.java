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
        for (int i = 0; i < 15; i++) {
            // Random position within arena (with margin)
            float angle = (float) (Math.random() * Math.PI * 2);
            float distance = (float) (Math.random() * (ShooterGame.ARENA_RADIUS * 0.8));
            float x = (float) (Math.cos(angle) * distance);
            float y = (float) (Math.sin(angle) * distance);
            
            float size = (float) (Math.random() * 3 + 1);
            float height = (float) (Math.random() * 3 + 2);
            
            buildings.add(new Building(x, y, size, height));
        }
    }
    
    /**
     * Spawn a new enemy at a random position
     */
    public void spawnEnemy() {
        // Generate position away from player (on edge of arena)
        float angle = (float) (Math.random() * Math.PI * 2);
        float x = (float) (Math.cos(angle) * (ShooterGame.ARENA_RADIUS * 0.8));
        float y = (float) (Math.sin(angle) * (ShooterGame.ARENA_RADIUS * 0.8));
        
        // Choose enemy type
        boolean isFast = Math.random() > 0.7;
        
        // Create and add enemy
        Enemy enemy = new Enemy(x, y, isFast);
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