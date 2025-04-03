package io.github.tavuc.platformer.effects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;

/**
 * A class to manage particle effects in the game.
 * Handles creating, updating, and rendering particles.
 */
public class ParticleEffect implements Disposable {
    
    /**
     * Represents a single particle in the effect system.
     */
    private class Particle implements Pool.Poolable {
        ModelInstance modelInstance;
        Vector3 position = new Vector3();
        Vector3 velocity = new Vector3();
        Vector3 scale = new Vector3(1, 1, 1);
        float rotation = 0;
        float rotationSpeed = 0;
        float lifetime = 1.0f;
        float alpha = 1.0f;
        float timeAlive = 0;
        boolean isActive = false;
        
        public Particle() {}
        
        /**
         * Resets the particle to its default state when returned to the pool.
         */
        @Override
        public void reset() {
            position.set(0, 0, 0);
            velocity.set(0, 0, 0);
            scale.set(1, 1, 1);
            rotation = 0;
            rotationSpeed = 0;
            lifetime = 1.0f;
            alpha = 1.0f;
            timeAlive = 0;
            isActive = false;
            modelInstance = null;
        }
        
        /**
         * Updates the particle's position, rotation, and alpha based on elapsed time.
         *
         * @param delta The time in seconds since the last update
         * @return Whether the particle is still active
         */
        public boolean update(float delta) {
            if (!isActive) return false;
            
            timeAlive += delta;
            if (timeAlive >= lifetime) {
                isActive = false;
                return false;
            }
            
            // Update position based on velocity
            position.add(
                velocity.x * delta,
                velocity.y * delta,
                velocity.z * delta
            );
            
            // Update rotation
            rotation += rotationSpeed * delta;
            
            // Update alpha for fade out
            alpha = 1.0f - (timeAlive / lifetime);
            
            // Update the transform
            Matrix4 transform = modelInstance.transform;
            transform.setToTranslation(position);
            transform.rotate(0, 1, 0, rotation);
            transform.scale(scale.x, scale.y, scale.z);
            
            // Update material alpha
            Material material = modelInstance.materials.get(0);
            BlendingAttribute blendAttr = (BlendingAttribute) material.get(BlendingAttribute.Type);
            if (blendAttr != null) {
                blendAttr.opacity = alpha;
            }
            
            return true;
        }
    }
    
    // Particle pool for efficient memory management
    private final Pool<Particle> particlePool = new Pool<Particle>() {
        @Override
        protected Particle newObject() {
            return new Particle();
        }
    };
    
    private final Array<Particle> activeParticles = new Array<>();
    private final Model particleModel;
    
    /**
     * Creates a new particle effect system.
     */
    public ParticleEffect() {
        // Create a simple cube model for the particles
        ModelBuilder modelBuilder = new ModelBuilder();
        particleModel = modelBuilder.createBox(
            1f, 1f, 1f,
            new Material(
                ColorAttribute.createDiffuse(Color.WHITE),
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 1.0f)
            ),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
    }
    
    /**
     * Creates a dust particle effect at the specified position.
     *
     * @param position The position to spawn the dust at
     * @param color The color of the dust particles
     * @param count The number of particles to spawn
     */
    public void createDustEffect(Vector3 position, Color color, int count) {
        for (int i = 0; i < count; i++) {
            Particle particle = particlePool.obtain();
            particle.modelInstance = new ModelInstance(particleModel);
            
            // Clone the position vector to avoid reference issues
            particle.position.set(position);
            
            // Add a small random offset
            particle.position.add(
                MathUtils.random(-0.5f, 0.5f),
                MathUtils.random(0.0f, 0.2f),
                MathUtils.random(-0.5f, 0.5f)
            );
            
            // Set a small upward and outward velocity
            particle.velocity.set(
                MathUtils.random(-1.0f, 1.0f),
                MathUtils.random(0.5f, 1.5f),
                MathUtils.random(-1.0f, 1.0f)
            );
            
            // Set a random scale (small)
            float scale = MathUtils.random(0.1f, 0.3f);
            particle.scale.set(scale, scale, scale);
            
            // Set a random rotation and rotation speed
            particle.rotation = MathUtils.random(0, 360);
            particle.rotationSpeed = MathUtils.random(-90, 90);
            
            // Set a random lifetime
            particle.lifetime = MathUtils.random(0.5f, 1.0f);
            
            // Set the color
            Material material = particle.modelInstance.materials.get(0);
            material.set(ColorAttribute.createDiffuse(color));
            
            // Activate the particle
            particle.isActive = true;
            
            // Add to active particles
            activeParticles.add(particle);
        }
    }
    
