package io.github.tavuc.platformer.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.GL20;
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
 */
public class Player {
    public enum AnimationState {
        IDLE, 
        WALKING, 
        JUMPING, 
        DASHING, 
        ATTACKING, 
        INVISIBLE,
        WALL_SLIDING
    }
    
    private static final float MOVE_SPEED = 15f;
    private static final float JUMP_FORCE = 40f;
    private static final float DASH_DISTANCE = 25f;
    
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private static final float DASH_COOLDOWN = 1.5f;
    private static final float JUMP_COOLDOWN = 0.2f;
    
    private final Logger logger;
    private final Model model;
    private final ModelInstance modelInstance;
    
    private Vector3 position;
    private Vector3 velocity;
    private Vector3 acceleration;
    
    private AnimationState currentState;
    private AnimationState previousState;
    private float stateTime;
    
    private boolean isJumping;
    private boolean isDashing;
    private boolean wasGrounded;
    private float jumpCooldown;
    private float dashCooldown;
    
    private float idleAnimTime;
    private float bounceOffset;
    
    // In our design, we only use WALL_SLIDING.
    // When movement keys are pressed during wall sliding, horizontal velocity is applied and downward velocity is canceled.
    // We'll log "Wall run active" once when that happens.
    private boolean isWallRunning;
    private Vector3 wallNormal;
    
    private EffectsManager effectsManager;
    
