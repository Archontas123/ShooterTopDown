package io.github.tavuc.platformer.effects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import io.github.tavuc.platformer.utils.Logger;

/**
 * Manages all visual effects in the game.
 * Acts as a centralized system for creating and updating different effects.
 */
public class EffectsManager implements Disposable {
    private final Logger logger;
    private final ParticleEffect particleEffect;
    
    private static final Color WALK_DUST_COLOR = new Color(0.7f, 0.7f, 0.65f, 1.0f);
    private static final Color DASH_TRAIL_COLOR = new Color(0.2f, 0.6f, 1.0f, 1.0f);
    private static final Color WALL_SLIDE_COLOR = new Color(0.8f, 0.8f, 0.8f, 1.0f);
    private static final Color WALL_RUN_COLOR = new Color(0.9f, 0.6f, 0.3f, 1.0f); 
    
    private float walkEffectTimer = 0f;
    private static final float WALK_EFFECT_INTERVAL = 0.3f; 
    
    private final Vector3 previousPosition = new Vector3();
    private final Vector3 tempVector = new Vector3(); 
    
    /**
     * Creates a new effects manager.
     *
     * @param logger The logger instance
     */
    public EffectsManager(Logger logger) {
        this.logger = logger;
        this.particleEffect = new ParticleEffect();
        logger.info("Effects manager initialized");
    }
    
    /**
     * Updates all effects.
     *
     * @param delta The time in seconds since the last update
     */
    public void update(float delta) {
        particleEffect.update(delta);
        
        if (walkEffectTimer > 0) {
            walkEffectTimer -= delta;
        }
    }
    
    /**
     * Renders all effects.
     *
     * @param modelBatch The model batch to render with
     * @param environment The lighting environment
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        particleEffect.render(modelBatch, environment);
    }
    
    /**
     * Creates a walking dust effect at the specified position if enough time has passed.
     *
     * @param position The position of the player
     * @param isMoving Whether the player is moving
     */
    public void createWalkDustEffect(Vector3 position, boolean isMoving) {
        if (!isMoving) {
            return;
        }
        
        if (walkEffectTimer <= 0) {
            tempVector.set(position.x, position.y - 0.5f, position.z);
            particleEffect.createDustEffect(tempVector, WALK_DUST_COLOR, 3);
            walkEffectTimer = WALK_EFFECT_INTERVAL;
        }
    }
    
    /**
     * Creates a dash trail effect between the previous and current position.
     *
     * @param currentPosition The current position of the player
     * @param isDashing Whether the player is currently dashing
     */
    public void createDashTrailEffect(Vector3 currentPosition, boolean isDashing) {
        if (!isDashing) {
            previousPosition.set(currentPosition);
            return;
        }
        
        if (previousPosition.dst2(currentPosition) < 0.5f) {
            previousPosition.set(currentPosition);
            return;
        }
        
        particleEffect.createTrailEffect(
            previousPosition,
            currentPosition,
            DASH_TRAIL_COLOR,
            10 
        );
        
        previousPosition.set(currentPosition);
    }
    
    /**
     * Creates a wall slide effect at the specified position.
     *
     * @param position The position of the player
     * @param wallNormal The normal vector of the wall
     */
    public void createWallSlideEffect(Vector3 position, Vector3 wallNormal) {
        tempVector.set(position);
        
        // Create particles that appear to slide down the wall
        for (int i = 0; i < 2; i++) { 
            float offsetY = -0.1f + (float)Math.random() * 0.2f;
            
            // Calculate position slightly offset from the wall
            Vector3 particlePos = new Vector3(position).add(
                wallNormal.x * 0.1f,
                offsetY,
                wallNormal.z * 0.1f
            );
            
            particleEffect.createDustEffect(particlePos, WALL_SLIDE_COLOR, 1);
        }
        
        logger.debug("Created wall slide effect at: " + position);
    }
    
