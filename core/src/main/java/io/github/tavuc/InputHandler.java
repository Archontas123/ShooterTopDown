package io.github.tavuc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

/**
 * Handles input for the game.
 */
public class InputHandler extends InputAdapter {
    private final ShooterGame game;
    private final GameScreen gameScreen;
    private final GameState gameState;
    private final Vector2 moveDirection;
    
    public InputHandler(ShooterGame game, GameScreen gameScreen) {
        this.game = game;
        this.gameScreen = gameScreen;
        this.gameState = game.getGameState();
        this.moveDirection = new Vector2();
    }
    
    /**
     * Check if a point is inside the hexagonal arena
     * Takes into account the border width with improved boundary checking
     */
    private boolean isInsideHexagon(float x, float y, float hexRadius) {
        // For a regular hexagon, we can use this formula to determine if a point is inside
        // Math based on: distance in "hex space" <= radius
        float q = x * (2f/3f);
        float r = (-x/3f) + (float)(Math.sqrt(3f)/3f) * y;
        float s = (-x/3f) - (float)(Math.sqrt(3f)/3f) * y;
        
        float distance = Math.max(Math.abs(q), Math.max(Math.abs(r), Math.abs(s)));
        
        // Use a stricter boundary to ensure the player can't cross the border
        // The player radius (0.4f) plus some margin
        float effectiveRadius = hexRadius - ShooterGame.ARENA_BORDER_WIDTH - 0.8f;
        
        return distance <= effectiveRadius;
    }
    