    /**
     * Creates a new player.
     *
     * @param position The initial position.
     * @param logger The logger instance.
     * @param effectsManager The effects manager.
     */
    public Player(Vector3 position, Logger logger, EffectsManager effectsManager) {
        this.position = position;
        this.logger = logger;
        this.effectsManager = effectsManager;
        this.velocity = new Vector3(0, 0, 0);
        this.acceleration = new Vector3(0, -40f, 0);
        this.currentState = AnimationState.IDLE;
        this.previousState = AnimationState.IDLE;
        this.stateTime = 0;
        this.isJumping = false;
        this.isDashing = false;
        this.wasGrounded = true;
        this.jumpCooldown = 0;
        this.dashCooldown = 0;
        this.isWallRunning = false;
        this.wallNormal = new Vector3();
        this.idleAnimTime = 0;
        this.bounceOffset = 0;
        if (this.effectsManager != null) {
            this.effectsManager.setInitialPosition(position);
        }
        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(
            PlatformerGame.PLAYER_SIZE,
            PlatformerGame.PLAYER_SIZE, 
            PlatformerGame.PLAYER_SIZE,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        model.materials.get(0).set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        model.materials.get(0).set(new com.badlogic.gdx.graphics.g3d.attributes.IntAttribute(
            com.badlogic.gdx.graphics.g3d.attributes.IntAttribute.CullFace, GL20.GL_NONE));
        modelInstance = new ModelInstance(model);
        updateModelPosition();
        logger.info("Player created at position: " + position);
    }

    /**
     * Checks if the player is currently jumping.
     *
     * @return True if jumping.
     */
    public boolean isJumping() {
        return isJumping;
    }

    /**
     * Checks if the player is moving left.
     *
     * @return True if moving left.
     */
    public boolean isMovingLeft() {
        return movingLeft;
    }

    /**
     * Checks if the player is moving right.
     *
     * @return True if moving right.
     */
    public boolean isMovingRight() {
        return movingRight;
    }

    /**
     * Checks if the player is moving up.
     *
     * @return True if moving up.
     */
    public boolean isMovingUp() {
        return movingUp;
    }

    /**
     * Checks if the player is moving down.
     *
     * @return True if moving down.
     */
    public boolean isMovingDown() {
        return movingDown;
    }

    /**
     * Sets the wall normal.
     *
     * @param normal The wall normal vector.
     */
    public void setWallNormal(Vector3 normal) {
        this.wallNormal = normal;
    }

    /**
     * Performs a wall jump.
     */
    public void wallJump() {
        if (currentState != AnimationState.WALL_SLIDING || wallNormal == null) {
            return;
        }
        isJumping = true;
        isWallRunning = false;
        Vector3 jumpDir = new Vector3(wallNormal).nor();
        // Increase horizontal boost for wall jump
        velocity.x = jumpDir.x * 40.0f;
        velocity.y = 25.0f;
        velocity.z = jumpDir.z * 40.0f;
        setAnimationState(AnimationState.JUMPING);
        jumpCooldown = JUMP_COOLDOWN;
        logger.info("Player performed wall jump at position: " + position);
    }

    /**
     * Updates the player's state and position.
     *
     * @param delta Time since the last frame.
     */
    public void update(float delta) {
        previousState = currentState;
        boolean isGrounded = !isJumping;
        stateTime += delta;
        if (jumpCooldown > 0) {
            jumpCooldown -= delta;
        }
        if (dashCooldown > 0) {
            dashCooldown -= delta;
        }
        if (!isDashing) {
            if (currentState == AnimationState.WALL_SLIDING) {
                // Apply horizontal movement based on input while on the wall:
                if (movingLeft) {
                    velocity.x = -MOVE_SPEED;
                } else if (movingRight) {
                    velocity.x = MOVE_SPEED;
                } else {
                    velocity.x *= 0.9f;
                }
                if (movingUp) {
                    velocity.z = -MOVE_SPEED;
                } else if (movingDown) {
                    velocity.z = MOVE_SPEED;
                } else {
                    velocity.z *= 0.9f;
                }
                // Cancel any downward vertical velocity
                if (velocity.y < 0) {
                    velocity.y = 0;
                }
                if (!isWallRunning && (movingLeft || movingRight || movingUp || movingDown)) {
                    isWallRunning = true;
                    logger.info("Wall run active");
                }
            } else {
                velocity.add(acceleration.x * delta, acceleration.y * delta, acceleration.z * delta);
            }
        }
        if (isJumping) {
            // Allow horizontal input while jumping:
            if (movingLeft) {
                velocity.x = -MOVE_SPEED;
            } else if (movingRight) {
                velocity.x = MOVE_SPEED;
            } else {
                velocity.x *= 0.9f;
            }
            if (movingUp) {
                velocity.z = -MOVE_SPEED;
            } else if (movingDown) {
                velocity.z = MOVE_SPEED;
            } else {
                velocity.z *= 0.9f;
            }
        }
        position.add(velocity.x * delta, velocity.y * delta, velocity.z * delta);
        updateAnimation(delta);
        updateModelPosition();
        if (effectsManager != null) {
            boolean isWalking = currentState == AnimationState.WALKING;
            effectsManager.createWalkDustEffect(position, isWalking);
            effectsManager.createDashTrailEffect(position, isDashing);
            if (previousState != AnimationState.JUMPING && currentState == AnimationState.JUMPING) {
                effectsManager.createJumpEffect(position, true);
            }
            if (!wasGrounded && isGrounded) {
                effectsManager.createJumpEffect(position, false);
            }
        }
        wasGrounded = isGrounded;
    }

    /**
     * Updates the player's animation based on state.
     *
     * @param delta Time since the last frame.
     */
    private void updateAnimation(float delta) {
        switch (currentState) {
            case IDLE:
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
     * Updates the model's position.
     */
    private void updateModelPosition() {
        Vector3 modelPos = new Vector3(position);
        if (currentState == AnimationState.IDLE) {
            modelPos.y += bounceOffset;
        }
        modelInstance.transform.setToTranslation(modelPos);
    }

    /**
     * Moves the player.
     *
     * @param dirX X direction.
     * @param dirY Y direction.
     * @param delta Time since the last frame.
     */
    public void move(float dirX, float dirY, float delta) {
        if (isDashing || isJumping) {
            return;
        }
        position.x += dirX * MOVE_SPEED * delta;
        position.z += dirY * MOVE_SPEED * delta;
    }

    /**
     * Makes the player jump.
     */
    public void jump() {
        if (currentState == AnimationState.WALL_SLIDING) {
            wallJump();
            return;
        }
        if (!isJumping && jumpCooldown <= 0) {
            isJumping = true;
            velocity.y = JUMP_FORCE;
            jumpCooldown = JUMP_COOLDOWN;
            setAnimationState(AnimationState.JUMPING);
            logger.info("Player jumped at position: " + position);
        }
    }

    /**
     * Sets movement flags.
     *
     * @param up True if up.
     * @param down True if down.
     * @param left True if left.
     * @param right True if right.
     */
    public void setMovementFlags(boolean up, boolean down, boolean left, boolean right) {
        this.movingUp = up;
        this.movingDown = down;
        this.movingLeft = left;
        this.movingRight = right;
    }

    /**
     * Makes the player dash.
     *
     * @param dirX X direction.
     * @param dirY Y direction.
     */
    public void dash(float dirX, float dirY) {
        if (!isDashing && dashCooldown <= 0) {
            isDashing = true;
            float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
            if (length != 0) {
                dirX /= length;
                dirY /= length;
            }
            final Vector3 startPos = new Vector3(position);
            final Vector3 endPos = new Vector3(
                position.x + dirX * DASH_DISTANCE,
                position.y,
                position.z + dirY * DASH_DISTANCE
            );
            new Thread(() -> {
                try {
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
     * @param state The new animation state.
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
     * @param modelBatch The model batch.
     * @param environment The lighting environment.
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(modelInstance, environment);
    }

    /**
     * Gets the player's position.
     *
     * @return The position.
     */
    public Vector3 getPosition() {
        return position;
    }

    /**
     * Sets the player's position.
     *
     * @param position The new position.
     */
    public void setPosition(Vector3 position) {
        this.position = position;
        resetVelocity();
        updateModelPosition();
    }

    /**
     * Resets the player's velocity.
     */
    public void resetVelocity() {
        this.velocity.set(0, 0, 0);
        this.isJumping = false;
        this.isDashing = false;
        this.jumpCooldown = 0;
        this.dashCooldown = 0;
    }

    /**
     * Sets the player's velocity.
     *
     * @param velocity The new velocity.
     */
    public void setVelocity(Vector3 velocity) {
        this.velocity = velocity;
    }

    /**
     * Gets the player's current animation state.
     *
     * @return The animation state.
     */
    public AnimationState getCurrentState() {
        return currentState;
    }

    /**
     * Gets the player's velocity.
     *
     * @return The velocity.
     */
    public Vector3 getVelocity() {
        return velocity;
    }

    /**
     * Disposes resources used by the player.
     */
    public void dispose() {
        model.dispose();
    }
}