    /**
     * Creates a stretched particle effect at the specified position.
     * This is useful for effects like wall running that need elongated particles.
     *
     * @param position The position to spawn the particles at
     * @param direction The direction to stretch the particles in
     * @param color The color of the particles
     * @param lifetime The lifetime of the particles
     */
    public void createStretchedEffect(Vector3 position, Vector3 direction, Color color, float lifetime) {
        Particle particle = particlePool.obtain();
        particle.modelInstance = new ModelInstance(particleModel);
        
        // Clone the position vector to avoid reference issues
        particle.position.set(position);
        
        // Set velocity to move slightly in the direction of the stretch
        particle.velocity.set(
            direction.x * MathUtils.random(0.5f, 1.5f),
            MathUtils.random(0.2f, 0.5f), // Always some upward drift
            direction.z * MathUtils.random(0.5f, 1.5f)
        );
        
        // Set stretched scale - longer in the direction of movement
        float baseScale = MathUtils.random(0.15f, 0.3f);
        float stretchFactor = 3.0f; // How much to stretch
        
        if (Math.abs(direction.z) > Math.abs(direction.x)) {
            // Stretching in Z direction
            particle.scale.set(baseScale, baseScale, baseScale * stretchFactor);
        } else {
            // Stretching in X direction
            particle.scale.set(baseScale * stretchFactor, baseScale, baseScale);
        }
        
        // Set rotation to match direction
        if (direction.z != 0) {
            particle.rotation = direction.z > 0 ? 0 : 180;
        } else if (direction.x != 0) {
            particle.rotation = direction.x > 0 ? 90 : 270;
        }
        
        // Minimal rotation speed for stretched particles
        particle.rotationSpeed = MathUtils.random(-20, 20);
        
        // Set lifetime
        particle.lifetime = lifetime;
        
        // Set the color
        Material material = particle.modelInstance.materials.get(0);
        material.set(ColorAttribute.createDiffuse(color));
        
        // Activate the particle
        particle.isActive = true;
        
        // Add to active particles
        activeParticles.add(particle);
    }
    
    /**
     * Creates a trail effect along a line with the specified parameters.
     *
     * @param start The start position of the trail
     * @param end The end position of the trail
     * @param color The color of the trail particles
     * @param count The number of particles to spawn
     */
    public void createTrailEffect(Vector3 start, Vector3 end, Color color, int count) {
        Vector3 direction = new Vector3(end).sub(start);
        float length = direction.len();
        direction.nor();
        
        for (int i = 0; i < count; i++) {
            Particle particle = particlePool.obtain();
            particle.modelInstance = new ModelInstance(particleModel);
            
            // Position along the line
            float t = i / (float) count;
            particle.position.set(start).mulAdd(direction, t * length);
            
            // Add a small random offset perpendicular to the direction
            Vector3 perpendicular = new Vector3(
                MathUtils.random(-0.5f, 0.5f),
                MathUtils.random(-0.5f, 0.5f),
                MathUtils.random(-0.5f, 0.5f)
            );
            // Make sure perpendicular is actually perpendicular
            perpendicular.sub(direction.scl(perpendicular.dot(direction)));
            perpendicular.nor().scl(MathUtils.random(0.1f, 0.3f));
            particle.position.add(perpendicular);
            
            // Set a small random velocity
            particle.velocity.set(
                perpendicular.x * 0.5f,
                MathUtils.random(0.1f, 0.5f),
                perpendicular.z * 0.5f
            );
            
            // Set a random scale (small)
            float scale = MathUtils.random(0.2f, 0.4f);
            particle.scale.set(scale, scale, scale);
            
            // Set a random rotation and rotation speed
            particle.rotation = MathUtils.random(0, 360);
            particle.rotationSpeed = MathUtils.random(-45, 45);
            
            // Set a random lifetime
            particle.lifetime = MathUtils.random(0.3f, 0.7f);
            
            // Set the color
            Material material = particle.modelInstance.materials.get(0);
            material.set(ColorAttribute.createDiffuse(color));
            
            // Activate the particle
            particle.isActive = true;
            
            // Add to active particles
            activeParticles.add(particle);
        }
    }
    
