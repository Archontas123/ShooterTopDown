package io.github.tavuc;

/**
 * Enum representing the different types of weapons in the game.
 */
public enum WeaponType {
    PISTOL,
    SHOTGUN,
    RIFLE,
    LAUNCHER;
    
    /**
     * Get stats for a specific weapon type
     */
    public static WeaponStats getStats(WeaponType type) {
        switch (type) {
            case PISTOL:
                return new WeaponStats(25, 20, 60, 0.3f, 0xf1c40f, 1, 0);
            case SHOTGUN:
                return new WeaponStats(15, 45, 90, 0.35f, 0xe67e22, 8, 0.4f);
            case RIFLE:
                return new WeaponStats(15, 8, 70, 0.4f, 0x2ecc71, 1, 0);
            case LAUNCHER:
                // Buffed damage from 90 to 150
                return new WeaponStats(150, 60, 120, 0.25f, 0xe74c3c, 1, 0);
            default:
                throw new IllegalArgumentException("Unknown weapon type: " + type);
        }
    }
}

/**
 * Class containing statistics for weapons.
 */
class WeaponStats {
    private int damage;
    private int cooldown;
    private int reloadTime;
    private float projectileSpeed;
    private int projectileColor;
    private int shotCount;
    private float spread;
    
    public WeaponStats(int damage, int cooldown, int reloadTime, float projectileSpeed, 
                       int projectileColor, int shotCount, float spread) {
        this.damage = damage;
        this.cooldown = cooldown;
        this.reloadTime = reloadTime;
        this.projectileSpeed = projectileSpeed;
        this.projectileColor = projectileColor;
        this.shotCount = shotCount;
        this.spread = spread;
    }
    
    // Getters
    
    public int getDamage() {
        return damage;
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    public int getReloadTime() {
        return reloadTime;
    }
    
    public float getProjectileSpeed() {
        return projectileSpeed;
    }
    
    public int getProjectileColor() {
        return projectileColor;
    }
    
    public int getShotCount() {
        return shotCount;
    }
    
    public float getSpread() {
        return spread;
    }
}