package io.github.tavuc.platformer.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import io.github.tavuc.platformer.PlatformerGame;
import io.github.tavuc.platformer.utils.Logger;

/**
 * Represents the player character in the game.
 * Handles movement, abilities, and animations.
 */
public class Player {
    /**
     * Enum representing the player's animation states.
     */
    public enum AnimationState {
        IDLE, 
        WALKING, 
        JUMPING, 
        DASHING, 
        ATTACKING, 
        INVISIBLE
    }
    
    private static final float MOVE_SPEED = 15f; // Faster movement for larger world
    private static final float JUMP_FORCE = 30f; // Stronger jump for larger distances
    private static final float DASH_DISTANCE = 25f; // Longer dash (5 tiles as per requirements)
    
    // Movement control flags
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private static final float DASH_COOLDOWN = 1.5f;
    private static final float JUMP_COOLDOWN = 0.8f;
    
    private final Logger logger;
    private final Model model;
    private final ModelInstance modelInstance;
    
    private Vector3 position;
    private Vector3 velocity;
    private Vector3 acceleration;
    
    private AnimationState currentState;
    private float stateTime;
    
    private boolean isJumping;
    private boolean isDashing;
    private float jumpCooldown;
    private float dashCooldown;
    
    private float idleAnimTime;
    private float bounceOffset;
    
