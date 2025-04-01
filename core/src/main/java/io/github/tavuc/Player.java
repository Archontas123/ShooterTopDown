package io.github.tavuc;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the player character in the game.
 */
public class Player {
    // Position and movement
    private Vector2 position;
    private float rotation;
    private boolean isImmune;
    
    // Health and abilities
    private int health;
    private int grenades;
    private int grenadeRefreshTimer;
    private int grenadeRefreshRate;
    private int dashCooldown;
    private int dashCooldownMax;
    
    // Weapon system
    private WeaponType currentWeapon;
    private int weaponCooldown;
    private boolean reloading;
    private int reloadTime;
    private Map<WeaponType, Integer> ammo;
    private Map<WeaponType, Integer> maxAmmo;

    public Player() {
        // Initialize position
        position = new Vector2(0, 0);
        rotation = 0;
        isImmune = false;
        
        // Initialize health and abilities
        health = 100;
        grenades = 3;
        grenadeRefreshTimer = 0;
        grenadeRefreshRate = 500; // frames until new grenade
        dashCooldown = 0;
        dashCooldownMax = 120; // frames (2 seconds at 60fps)
        
        // Initialize weapon system
        currentWeapon = WeaponType.PISTOL;
        weaponCooldown = 0;
        reloading = false;
        reloadTime = 0;
        
        // Initialize ammo
        ammo = new HashMap<>();
        maxAmmo = new HashMap<>();
        
        for (WeaponType type : WeaponType.values()) {
            switch (type) {
                case PISTOL:
                    maxAmmo.put(type, 12);
                    break;
                case SHOTGUN:
                    maxAmmo.put(type, 6);
                    break;
                case RIFLE:
                    maxAmmo.put(type, 30);
                    break;
                case LAUNCHER:
                    maxAmmo.put(type, 3);
                    break;
            }
            ammo.put(type, maxAmmo.get(type));
        }
    }
    
    /**
     * Reset the player to initial state
     */
    public void reset() {
        position.set(0, 0);
        rotation = 0;
        isImmune = false;
        health = 100;
        grenades = 3;
        grenadeRefreshTimer = 0;
        dashCooldown = 0;
        currentWeapon = WeaponType.PISTOL;
        weaponCooldown = 0;
        reloading = false;
        reloadTime = 0;
        
        // Reset ammo
        for (WeaponType type : WeaponType.values()) {
            ammo.put(type, maxAmmo.get(type));
        }
    }
    
