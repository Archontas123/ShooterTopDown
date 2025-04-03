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
import io.github.tavuc.platformer.effects.EffectsManager;

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
        INVISIBLE,
        WALL_SLIDING,
        WALL_RUNNING  // State for wall running
    }
    
    private static final float MOVE_SPEED = 15f; // Faster movement for larger world
    private static final float JUMP_FORCE = 40f; // Doubled jump height (was 10f)
    private static final float DASH_DISTANCE = 25f; // Longer dash (5 tiles as per requirements)
    private static final float WALL_SLIDE_SPEED = -5f; // Speed of sliding down a wall
    private static final float WALL_JUMP_HORIZONTAL_FORCE = 15f; // Force away from wall
    private static final float WALL_JUMP_VERTICAL_FORCE = 18f; // Upward force
    private static final float WALL_RUN_SPEED = 12f; // Speed of running along a wall
    private static final float WALL_RUN_DURATION = 1.5f; // Maximum time player can wall run
    private static final float WALL_RUN_GRAVITY = -5f; // Reduced gravity while wall running
    
    // Movement control flags
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private static final float DASH_COOLDOWN = 1.5f;
    private static final float JUMP_COOLDOWN = 0.2f; // Further reduced for better responsiveness
    
    private final Logger logger;
    private final Model model;
    private final ModelInstance modelInstance;
    
    private Vector3 position;
    private Vector3 velocity;
    private Vector3 acceleration;
    
    private AnimationState currentState;
    private AnimationState previousState; // Added to track state changes
    private float stateTime;
    
    private boolean isJumping;
    private boolean isDashing;
    private boolean wasGrounded; // Added to detect landing
    private boolean isAgainstWall; // Added for wall sliding
    private int wallDirection; // Direction of the wall the player is against
    private Vector3 wallNormal; // Normal vector of the wall
    private boolean isWallRunning; // Whether the player is wall running
    private float wallRunTimer; // Time remaining for wall running
    private float wallRunDirection; // 1 for right, -1 for left
    private float jumpCooldown;
    private float dashCooldown;
    
    private float idleAnimTime;
    private float bounceOffset;
    
    // Add reference to effects manager
    private EffectsManager effectsManager;
    
    /**
     * Creates a new player at the specified position.
     * 
     * @param position The initial position
     * @param logger The logger instance
     * @param effectsManager The effects manager for visual effects
     */
    public Player(Vector3 position, Logger logger, EffectsManager effectsManager) {
        this.position = position;
        this.logger = logger;
        this.effectsManager = effectsManager;
        this.velocity = new Vector3(0, 0, 0);
        this.acceleration = new Vector3(0, -40f, 0); // Doubled gravity for snappier jumps
        
        this.currentState = AnimationState.IDLE;
        this.previousState = AnimationState.IDLE;
        this.stateTime = 0;
        
        this.isJumping = false;
        this.isDashing = false;
        this.wasGrounded = true;
        this.isAgainstWall = false;
        this.wallDirection = 0;
        this.wallNormal = new Vector3();
        this.isWallRunning = false;
        this.wallRunTimer = 0;
        this.wallRunDirection = 0;
        this.jumpCooldown = 0;
        this.dashCooldown = 0;
        
        this.idleAnimTime = 0;
        this.bounceOffset = 0;
        
        // Initialize effects manager with current position
        if (this.effectsManager != null) {
            this.effectsManager.setInitialPosition(position);
        }
        
        // Create player model with original size
        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(
            PlatformerGame.PLAYER_SIZE, // Back to original size
            PlatformerGame.PLAYER_SIZE, 
            PlatformerGame.PLAYER_SIZE,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)), // Back to original white color
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        
        // Original material settings
        model.materials.get(0).set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        
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
        // Save the previous state for comparison
        previousState = currentState;
        
        // Save whether player was on the ground
        boolean isGrounded = !isJumping;
        
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
            // Check for wall running conditions
            if (isAgainstWall && !isGrounded) {
                if (currentState == AnimationState.WALL_SLIDING) {
                    // When wall sliding, reduce falling speed
                    velocity.y = WALL_SLIDE_SPEED;
                    
                    // Zero out horizontal velocity in wall normal direction to prevent 
                    // drifting away from the wall
                    if (wallDirection == 1 || wallDirection == 2) { // Left/right walls
                        velocity.x = 0;
                    } else if (wallDirection == 3 || wallDirection == 4) { // Front/back walls
                        velocity.z = 0;
                    }
                    
                    // If player is moving (pressing any movement key), transition to wall running
                    if (movingUp || movingDown) {
                        isWallRunning = true;
                        wallRunTimer = WALL_RUN_DURATION;
                        
                        // Set wall run direction based on which wall we're on
                        wallRunDirection = (wallDirection == 1) ? -1 : 1;
                        
                        setAnimationState(AnimationState.WALL_RUNNING);
                        logger.info("Player started wall running on wall: " + wallDirection);
                    }
                }
                // Apply wall running physics
                else if (currentState == AnimationState.WALL_RUNNING) {
                    // Stop vertical movement completely while wall running
                    velocity.y = 0;
                    
                    // Zero out horizontal velocity in wall normal direction
                    if (wallDirection == 1 || wallDirection == 2) { // Left/right walls
                        velocity.x = 0;
                    } else if (wallDirection == 3 || wallDirection == 4) { // Front/back walls
                        velocity.z = 0;
                    }
                    
                    // Move forward/backward along the wall
                    if (movingUp) {
                        velocity.z = -WALL_RUN_SPEED;
                    } else if (movingDown) {
                        velocity.z = WALL_RUN_SPEED;
                    } else {
                        velocity.z *= 0.9f; // Slow down if no input
                        
                        // If no forward/backward movement keys pressed, transition back to wall sliding
                        if (Math.abs(velocity.z) < 2.0f) {
                            isWallRunning = false;
                            setAnimationState(AnimationState.WALL_SLIDING);
                            logger.info("Player returned to wall sliding");
                        }
                    }
                    
                    // Decrease wall run timer
                    wallRunTimer -= delta;
                    if (wallRunTimer <= 0) {
                        isWallRunning = false;
                        setAnimationState(AnimationState.JUMPING);
                    }
                }
            } else if (isWallRunning) {
                // End wall running if no longer against wall
                isWallRunning = false;
                setAnimationState(AnimationState.JUMPING);
            } else {
                // Apply normal acceleration (gravity) to velocity
                velocity.add(
                    acceleration.x * delta,
                    acceleration.y * delta,
                    acceleration.z * delta
                );
            }
            
            // If jumping, apply horizontal movement based on input flags
            if (isJumping && !isWallRunning) {
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
        
        // Handle effects based on state transitions
        if (effectsManager != null) {
            // Walking dust effect
            boolean isWalking = currentState == AnimationState.WALKING;
            effectsManager.createWalkDustEffect(position, isWalking);
            
            // Dash trail effect
            effectsManager.createDashTrailEffect(position, isDashing);
            
            // Jump effect on takeoff
            if (previousState != AnimationState.JUMPING && currentState == AnimationState.JUMPING) {
                effectsManager.createJumpEffect(position, true);
            }
            
            // Landing effect
            if (wasGrounded == false && isGrounded == true) {
                effectsManager.createJumpEffect(position, false);
            }
            
            // Wall slide effect
            if (currentState == AnimationState.WALL_SLIDING) {
                // Add wall slide dust effect
                createWallSlideEffect();
            }
            
            // Wall run effect
            if (currentState == AnimationState.WALL_RUNNING) {
                // Add wall run particle effect
                createWallRunEffect();
            }
        }
        
        // Update grounded state for next frame
        wasGrounded = isGrounded;
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
                
            case WALL_RUNNING:
                // Add some visual tilt while wall running
                // This would be expanded in a real animation system
                break;
                
            default:
                break;
        }
    }
    
    /**
     * Updates the model's position to match the player's position.
     */
    private void updateModelPosition() {
        // Start with base position
        Vector3 modelPos = new Vector3(position);
        
        // Add bounce offset for idle animation
        if (currentState == AnimationState.IDLE) {
            modelPos.y += bounceOffset;
        }
        
        // Set the transform
        modelInstance.transform.setToTranslation(modelPos);
        
        // Add slight rotation for wall running
        if (currentState == AnimationState.WALL_RUNNING) {
            // Rotate the model slightly toward the wall
            if (wallDirection == 1) { // Left wall
                modelInstance.transform.rotate(0, 0, 1, 15f);
            } else if (wallDirection == 2) { // Right wall
                modelInstance.transform.rotate(0, 0, 1, -15f);
            }
        }
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
     * Also handles wall jumping when against a wall.
     */
    public void jump() {
        // Handle wall jump if against a wall and wall sliding or running
        if (isAgainstWall && 
            (currentState == AnimationState.WALL_SLIDING || 
             currentState == AnimationState.WALL_RUNNING) && 
            jumpCooldown <= 0) {
            
            wallJump();
            return;
        }
        
        // Normal jump when on ground
        if (!isJumping && jumpCooldown <= 0) {
            isJumping = true;
            velocity.y = JUMP_FORCE; // Purely vertical force
            jumpCooldown = JUMP_COOLDOWN;
            setAnimationState(AnimationState.JUMPING);
            logger.info("Player jumped at position: " + position);
        }
    }
    
    /**
     * Makes the player perform a wall jump if against a wall.
     */
    public void wallJump() {
        // Allow wall jumping if wall sliding OR wall running
        if (isAgainstWall && 
            (currentState == AnimationState.WALL_SLIDING || 
             currentState == AnimationState.WALL_RUNNING) && 
            jumpCooldown <= 0) {
            
            // Calculate jump direction (away from wall)
            Vector3 jumpDirection = new Vector3(wallNormal);
            
            // Apply stronger horizontal force away from the wall
            velocity.x = jumpDirection.x * WALL_JUMP_HORIZONTAL_FORCE * 1.2f;
            
            // Keep some Z momentum from wall running
            if (currentState == AnimationState.WALL_RUNNING) {
                velocity.z = velocity.z * 0.7f; // Preserve some forward momentum
            } else {
                velocity.z = jumpDirection.z * WALL_JUMP_HORIZONTAL_FORCE;
            }
            
            // Apply vertical force upward (stronger for better jumps)
            velocity.y = WALL_JUMP_VERTICAL_FORCE * 1.1f;
            
            // Set to jumping state
            isJumping = true;
            isWallRunning = false; // End wall running
            setAnimationState(AnimationState.JUMPING);
            
            // Reset cooldown
            jumpCooldown = JUMP_COOLDOWN;
            
            // Create jump effect
            if (effectsManager != null) {
                effectsManager.createJumpEffect(position, true);
            }
            
            logger.info("Player wall-jumped from wall direction: " + wallDirection);
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
     * Creates wall slide particle effects.
     */
    private void createWallSlideEffect() {
        if (stateTime % 0.2f < 0.05f) { // Create particles periodically
            // Create a position slightly offset from the player
            Vector3 effectPos = new Vector3(position);
            
            // Offset based on wall direction
            switch (wallDirection) {
                case 1: // Left wall
                    effectPos.x -= PlatformerGame.PLAYER_SIZE * 0.6f;
                    break;
                case 2: // Right wall
                    effectPos.x += PlatformerGame.PLAYER_SIZE * 0.6f;
                    break;
                case 3: // Front wall
                    effectPos.z -= PlatformerGame.PLAYER_SIZE * 0.6f;
                    break;
                case 4: // Back wall
                    effectPos.z += PlatformerGame.PLAYER_SIZE * 0.6f;
                    break;
            }
            
            // Create small dust particles
            if (effectsManager != null) {
                effectsManager.createWallSlideEffect(effectPos, wallNormal);
            }
        }
    }
    
    /**
     * Creates wall run particle effects.
     */
    private void createWallRunEffect() {
        // Create wall run particles more frequently than wall slide
        if (stateTime % 0.1f < 0.05f) {
            // Create a position slightly offset from the player
            Vector3 effectPos = new Vector3(position);
            
            // Offset based on wall direction
            switch (wallDirection) {
                case 1: // Left wall
                    effectPos.x -= PlatformerGame.PLAYER_SIZE * 0.6f;
                    break;
                case 2: // Right wall
                    effectPos.x += PlatformerGame.PLAYER_SIZE * 0.6f;
                    break;
            }
            
            // Create horizontal streak particles
            if (effectsManager != null) {
                effectsManager.createWallRunEffect(effectPos, wallNormal, velocity.z);
            }
        }
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
     * Sets whether the player is against a wall and the direction of the wall.
     * 
     * @param againstWall Whether the player is against a wall
     * @param direction Direction of the wall (0=none, 1=left, 2=right, 3=front, 4=back)
     * @param normal Normal vector of the wall
     */
    public void setAgainstWall(boolean againstWall, int direction, Vector3 normal) {
        this.isAgainstWall = againstWall;
        this.wallDirection = direction;
        
        if (normal != null) {
            this.wallNormal.set(normal);
        } else {
            this.wallNormal.setZero();
        }
        
        // If no longer against a wall, reset wall sliding and running states
        if (!againstWall) {
            if (currentState == AnimationState.WALL_SLIDING) {
                setAnimationState(AnimationState.JUMPING);
            }
            
            if (isWallRunning) {
                isWallRunning = false;
                setAnimationState(AnimationState.JUMPING);
            }
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
        this.isWallRunning = false;
        this.jumpCooldown = 0;
        this.dashCooldown = 0;
    }
    
    /**
     * Sets the player's velocity directly.
     * Used for immediate velocity changes like respawn.
     * 
     * @param velocity The new velocity
     */
    public void setVelocity(Vector3 velocity) {
        this.velocity = velocity;
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
     * Gets the player's current velocity.
     * 
     * @return The player's velocity
     */
    public Vector3 getVelocity() {
        return velocity;
    }
    
    /**
     * Checks if the player is currently wall running.
     * 
     * @return True if the player is wall running
     */
    public boolean isWallRunning() {
        return isWallRunning;
    }
    
    /**
     * Disposes of resources used by the player.
     */
    public void dispose() {
        model.dispose();
    }
}