    /**
     * Creates a jump effect at the specified position.
     *
     * @param position The position to spawn the jump effect at
     * @param up Whether this is a jump-up (true) or landing (false) effect
     */
    public void createJumpEffect(Vector3 position, boolean up) {
        Color color = up ? new Color(0.9f, 0.9f, 1.0f, 1.0f) : new Color(0.8f, 0.8f, 0.7f, 1.0f);
        int count = up ? 12 : 15;
        
        for (int i = 0; i < count; i++) {
            Particle particle = particlePool.obtain();
            particle.modelInstance = new ModelInstance(particleModel);
            
            // Clone the position vector to avoid reference issues
            particle.position.set(position);
            
            // Add a small random offset in a circle
            float angle = MathUtils.random(360) * MathUtils.degreesToRadians;
            float radius = MathUtils.random(0.2f, 0.5f);
            particle.position.add(
                MathUtils.cos(angle) * radius,
                0,
                MathUtils.sin(angle) * radius
            );
            
            // Set velocity based on whether jumping up or landing
            if (up) {
                // For jump-up, particles mostly go upward
                particle.velocity.set(
                    MathUtils.cos(angle) * MathUtils.random(0.5f, 1.0f),
                    MathUtils.random(2.0f, 3.0f),
                    MathUtils.sin(angle) * MathUtils.random(0.5f, 1.0f)
                );
            } else {
                // For landing, particles go outward in a circle
                particle.velocity.set(
                    MathUtils.cos(angle) * MathUtils.random(1.0f, 2.0f),
                    MathUtils.random(0.5f, 1.5f),
                    MathUtils.sin(angle) * MathUtils.random(1.0f, 2.0f)
                );
            }
            
            // Set a random scale
            float scale = MathUtils.random(0.15f, 0.35f);
            particle.scale.set(scale, scale, scale);
            
            // Set a random rotation and rotation speed
            particle.rotation = MathUtils.random(0, 360);
            particle.rotationSpeed = MathUtils.random(-60, 60);
            
            // Set a random lifetime
            particle.lifetime = MathUtils.random(0.5f, 1.2f);
            
            // Set the color
            Material material = particle.modelInstance.materials.get(0);
            material.set(ColorAttribute.createDiffuse(color));
            
            // Activate the particle
            particle.isActive = true;
            
            // Add to active particles
            activeParticles.add(particle);
        }
    }
    
    /**
     * Updates all active particles.
     *
     * @param delta The time in seconds since the last update
     */
    public void update(float delta) {
        // Iterate through active particles backwards to safely remove
        for (int i = activeParticles.size - 1; i >= 0; i--) {
            Particle particle = activeParticles.get(i);
            boolean stillActive = particle.update(delta);
            
            if (!stillActive) {
                activeParticles.removeIndex(i);
                particlePool.free(particle);
            }
        }
    }
    
    /**
     * Renders all active particles.
     *
     * @param modelBatch The model batch to render with
     * @param environment The lighting environment
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        for (Particle particle : activeParticles) {
            if (particle.isActive) {
                modelBatch.render(particle.modelInstance, environment);
            }
        }
    }
    
    /**
     * Disposes of resources used by the particle effect.
     */
    @Override
    public void dispose() {
        if (particleModel != null) {
            particleModel.dispose();
        }
        
        // Clear all active particles
        for (Particle particle : activeParticles) {
            particlePool.free(particle);
        }
        activeParticles.clear();
    }
}