    /**
     * Update player state based on delta time
     */
    public void update(float delta) {
        // Update weapon cooldown
        if (weaponCooldown > 0) {
            weaponCooldown--;
        }
        
        // Update reloading
        if (reloading) {
            reloadTime--;
            
            if (reloadTime <= 0) {
                // Reload complete
                reloading = false;
                ammo.put(currentWeapon, maxAmmo.get(currentWeapon));
            }
        }
        
        // Update dash cooldown
        if (dashCooldown > 0) {
            dashCooldown--;
        }
        
        // Update grenade regeneration
        if (grenades < 3) {
            grenadeRefreshTimer++;
            
            if (grenadeRefreshTimer >= grenadeRefreshRate) {
                grenades++;
                grenadeRefreshTimer = 0;
            }
        }
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
     * Fire the current weapon
     */
    public Projectile[] fire() {
        // Check if can fire
        if (weaponCooldown > 0 || 
            ammo.get(currentWeapon) <= 0 || 
            reloading) {
            return new Projectile[0];
        }
        
        // Set cooldown based on weapon type
        weaponCooldown = getWeaponStats().getCooldown();
        
        // Reduce ammo
        ammo.put(currentWeapon, ammo.get(currentWeapon) - 1);
        
        // Create projectiles
        Projectile[] projectiles;
        
        // Spawn position
        float spawnDistance = 1;
        float spawnX = position.x + (float) Math.cos(rotation) * spawnDistance;
        float spawnY = position.y + (float) Math.sin(rotation) * spawnDistance;
        
        switch (currentWeapon) {
            case SHOTGUN:
                // Create multiple projectiles with spread
                WeaponStats stats = getWeaponStats();
                projectiles = new Projectile[stats.getShotCount()];
                
                for (int i = 0; i < stats.getShotCount(); i++) {
                    float spreadAngle = rotation + (float)((Math.random() * 2 - 1) * stats.getSpread());
                    projectiles[i] = new Projectile(
                        spawnX, spawnY, spreadAngle, stats.getProjectileSpeed(),
                        stats.getDamage(), stats.getProjectileColor(), false);
                }
                break;
                
            case LAUNCHER:
                // Create explosive projectile
                WeaponStats launcherStats = getWeaponStats();
                projectiles = new Projectile[1];
                projectiles[0] = new Projectile(
                    spawnX, spawnY, rotation, launcherStats.getProjectileSpeed(),
                    launcherStats.getDamage(), launcherStats.getProjectileColor(), true);
                break;
                
            default:
                // Create single projectile (pistol or rifle)
                WeaponStats regularStats = getWeaponStats();
                projectiles = new Projectile[1];
                projectiles[0] = new Projectile(
                    spawnX, spawnY, rotation, regularStats.getProjectileSpeed(),
                    regularStats.getDamage(), regularStats.getProjectileColor(), false);
                break;
        }
        
        // Auto reload if empty
        if (ammo.get(currentWeapon) <= 0) {
            reload();
        }
        
        return projectiles;
    }
    
    /**
     * Throw a grenade
     */
    public Projectile throwGrenade() {
        if (grenades <= 0) {
            return null;
        }
        
        grenades--;
        
        float spawnDistance = 1;
        float spawnX = position.x + (float) Math.cos(rotation) * spawnDistance;
        float spawnY = position.y + (float) Math.sin(rotation) * spawnDistance;
        
        return new Projectile(
            spawnX, spawnY, rotation, 0.2f,
            100, 0x2ecc71, true, 90); // Fuse timer: 90 frames (1.5 seconds at 60fps)
    }
    
    /**
     * Perform a dash in the movement or facing direction
     */
    public void dash(Vector2 moveDirection) {
        if (dashCooldown > 0) {
            return;
        }
        
        dashCooldown = dashCooldownMax;
        
        // Get dash direction
        float dashAngle = rotation;
        
        if (moveDirection.len2() > 0) {
            // Use actual move direction for dash, not the angle in degrees
            dashAngle = (float) Math.atan2(moveDirection.y, moveDirection.x);
        }
        
        // Apply dash movement
        float dashDistance = 5;
        float targetX = position.x + (float) Math.cos(dashAngle) * dashDistance;
        float targetY = position.y + (float) Math.sin(dashAngle) * dashDistance;
        
        // Ensure player doesn't dash outside arena
        if (!isInsideHexagon(targetX, targetY, ShooterGame.ARENA_RADIUS)) {
            // Adjust the dash to stay inside arena
            // Binary search to find the farthest valid point
            float low = 0;
            float high = dashDistance;
            float mid;
            
            for (int i = 0; i < 10; i++) { // 10 iterations should be enough for precision
                mid = (low + high) / 2;
                float testX = position.x + (float) Math.cos(dashAngle) * mid;
                float testY = position.y + (float) Math.sin(dashAngle) * mid;
                
                if (isInsideHexagon(testX, testY, ShooterGame.ARENA_RADIUS)) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
            
            // Use the best approximation
            targetX = position.x + (float) Math.cos(dashAngle) * low;
            targetY = position.y + (float) Math.sin(dashAngle) * low;
        }
        
        // Apply dash
        position.set(targetX, targetY);
        
        // Temporary immunity
        isImmune = true;
        
        // Will be set back to false after a short delay by the game screen
    }
    
    /**
     * Reload the current weapon
     */
    public void reload() {
        // Don't reload if already reloading or full ammo
        if (reloading || ammo.get(currentWeapon).equals(maxAmmo.get(currentWeapon))) {
            return;
        }
        
        reloading = true;
        reloadTime = getWeaponStats().getReloadTime();
    }
    
    /**
     * Switch to a different weapon
     */
    public void switchWeapon(WeaponType newWeapon) {
        if (reloading) {
            return;
        }
        
        currentWeapon = newWeapon;
    }
    
    /**
     * Take damage
     * @return true if player was damaged, false if immune
     */
    public boolean takeDamage(int amount) {
        if (isImmune) {
            return false;
        }
        
        health -= amount;
        return true;
    }
    
    /**
     * Get stats for the current weapon
     */
    public WeaponStats getWeaponStats() {
        return WeaponType.getStats(currentWeapon);
    }
    
    // Getters and setters
    
    public Vector2 getPosition() {
        return position;
    }
    
    public void setPosition(float x, float y) {
        position.set(x, y);
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
    
    public int getHealth() {
        return health;
    }
    
    public boolean isImmune() {
        return isImmune;
    }
    
    public void setImmune(boolean immune) {
        isImmune = immune;
    }
    
    public int getGrenades() {
        return grenades;
    }
    
    public float getGrenadeRefreshPercentage() {
        return grenades < 3 ? (float)grenadeRefreshTimer / grenadeRefreshRate : 0;
    }
    
    public float getDashCooldownPercentage() {
        return (float)dashCooldown / dashCooldownMax;
    }
    
    public WeaponType getCurrentWeapon() {
        return currentWeapon;
    }
    
    public int getCurrentAmmo() {
        return ammo.get(currentWeapon);
    }
    
    public int getMaxAmmo() {
        return maxAmmo.get(currentWeapon);
    }
    
    public boolean isReloading() {
        return reloading;
    }
    
    public float getReloadPercentage() {
        if (!reloading) return 0;
        return (float)reloadTime / getWeaponStats().getReloadTime();
    }
}