    /**
     * Creates a wall run effect at the specified position.
     *
     * @param position The position of the player
     * @param wallNormal The normal vector of the wall
     * @param forwardSpeed The player's forward speed
     */
    public void createWallRunEffect(Vector3 position, Vector3 wallNormal, float forwardSpeed) {
        tempVector.set(position);
        
        float direction = Math.signum(forwardSpeed);
        Vector3 moveDir = new Vector3();
        
        // Determine movement direction based on wall orientation
        if (Math.abs(wallNormal.x) > 0.9f) {
            // Wall facing X direction, movement is along Z
            moveDir.set(0, 0, direction);
        } else if (Math.abs(wallNormal.z) > 0.9f) {
            // Wall facing Z direction, movement is along X
            moveDir.set(direction, 0, 0);
        }
        
        for (int i = 0; i < 3; i++) {
            float offsetY = -0.3f + (float)Math.random() * 0.6f;
            
            // Position particle slightly away from wall
            Vector3 particlePos = new Vector3(position).add(
                wallNormal.x * 0.1f,
                offsetY,
                wallNormal.z * 0.1f
            );
            
            // Add slight offset in movement direction
            particlePos.add(
                moveDir.x * ((float)Math.random() * 0.5f),
                0,
                moveDir.z * ((float)Math.random() * 0.5f)
            );
            
            particleEffect.createStretchedEffect(
                particlePos,
                moveDir, 
                WALL_RUN_COLOR,
                0.8f + (float)Math.random() * 0.4f 
            );
        }
        
        logger.debug("Created wall run effect at: " + position + " with direction: " + moveDir);
    }
    
    /**
     * Sets the initial previous position for effects that need it.
     *
     * @param position The current position to set as previous
     */
    public void setInitialPosition(Vector3 position) {
        previousPosition.set(position);
    }

    /**
     * Disposes of resources used by the effects manager.
     * Properly releases all resources to prevent memory leaks.
     */
    @Override
    public void dispose() {
        if (particleEffect != null) {
            particleEffect.dispose();
        }
        logger.info("Effects manager disposed");
    }

    /**
     * Creates a jump effect at the specified position.
     *
     * @param position The position of the player
     * @param jumpingUp Whether the player is jumping up (true) or landing (false)
     */
    public void createJumpEffect(Vector3 position, boolean jumpingUp) {
        Color color = jumpingUp ? new Color(0.9f, 0.9f, 1.0f, 1.0f) : new Color(0.8f, 0.8f, 0.7f, 1.0f);
        int count = jumpingUp ? 12 : 15;
        
        for (int i = 0; i < count; i++) {
            Vector3 effectPos = new Vector3(position);
            
            float angle = MathUtils.random(360) * MathUtils.degreesToRadians;
            float radius = MathUtils.random(0.2f, 0.5f);
            effectPos.add(
                MathUtils.cos(angle) * radius,
                jumpingUp ? -0.5f : -0.8f,
                MathUtils.sin(angle) * radius
            );
            
            particleEffect.createDustEffect(effectPos, color, 1);
        }
        
        logger.debug("Created jump effect at: " + position + " (jumping up: " + jumpingUp + ")");
    }
    
    /**
     * Creates a wall jump effect at the specified position.
     *
     * @param position The position of the player
     * @param wallNormal The normal vector of the wall
     */
    public void createWallJumpEffect(Vector3 position, Vector3 wallNormal) {
        Color color = new Color(1.0f, 0.8f, 0.3f, 1.0f);
        int count = 15;
        
        // Direction to spawn particles (away from wall)
        Vector3 jumpDir = new Vector3(wallNormal).nor();
        
        for (int i = 0; i < count; i++) {
            Vector3 effectPos = new Vector3(position);
            
            // Spawn in a cone shape away from the wall
            float angle = MathUtils.random(-45, 45) * MathUtils.degreesToRadians;
            float radius = MathUtils.random(0.2f, 0.5f);
            
            // Create a rotation matrix to rotate jumpDir around the Y axis
            float cosAngle = MathUtils.cos(angle);
            float sinAngle = MathUtils.sin(angle);
            
            Vector3 rotatedDir = new Vector3();
            if (Math.abs(jumpDir.x) > 0.9f) {
                // Rotate around Y for X-aligned walls
                rotatedDir.x = jumpDir.x * cosAngle - jumpDir.z * sinAngle;
                rotatedDir.y = 0;
                rotatedDir.z = jumpDir.x * sinAngle + jumpDir.z * cosAngle;
            } else {
                // Rotate around Y for Z-aligned walls
                rotatedDir.x = jumpDir.x * cosAngle + jumpDir.z * sinAngle;
                rotatedDir.y = 0;
                rotatedDir.z = -jumpDir.x * sinAngle + jumpDir.z * cosAngle;
            }
            
            effectPos.add(
                rotatedDir.x * radius,
                MathUtils.random(-0.3f, 0.3f),
                rotatedDir.z * radius
            );
            
            // Create stretched effect in the jump direction
            particleEffect.createStretchedEffect(
                effectPos,
                rotatedDir,
                color,
                MathUtils.random(0.5f, 1.0f)
            );
        }
        
        logger.debug("Created wall jump effect at: " + position);
    }
}