    /**
     * Creates a new player at the specified position.
     * 
     * @param position The initial position
     * @param logger The logger instance
     */
    public Player(Vector3 position, Logger logger) {
        this.position = position;
        this.logger = logger;
        this.velocity = new Vector3(0, 0, 0);
        this.acceleration = new Vector3(0, -9.8f, 0); // Gravity
        
        this.currentState = AnimationState.IDLE;
        this.stateTime = 0;
        
        this.isJumping = false;
        this.isDashing = false;
        this.jumpCooldown = 0;
        this.dashCooldown = 0;
        
        this.idleAnimTime = 0;
        this.bounceOffset = 0;
        
        // Create player model (white cube)
        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(
            PlatformerGame.PLAYER_SIZE, 
            PlatformerGame.PLAYER_SIZE, 
            PlatformerGame.PLAYER_SIZE,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        
        // Disable face culling for the player model
        model.materials.get(0).set(new com.badlogic.gdx.graphics.g3d.attributes.IntAttribute(
            com.badlogic.gdx.graphics.g3d.attributes.IntAttribute.CullFace, 
            com.badlogic.gdx.graphics.GL20.GL_NONE));
        
        modelInstance = new ModelInstance(model);
        updateModelPosition();
        
        logger.info("Player created at position: " + position);
    }
    
    /**
     * Updates the player's state and position.
     * 
     * @param delta Time since the last frame
     */
    public void update(float delta) {
        stateTime += delta;
        
        // Update cooldowns
        if (jumpCooldown > 0) {
            jumpCooldown -= delta;
        }
        
        if (dashCooldown > 0) {
            dashCooldown -= delta;
        }
        
        // Apply physics if not dashing
        if (!isDashing) {
            // Apply acceleration to velocity
            velocity.add(
                acceleration.x * delta,
                acceleration.y * delta,
                acceleration.z * delta
            );
            
            // If jumping, apply horizontal movement based on input flags
            if (isJumping) {
                if (movingUp) {
                    velocity.z = -MOVE_SPEED; // Backward (W)
                } else if (movingDown) {
                    velocity.z = MOVE_SPEED;  // Forward (S)
                } else {
                    velocity.z *= 0.9f; // Slow down if no input
                }
                
                if (movingLeft) {
                    velocity.x = -MOVE_SPEED; // Left (A)
                } else if (movingRight) {
                    velocity.x = MOVE_SPEED;  // Right (D)
                } else {
                    velocity.x *= 0.9f; // Slow down if no input
                }
            }
            
            // Apply velocity to position
            position.add(
                velocity.x * delta,
                velocity.y * delta,
                velocity.z * delta
            );
        }
        
        // Update animation based on state
        updateAnimation(delta);
        
        // Update model position
        updateModelPosition();
    }
    
    /**
     * Updates the player's animation based on current state.
     * 
     * @param delta Time since the last frame
     */
    private void updateAnimation(float delta) {
        switch (currentState) {
            case IDLE:
                // Implement bouncing idle animation
                idleAnimTime += delta * 3;
                bounceOffset = (float) Math.sin(idleAnimTime) * 0.1f;
                break;
                
            case JUMPING:
                if (velocity.y < 0 && !isJumping) {
                    setAnimationState(AnimationState.IDLE);
                }
                break;
                
            case DASHING:
                if (!isDashing) {
                    setAnimationState(AnimationState.IDLE);
                }
                break;
                
            default:
                break;
        }
    }
    
    /**
     * Updates the model's position to match the player's position.
     */
    private void updateModelPosition() {
        modelInstance.transform.setToTranslation(
            position.x,
            position.y + bounceOffset, // Add bounce offset for idle animation
            position.z
        );
    }
    
    /**
     * Moves the player in the specified direction.
     * 
     * @param dirX X direction (-1, 0, 1)
     * @param dirY Y direction (-1, 0, 1)
     * @param delta Time since the last frame
     */
    public void move(float dirX, float dirY, float delta) {
        // Skip movement if dashing
        if (isDashing) {
            return;
        }
        
        // Skip horizontal movement if jumping (handled by flags)
        if (isJumping) {
            return;
        }
        
        // Apply movement
        position.x += dirX * MOVE_SPEED * delta;
        position.z += dirY * MOVE_SPEED * delta;
    }
    
    /**
     * Makes the player jump if not already jumping.
     * Momentum is controlled by player input during jump.
     */
    public void jump() {
        if (!isJumping && jumpCooldown <= 0) {
            isJumping = true;
            velocity.y = JUMP_FORCE; // Purely vertical force
            jumpCooldown = JUMP_COOLDOWN;
            setAnimationState(AnimationState.JUMPING);
            logger.info("Player jumped at position: " + position);
        }
    }
    
    /**
     * Sets movement flags based on input.
     * 
     * @param up Whether the up key (W) is pressed
     * @param down Whether the down key (S) is pressed
     * @param left Whether the left key (A) is pressed
     * @param right Whether the right key (D) is pressed
     */
    public void setMovementFlags(boolean up, boolean down, boolean left, boolean right) {
        this.movingUp = up;
        this.movingDown = down;
        this.movingLeft = left;
        this.movingRight = right;
    }
    
    /**
     * Makes the player dash in the specified direction.
     * 
     * @param dirX X direction (-1, 0, 1)
     * @param dirY Y direction (-1, 0, 1)
     */
    public void dash(float dirX, float dirY) {
        if (!isDashing && dashCooldown <= 0) {
            isDashing = true;
            
            // Normalize the direction vector if needed
            float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
            if (length != 0) {
                dirX /= length;
                dirY /= length;
            }
            
            // Store current position
            final Vector3 startPos = new Vector3(position);
            final Vector3 endPos = new Vector3(
                position.x + dirX * DASH_DISTANCE,
                position.y,
                position.z + dirY * DASH_DISTANCE
            );
            
            // Reset dash after a short time
            new Thread(() -> {
                try {
                    // Move player gradually to dash position
                    float progress = 0;
                    float dashDuration = 0.2f;
                    float stepTime = 0.01f;
                    
                    while (progress < 1) {
                        progress += stepTime / dashDuration;
                        if (progress > 1) progress = 1;
                        
                        position.set(
                            startPos.x + (endPos.x - startPos.x) * progress,
                            startPos.y + (endPos.y - startPos.y) * progress,
                            startPos.z + (endPos.z - startPos.z) * progress
                        );
                        
                        Thread.sleep((long)(stepTime * 1000));
                    }
                    
                    isDashing = false;
                    dashCooldown = DASH_COOLDOWN;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            setAnimationState(AnimationState.DASHING);
            logger.info("Player dashed in direction: (" + dirX + ", " + dirY + ") from: " + startPos + " to: " + endPos);
        }
    }
    
    /**
     * Sets the player's animation state.
     * 
     * @param state The new animation state
     */
    public void setAnimationState(AnimationState state) {
        if (currentState != state) {
            currentState = state;
            stateTime = 0;
        }
    }
    
    /**
     * Renders the player.
     * 
     * @param modelBatch The model batch to render with
     * @param environment The lighting environment
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(modelInstance, environment);
    }
    
    /**
     * Gets the player's current position.
     * 
     * @return The player's position
     */
    public Vector3 getPosition() {
        return position;
    }
    
    /**
     * Sets the player's position.
     * 
     * @param position The new position
     */
    public void setPosition(Vector3 position) {
        this.position = position;
        resetVelocity();
        updateModelPosition();
    }
    
    /**
     * Resets the player's velocity and movement state.
     * Used during respawn to ensure clean physics state.
     */
    public void resetVelocity() {
        this.velocity.set(0, 0, 0);
        this.isJumping = false;
        this.isDashing = false;
        this.jumpCooldown = 0;
        this.dashCooldown = 0;
    }
    
    /**
     * Gets the player's current velocity.
     * 
     * @return The player's velocity
     */
    public Vector3 getVelocity() {
        return velocity;
    }
    
    /**
     * Gets the player's current animation state.
     * 
     * @return The current animation state
     */
    public AnimationState getCurrentState() {
        return currentState;
    }
    
    /**
     * Disposes of resources used by the player.
     */
    public void dispose() {
        model.dispose();
    }
}