    /**
     * Process input and update game state
     */
    public void update(float delta) {
        // Handle key input for movement
        moveDirection.set(0, 0);
        
        if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP)) {
            moveDirection.y += 1;
        }
        if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN)) {
            moveDirection.y -= 1;
        }
        if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT)) {
            moveDirection.x -= 1;
        }
        if (Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT)) {
            moveDirection.x += 1;
        }
        
        // Normalize diagonal movement
        if (moveDirection.len2() > 1) {
            moveDirection.nor();
        }
        
        // Apply movement to player
        Player player = gameState.getPlayer();
        if (moveDirection.len2() > 0) {
            // Calculate potential new position
            float moveSpeed = 0.15f;
            float newX = player.getPosition().x + moveDirection.x * moveSpeed;
            float newY = player.getPosition().y + moveDirection.y * moveSpeed;
            
            // Check arena boundaries with stricter collision
            boolean canMoveX = true;
            boolean canMoveY = true;
            
            // Use hexagon boundary check instead of circular distance
            if (!isInsideHexagon(newX, player.getPosition().y, ShooterGame.ARENA_RADIUS)) {
                canMoveX = false;
            }
            
            if (!isInsideHexagon(player.getPosition().x, newY, ShooterGame.ARENA_RADIUS)) {
                canMoveY = false;
            }
            
            // Check building collisions
            for (Building building : gameState.getBuildings()) {
                if (checkCollision(newX, player.getPosition().y, 0.4f, building)) {
                    canMoveX = false;
                }
                
                if (checkCollision(player.getPosition().x, newY, 0.4f, building)) {
                    canMoveY = false;
                }
            }
            
            // Apply movement
            if (canMoveX) {
                player.getPosition().x = newX;
            }
            if (canMoveY) {
                player.getPosition().y = newY;
            }
        }
        
        // Handle mouse input for rotation
        updateRotation();
        
        // Handle mouse clicks
        if (Gdx.input.isTouched() && !player.isReloading() && !gameScreen.isGameOver()) {
            // Try to shoot
            Array<Projectile> projectiles = new Array<>(player.fire());
            for (Projectile projectile : projectiles) {
                gameState.getProjectiles().add(projectile);
            }
        }
        
        // Handle keyboard inputs for actions
        if (Gdx.input.isKeyJustPressed(Keys.G) && !gameScreen.isGameOver()) {
            throwGrenade();
        }
        
        if (Gdx.input.isKeyJustPressed(Keys.R) && !gameScreen.isGameOver()) {
            reload();
        }
        
        if (Gdx.input.isKeyJustPressed(Keys.SPACE) && !gameScreen.isGameOver()) {
            performDash();
        }
        
        // Weapon switching
        if (Gdx.input.isKeyJustPressed(Keys.NUM_1) && !gameScreen.isGameOver()) {
            switchWeapon(WeaponType.PISTOL);
        } else if (Gdx.input.isKeyJustPressed(Keys.NUM_2) && !gameScreen.isGameOver()) {
            switchWeapon(WeaponType.SHOTGUN);
        } else if (Gdx.input.isKeyJustPressed(Keys.NUM_3) && !gameScreen.isGameOver()) {
            switchWeapon(WeaponType.RIFLE);
        } else if (Gdx.input.isKeyJustPressed(Keys.NUM_4) && !gameScreen.isGameOver()) {
            switchWeapon(WeaponType.LAUNCHER);
        }
    }
    
    /**
     * Update player rotation based on mouse position
     */
    private void updateRotation() {
        if (gameScreen.isGameOver()) return;
        
        // Convert mouse coordinates to world coordinates
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();
        
        // Update mouse position in game state
        gameState.setMousePos(mouseX, mouseY);
        gameState.setMouseDown(Gdx.input.isTouched());
        
        // Calculate ray from camera through mouse position
        Ray ray = gameScreen.getCamera().getPickRay(mouseX, mouseY);
        
        // Calculate intersection with y=0 plane
        Vector3 intersection = new Vector3();
        float t = -ray.origin.y / ray.direction.y;
        intersection.set(ray.origin).add(ray.direction.cpy().scl(t));
        
        // Calculate direction from player to intersection point
        Player player = gameState.getPlayer();
        Vector2 direction = new Vector2(
            intersection.x - player.getPosition().x,
            intersection.z - player.getPosition().y
        );
        
        // Calculate angle
        float rotation = (float) Math.atan2(direction.y, direction.x);
        player.setRotation(rotation);
    }
    
    /**
     * Check collision between circle and rectangle
     */
    private boolean checkCollision(float circleX, float circleY, float circleRadius, Building building) {
        // Find closest point on rectangle to circle center
        float closestX = Math.max(building.getPosition().x - building.getSize()/2, 
                               Math.min(circleX, building.getPosition().x + building.getSize()/2));
        float closestY = Math.max(building.getPosition().y - building.getSize()/2, 
                               Math.min(circleY, building.getPosition().y + building.getSize()/2));
        
        // Calculate distance between closest point and circle center
        float distanceX = circleX - closestX;
        float distanceY = circleY - closestY;
        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
        
        return distanceSquared <= (circleRadius * circleRadius);
    }
    
    @Override
    public boolean keyDown(int keycode) {
        // Store key state
        gameState.getKeys().put(keycode, true);
        
        // Special handling for restart
        if (keycode == Keys.ESCAPE && gameScreen.isGameOver()) {
            gameScreen.restartGame();
            return true;
        }
        
        return true;
    }
    
    @Override
    public boolean keyUp(int keycode) {
        // Store key state
        gameState.getKeys().put(keycode, false);
        return true;
    }
    
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        gameState.setMouseDown(true);
        return true;
    }
    
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        gameState.setMouseDown(false);
        return true;
    }
    
    /**
     * Switch to the specified weapon
     */
    private void switchWeapon(WeaponType weaponType) {
        if (gameScreen.isGameOver()) return;
        gameState.getPlayer().switchWeapon(weaponType);
        gameScreen.updateWeaponHUD();
    }
    
    /**
     * Reload the current weapon
     */
    private void reload() {
        if (gameScreen.isGameOver()) return;
        gameState.getPlayer().reload();
    }
    
    /**
     * Throw a grenade
     */
    private void throwGrenade() {
        if (gameScreen.isGameOver()) return;
        
        Projectile grenade = gameState.getPlayer().throwGrenade();
        if (grenade != null) {
            gameState.getProjectiles().add(grenade);
            gameScreen.updateGrenadeHUD();
        }
    }
    
    /**
     * Perform a dash
     */
    private void performDash() {
        if (gameScreen.isGameOver()) return;
        
        gameState.getPlayer().dash(moveDirection);
        
        // Create dash effect (trailing particles)
        createDashEffect();
    }
    
    /**
     * Create visual effect for dash
     */
    private void createDashEffect() {
        Player player = gameState.getPlayer();
        float dashAngle = player.getRotation();
        
        if (moveDirection.len2() > 0) {
            dashAngle = (float) Math.atan2(moveDirection.y, moveDirection.x);
        }
        
        float dashDistance = 5;
        
        // Create particle trail
        for (float i = 0; i < dashDistance; i += 0.5f) {
            float particleX = player.getPosition().x - (float)Math.cos(dashAngle) * i;
            float particleY = player.getPosition().y - (float)Math.sin(dashAngle) * i;
            
            Projectile particle = new Projectile(particleX, particleY, 10 - (int)i);
            gameState.getProjectiles().add(particle);
        }
    }
}