package io.github.tavuc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;

/**
 * The main game screen.
 */
public class GameScreen implements Screen {
    // Core game references
    private final ShooterGame game;
    private final GameState gameState;
    private InputHandler inputHandler;
    
    // Camera and environment
    private PerspectiveCamera camera;
    private Environment environment;
    
    // Rendering
    private SpriteBatch spriteBatch;
    private ModelBatch modelBatch;
    private ShapeRenderer shapeRenderer;
    
    // UI
    private Stage stage;
    private Skin skin;
    private Table table;
    private BitmapFont font;
    private boolean gameOver;
    
    // Game objects
    private ModelInstance groundInstance;
    private ModelInstance arenaBoundaryInstance;
    private ModelInstance playerInstance;
    
    // UI elements
    private Label healthLabel;
    private Label ammoLabel;
    private Label weaponLabel;
    private Label grenadeLabel;
    private Label scoreLabel;
    
    // Cooldown UI
    private float dashCooldownHeight;
    private float reloadCooldownHeight;
    private float grenadeCooldownHeight;
    
    // Models
    private Model groundModel;
    private Model arenaModel;
    private Model playerModel;
    private Model enemyModel;
    private Model fastEnemyModel;
    private Model projectileModel;
    private Model explosiveProjectileModel;
    private Model buildingModel;
    private Model explosionModel;
    
    public GameScreen(ShooterGame game) {
        this.game = game;
        this.gameState = game.getGameState();
        this.gameOver = false;
        
        // Initialize rendering
        spriteBatch = game.spriteBatch;
        modelBatch = game.modelBatch;
        shapeRenderer = new ShapeRenderer();
        
        // Create camera
        setupCamera();
        
        // Set up lighting environment
        setupEnvironment();
        
        // Create models
        createModels();
        
        // Set up UI
        setupUI();
        
        // Create input handler
        inputHandler = new InputHandler(game, this);
        
        // Start game
        startGame();
    }
    
    /**
     * Set up UI elements
     */
    private void setupUI() {
        // Create stage for UI
        stage = new Stage(new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        
        // Create simple font
        font = new BitmapFont();
        font.getData().setScale(1.5f);
        
        // Create labels
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        
        healthLabel = new Label("Health: 100%", labelStyle);
        ammoLabel = new Label("Ammo: 12/12", labelStyle);
        weaponLabel = new Label("Weapon: Pistol", labelStyle);
        grenadeLabel = new Label("Grenades: 3", labelStyle);
        scoreLabel = new Label("Score: 0", labelStyle);
        
        // Create layout
        table = new Table();
        table.setFillParent(true);
        table.top().left().pad(20);
        
        table.add(healthLabel).left().padBottom(10).row();
        table.add(weaponLabel).left().padBottom(5).row();
        table.add(ammoLabel).left().padBottom(10).row();
        table.add(grenadeLabel).left().padBottom(10).row();
        table.add(scoreLabel).left().row();
        
        stage.addActor(table);
    }
    
    /**
     * Start a new game
     */
    public void startGame() {
        gameOver = false;
        gameState.startGame();
        updateUI();
    }
    
    /**
     * Restart the game
     */
    public void restartGame() {
        startGame();
    }
    
    /**
     * Update all UI elements
     */
    private void updateUI() {
        Player player = gameState.getPlayer();
        
        // Update labels
        healthLabel.setText("Health: " + player.getHealth() + "%");
        updateWeaponHUD();
        updateGrenadeHUD();
        scoreLabel.setText("Score: " + gameState.getScore());
        
        // Update cooldown displays
        dashCooldownHeight = player.getDashCooldownPercentage();
        reloadCooldownHeight = player.getReloadPercentage();
        grenadeCooldownHeight = player.getGrenadeRefreshPercentage();
    }
    
    /**
     * Update weapon-related HUD elements
     */
    public void updateWeaponHUD() {
        Player player = gameState.getPlayer();
        
        String weaponName = player.getCurrentWeapon().toString().charAt(0) + 
                            player.getCurrentWeapon().toString().substring(1).toLowerCase();
        
        weaponLabel.setText("Weapon: " + weaponName);
        ammoLabel.setText("Ammo: " + player.getCurrentAmmo() + "/" + player.getMaxAmmo());
    }
    
    /**
     * Update grenade-related HUD elements
     */
    public void updateGrenadeHUD() {
        Player player = gameState.getPlayer();
        grenadeLabel.setText("Grenades: " + player.getGrenades());
    }
    
    /**
     * Process game over state
     */
    private void gameOver() {
        gameOver = true;
        System.out.println("GAME OVER! Final Score: " + gameState.getScore());
        System.out.println("Press ESC to restart");
    }
    
    /**
     * Set up the 3D camera
     */
    private void setupCamera() {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(15, 15, 15);
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();
    }
    
    /**
     * Set up the lighting environment
     */
    private void setupEnvironment() {
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
    }
    
    /**
     * Creates a hexagon model with grid pattern and orange border
     * Modified to remove orange triangle under the grid
     */
    private Model createHexagonModel(ModelBuilder modelBuilder, float width, float height, Material material) {
        modelBuilder.begin();
        
        // Calculate vertices for a regular hexagon
        int numVertices = 6;
        float[] hexagonVertices = new float[numVertices * 3];
        float[] outerHexagonVertices = new float[numVertices * 3];
        float borderWidth = ShooterGame.ARENA_BORDER_WIDTH;  // Width of the border
        float borderHeight = 0.15f; // Additional height for the border
        
        for (int i = 0; i < numVertices; i++) {
            float angle = (float) (i * 2 * Math.PI / numVertices);
            // Inner hexagon vertices
            float x = (float) (width / 2 * Math.cos(angle));
            float z = (float) (width / 2 * Math.sin(angle));
            
            hexagonVertices[i * 3] = x;
            hexagonVertices[i * 3 + 1] = 0; // y is always 0 for the top face
            hexagonVertices[i * 3 + 2] = z;
            
            // Outer hexagon vertices (for border)
            float outerX = (float) ((width / 2 + borderWidth) * Math.cos(angle));
            float outerZ = (float) ((width / 2 + borderWidth) * Math.sin(angle));
            
            outerHexagonVertices[i * 3] = outerX;
            outerHexagonVertices[i * 3 + 1] = 0; // y is always 0 for the top face
            outerHexagonVertices[i * 3 + 2] = outerZ;
        }
        
        // Create top face of the hexagon
        MeshPartBuilder builder = modelBuilder.part("top", GL20.GL_TRIANGLES, 
                          Usage.Position | Usage.Normal | Usage.TextureCoordinates, 
                          material);
        
        // Center point
        builder.vertex(0, 0, 0, 0, 1, 0, 0.5f, 0.5f);
        
        // Add all vertices and create triangles (fan)
        for (int i = 0; i < numVertices; i++) {
            float x = hexagonVertices[i * 3];
            float y = hexagonVertices[i * 3 + 1];
            float z = hexagonVertices[i * 3 + 2];
            
            // Calculate texture coordinates
            float u = (x / width) + 0.5f;
            float v = (z / width) + 0.5f;
            
            builder.vertex(x, y, z, 0, 1, 0, u, v);
            
            // Create triangle between center, current vertex and next vertex
            if (i < numVertices - 1) {
                builder.triangle((short)0, (short)(i + 1), (short)(i + 2));
            } else {
                builder.triangle((short)0, (short)(i + 1), (short)1); // Connect back to the first vertex
            }
        }
        
        // Add grid lines
        Material gridMaterial = new Material(ColorAttribute.createDiffuse(new Color(0.3f, 0.3f, 0.3f, 1f)));
        MeshPartBuilder gridBuilder = modelBuilder.part("grid", GL20.GL_LINES, 
                              Usage.Position | Usage.Normal, 
                              gridMaterial);
        
        // Create grid lines (radial)
        for (int i = 0; i < numVertices; i++) {
            gridBuilder.line(0, 0.01f, 0, 
                     hexagonVertices[i * 3], 0.01f, hexagonVertices[i * 3 + 2]);
        }
        
        // Create concentric hexagons for the grid
        int gridRings = 3;
        for (int r = 1; r <= gridRings; r++) {
            float ringScale = (float) r / gridRings;
            
            for (int i = 0; i < numVertices; i++) {
                int nextI = (i + 1) % numVertices;
                float x1 = hexagonVertices[i * 3] * ringScale;
                float z1 = hexagonVertices[i * 3 + 2] * ringScale;
                float x2 = hexagonVertices[nextI * 3] * ringScale;
                float z2 = hexagonVertices[nextI * 3 + 2] * ringScale;
                
                gridBuilder.line(x1, 0.01f, z1, x2, 0.01f, z2);
            }
        }
        
        // Create the orange border (top face)
        Material borderMaterial = new Material(ColorAttribute.createDiffuse(new Color(1.0f, 0.5f, 0.0f, 1.0f))); // Orange
        MeshPartBuilder borderBuilder = modelBuilder.part("border-top", GL20.GL_TRIANGLES, 
                                  Usage.Position | Usage.Normal, 
                                  borderMaterial);
        
        // Create the border top face - connecting inner and outer vertices
        for (int i = 0; i < numVertices; i++) {
            int nextI = (i + 1) % numVertices;
            
            float innerX1 = hexagonVertices[i * 3];
            float innerZ1 = hexagonVertices[i * 3 + 2];
            float innerX2 = hexagonVertices[nextI * 3];
            float innerZ2 = hexagonVertices[nextI * 3 + 2];
            
            float outerX1 = outerHexagonVertices[i * 3];
            float outerZ1 = outerHexagonVertices[i * 3 + 2];
            float outerX2 = outerHexagonVertices[nextI * 3];
            float outerZ2 = outerHexagonVertices[nextI * 3 + 2];
            
            // Create two triangles to form a quad for each section of the border
            // First triangle
            borderBuilder.vertex(innerX1, borderHeight, innerZ1, 0, 1, 0);
            borderBuilder.vertex(outerX1, borderHeight, outerZ1, 0, 1, 0);
            borderBuilder.vertex(innerX2, borderHeight, innerZ2, 0, 1, 0);
            borderBuilder.triangle((short)0, (short)1, (short)2);
            
            // Second triangle
            borderBuilder.vertex(innerX2, borderHeight, innerZ2, 0, 1, 0);
            borderBuilder.vertex(outerX1, borderHeight, outerZ1, 0, 1, 0);
            borderBuilder.vertex(outerX2, borderHeight, outerZ2, 0, 1, 0);
            borderBuilder.triangle((short)3, (short)4, (short)5);
        }
        
        // Create the border outer side faces
        for (int i = 0; i < numVertices; i++) {
            int nextI = (i + 1) % numVertices;
            MeshPartBuilder borderSideBuilder = modelBuilder.part("border-side" + i, GL20.GL_TRIANGLES, 
                                      Usage.Position | Usage.Normal, 
                                      borderMaterial);
            
            float outerX1 = outerHexagonVertices[i * 3];
            float outerZ1 = outerHexagonVertices[i * 3 + 2];
            float outerX2 = outerHexagonVertices[nextI * 3];
            float outerZ2 = outerHexagonVertices[nextI * 3 + 2];
            
            // Create a rectangle for each outer side face
            borderSideBuilder.rect(
                outerX1, borderHeight, outerZ1,          // top-left
                outerX1, -height, outerZ1,              // bottom-left
                outerX2, -height, outerZ2,              // bottom-right
                outerX2, borderHeight, outerZ2,          // top-right
                0, 0, -1                               // normal (facing outward)
            );
        }
        
        // Create the border inner side faces (connecting the border to the main platform)
        for (int i = 0; i < numVertices; i++) {
            int nextI = (i + 1) % numVertices;
            MeshPartBuilder borderInnerSideBuilder = modelBuilder.part("border-inner-side" + i, GL20.GL_TRIANGLES, 
                                          Usage.Position | Usage.Normal, 
                                          borderMaterial);
            
            float innerX1 = hexagonVertices[i * 3];
            float innerZ1 = hexagonVertices[i * 3 + 2];
            float innerX2 = hexagonVertices[nextI * 3];
            float innerZ2 = hexagonVertices[nextI * 3 + 2];
            
            // Create a rectangle for each inner side face (vertical wall of border)
            borderInnerSideBuilder.rect(
                innerX1, borderHeight, innerZ1,         // top-left
                innerX1, 0, innerZ1,                    // bottom-left
                innerX2, 0, innerZ2,                    // bottom-right
                innerX2, borderHeight, innerZ2,         // top-right
                0, 0, 1                                // normal (facing inward)
            );
        }
        
        // Add side faces to give the hexagon some thickness
        for (int i = 0; i < numVertices; i++) {
            int nextI = (i + 1) % numVertices;
            MeshPartBuilder sideBuilder = modelBuilder.part("side" + i, GL20.GL_TRIANGLES, 
                                  Usage.Position | Usage.Normal, 
                                  material);
            
            float x1 = hexagonVertices[i * 3];
            float z1 = hexagonVertices[i * 3 + 2];
            float x2 = hexagonVertices[nextI * 3];
            float z2 = hexagonVertices[nextI * 3 + 2];
            
            // Create a rectangle for each side face
            sideBuilder.rect(
                x1, 0, z1,                // top-left
                x1, -height, z1,          // bottom-left
                x2, -height, z2,          // bottom-right
                x2, 0, z2,                // top-right
                0, 0, -1                  // normal (facing outward)
            );
        }
        
        // MODIFIED: Don't create bottom face to avoid orange triangles
        // We'll create a single solid bottom face to avoid the triangular pattern


        return modelBuilder.end();
    }

    /**
     * Create all 3D models
     */
    private void createModels() {
        try {
            Gdx.app.log("GameScreen", "Creating 3D models...");
            ModelBuilder modelBuilder = new ModelBuilder();
            
            // Create ground model (hexagon instead of cylinder)
            Gdx.app.log("GameScreen", "Creating ground model...");
            groundModel = createHexagonModel(
                modelBuilder, 
                ShooterGame.ARENA_RADIUS * 2, 
                0.1f,
                new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY))
            );
            groundInstance = new ModelInstance(groundModel);
            groundInstance.transform.translate(0, -0.05f, 0);
            Gdx.app.log("GameScreen", "Ground model created successfully");
        
            // Create arena boundary model (hexagon instead of cylinder)
            arenaModel = createHexagonModel(
                modelBuilder, 
                ShooterGame.ARENA_RADIUS * 2, 
                0.1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.6f, 0.6f, 0.6f, 1f)))
            );
            arenaBoundaryInstance = new ModelInstance(arenaModel);
            arenaBoundaryInstance.transform.translate(0, 0.1f, 0);
            
            // Create player model
            playerModel = modelBuilder.createBox(
                0.8f, 1f, 0.8f, 
                new Material(ColorAttribute.createDiffuse(new Color(0x3498dbff))),
                Usage.Position | Usage.Normal
            );
            playerInstance = new ModelInstance(playerModel);
            
            // Create enemy models
            enemyModel = modelBuilder.createBox(
                0.8f, 1f, 0.8f,
                new Material(ColorAttribute.createDiffuse(new Color(0xe74c3cff))),
                Usage.Position | Usage.Normal
            );
            
            fastEnemyModel = modelBuilder.createBox(
                0.8f, 1f, 0.8f,
                new Material(ColorAttribute.createDiffuse(new Color(0xf39c12ff))),
                Usage.Position | Usage.Normal
            );
            
            // Create projectile models
            projectileModel = modelBuilder.createSphere(
                0.3f, 0.3f, 0.3f, 16, 16,
                new Material(ColorAttribute.createDiffuse(Color.YELLOW)),
                Usage.Position | Usage.Normal
            );
            
            explosiveProjectileModel = modelBuilder.createSphere(
                0.5f, 0.5f, 0.5f, 16, 16,
                new Material(ColorAttribute.createDiffuse(new Color(0x2ecc71ff))),
                Usage.Position | Usage.Normal
            );
            
            // Create explosion model
            explosionModel = modelBuilder.createSphere(
                1f, 1f, 1f, 32, 32,
                new Material(
                    ColorAttribute.createDiffuse(new Color(0xff5722ff)),
                    ColorAttribute.createEmissive(new Color(0xff9800ff))
                ),
                Usage.Position | Usage.Normal
            );
            
            // Note: Building models will be created dynamically based on size
            Gdx.app.log("GameScreen", "All models created successfully");
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Failed to create 3D models: " + e.getMessage(), e);
        }
    }

    @Override
    public void render(float delta) {
        try {
            // Clear the screen
            Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    
            // First update game state
            update(delta);
    
            // Try to render 3D elements (but continue even if it fails)
            try {
                renderModels();
            } catch (Exception e) {
                Gdx.app.error("GameScreen", "Failed to render 3D models: " + e.getMessage(), e);
            }
    
            // Render 2D elements
            renderHUD();
            
            // Check for restart input
            if (gameOver && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                restartGame();
            }
            
            // Check for GL errors
            DebugUtils.checkGLError("GameScreen.render");
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Exception in render: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update game state
     */
    private void update(float delta) {
        if (gameOver) return;
        
        // Process input
        inputHandler.update(delta);
        
        // Update player
        Player player = gameState.getPlayer();
        player.update(delta);
        
        // Update player model position and rotation
        playerInstance.transform.setToTranslation(player.getPosition().x, 0.5f, player.getPosition().y);
        playerInstance.transform.rotate(0, 1, 0, (float)Math.toDegrees(player.getRotation()));
        
        // Update camera position to follow player
        updateCameraPosition();
        
        // Update projectiles
        updateProjectiles(delta);
        
        // Update explosions
        updateExplosions(delta);
        
        // Update enemies
        updateEnemies(delta);
        
        // Update UI
        updateUI();
        
        // Check player health
        if (player.getHealth() <= 0 && !gameOver) {
            gameOver();
        }
    }
    
    /**
     * Update camera position to follow player
     */
    private void updateCameraPosition() {
        Vector2 playerPos = gameState.getPlayer().getPosition();
        camera.position.set(playerPos.x + 15, 15, playerPos.y + 15);
        camera.lookAt(playerPos.x, 0, playerPos.y);
        camera.update();
    }
    
    /**
     * Update projectiles
     */
    private void updateProjectiles(float delta) {
        for (int i = gameState.getProjectiles().size - 1; i >= 0; i--) {
            Projectile projectile = gameState.getProjectiles().get(i);
            
            // Update projectile state
            boolean remove = projectile.update(delta, gameState);
            
            if (remove) {
                // Check if explosive for explosion effect
                if (projectile.isExplosive()) {
                    // Create a grenade explosion if it's from a player grenade, or regular explosion otherwise
                    createExplosion(projectile.getPosition().x, projectile.getPosition().y, 
                             projectile.isFromGrenade());
                }
                
                // Remove projectile
                if (projectile.getModelInstance() != null) {
                    // Clean up model instance if needed
                }
                
                gameState.getProjectiles().removeIndex(i);
            } else {
                // Update model instance
                if (projectile.getModelInstance() == null && !projectile.isParticle()) {
                    // Create model instance if not exists
                    Model model = projectile.isExplosive() ? explosiveProjectileModel : projectileModel;
                    ModelInstance instance = new ModelInstance(model);
                    projectile.setModelInstance(instance);
                }
                
                if (projectile.getModelInstance() != null) {
                    // Update position
                    float height = projectile.isExplosive() ? projectile.getBounceHeight() : 0.5f;
                    projectile.getModelInstance().transform.setToTranslation(
                        projectile.getPosition().x, height, projectile.getPosition().y
                    );
                }
            }
            
            // Check for enemy hits
            if (!projectile.isParticle()) {
                checkProjectileHits(projectile);
            }
        }
    }
    
    /**
     * Check if a projectile hits any enemies
     */
    private void checkProjectileHits(Projectile projectile) {
        for (int i = gameState.getEnemies().size - 1; i >= 0; i--) {
            Enemy enemy = gameState.getEnemies().get(i);
            
            // Calculate distance between projectile and enemy
            float dx = projectile.getPosition().x - enemy.getPosition().x;
            float dy = projectile.getPosition().y - enemy.getPosition().y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (distance < 0.8f) { // Simple collision check
                // Hit enemy
                boolean killed = enemy.takeDamage(projectile.getDamage());
                
                // Create hit effect (not implemented yet)
                
                // Add score if enemy was killed
                if (killed) {
                    gameState.addScore(enemy.getScoreValue());
                    
                    // Remove enemy
                    if (enemy.getModelInstance() != null) {
                        // Clean up model instance if needed
                    }
                    
                    gameState.getEnemies().removeIndex(i);
                }
                
                // Remove projectile if not explosive (explosives explode on impact)
                if (!projectile.isExplosive()) {
                    return; // Will be removed in the updateProjectiles method
                } else {
                    // Explode immediately
                    createExplosion(projectile.getPosition().x, projectile.getPosition().y, 
                             projectile.isFromGrenade());
                    return; // Will be removed in the updateProjectiles method
                }
            }
        }
    }
    
    /**
     * Create an explosion at the specified position
     * @param isGrenade whether this is a grenade explosion (more powerful)
     */
    private void createExplosion(float x, float y, boolean isGrenade) {
        Explosion explosion = new Explosion(x, y, isGrenade);
        
        // Create model instance
        ModelInstance instance = new ModelInstance(explosionModel);
        instance.transform.setToTranslation(x, 0.5f, y);
        
        // Scale the model based on whether it's a grenade (3x larger)
        if (isGrenade) {
            instance.transform.scale(3.0f, 3.0f, 3.0f);
        }
        
        explosion.setModelInstance(instance);
        
        gameState.getExplosions().add(explosion);
        
        // Screen shake effect
        shakeCamera(isGrenade ? 0.9f : 0.3f, isGrenade ? 500 : 300);
    }
    
    /**
     * Apply a camera shake effect
     */
    private void shakeCamera(float amount, int duration) {
        // TODO: Implement camera shake
    }
    
    /**
     * Update explosions
     */
    private void updateExplosions(float delta) {
        for (int i = gameState.getExplosions().size - 1; i >= 0; i--) {
            Explosion explosion = gameState.getExplosions().get(i);
            
            // Update explosion state
            boolean remove = explosion.update(delta, gameState);
            
            if (remove) {
                // Remove explosion
                if (explosion.getModelInstance() != null) {
                    // Clean up model instance if needed
                }
                
                gameState.getExplosions().removeIndex(i);
            } else {
                // Update model instance scale and opacity
                if (explosion.getModelInstance() != null) {
                    float scale = explosion.getRadius() / explosion.getMaxRadius();
                    
                    // For grenade explosions, we already scaled the initial model, so we don't need to adjust here
                    if (!explosion.isGrenade()) {
                        explosion.getModelInstance().transform.setToTranslation(
                            explosion.getPosition().x, 0.5f, explosion.getPosition().y
                        );
                        explosion.getModelInstance().transform.scale(scale, scale, scale);
                    } else {
                        explosion.getModelInstance().transform.setToTranslation(
                            explosion.getPosition().x, 0.5f, explosion.getPosition().y
                        );
                    }
                    
                    // Can't update opacity directly through transform, would need to modify material
                }
            }
        }
    }
    
    /**
     * Update enemies
     */
    private void updateEnemies(float delta) {
        for (int i = gameState.getEnemies().size - 1; i >= 0; i--) {
            Enemy enemy = gameState.getEnemies().get(i);
            
            // Update enemy behavior
            enemy.update(delta, gameState.getPlayer(), gameState);
            
            // Update model instance if needed
            if (enemy.getModelInstance() == null) {
                // Create model instance based on enemy type
                Model model = enemy.getType().equals("fast") ? fastEnemyModel : enemyModel;
                ModelInstance instance = new ModelInstance(model);
                enemy.setModelInstance(instance);
            }
            
            // Update position and rotation
            if (enemy.getModelInstance() != null) {
                // Add bobbing animation
                float bobHeight = 0.5f + (float)Math.sin(System.currentTimeMillis() * 0.005 * 
                                         (enemy.getType().equals("fast") ? 2 : 1)) * 0.1f;
                
                enemy.getModelInstance().transform.setToTranslation(
                    enemy.getPosition().x, bobHeight, enemy.getPosition().y
                );
                enemy.getModelInstance().transform.rotate(0, 1, 0, (float)Math.toDegrees(enemy.getRotation()));
            }
        }
        
        // Spawn new enemies if needed
        if (gameState.getEnemies().size < 5 + Math.floor(gameState.getScore() / 100) && Math.random() < 0.01) {
            gameState.spawnEnemy();
        }
    }
    
    /**
     * Render 3D models
     */
    private void renderModels() {
        // Check if camera is properly initialized
        if (camera == null) {
            Gdx.app.error("GameScreen", "Camera is null during rendering");
            return;
        }
        
        try {
            modelBatch.begin(camera);
            
            // Render ground and arena
            if (groundInstance != null) {
                modelBatch.render(groundInstance, environment);
            } else {
                Gdx.app.error("GameScreen", "Ground instance is null");
            }
            
            if (arenaBoundaryInstance != null) {
                modelBatch.render(arenaBoundaryInstance, environment);
            } else {
                Gdx.app.error("GameScreen", "Arena boundary instance is null");
            }
            
            // Render buildings
            for (Building building : gameState.getBuildings()) {
                if (building.getModelInstance() == null) {
                    // Create model instance if not exists
                    ModelBuilder modelBuilder = new ModelBuilder();
                    Model buildingModel = modelBuilder.createBox(
                        building.getSize(), building.getHeight(), building.getSize(),
                        new Material(ColorAttribute.createDiffuse(new Color(0x95a5a6ff))),
                        Usage.Position | Usage.Normal
                    );
                    ModelInstance instance = new ModelInstance(buildingModel);
                    instance.transform.setToTranslation(
                        building.getPosition().x, building.getHeight() / 2, building.getPosition().y
                    );
                    building.setModelInstance(instance);
                }
                
                modelBatch.render(building.getModelInstance(), environment);
            }
            
            // Render player
            modelBatch.render(playerInstance, environment);
            
            // Render enemies
            for (Enemy enemy : gameState.getEnemies()) {
                if (enemy.getModelInstance() != null) {
                    modelBatch.render(enemy.getModelInstance(), environment);
                }
            }
            
            // Render projectiles
            for (Projectile projectile : gameState.getProjectiles()) {
                if (!projectile.isParticle() && projectile.getModelInstance() != null) {
                    modelBatch.render(projectile.getModelInstance(), environment);
                }
            }
            
            // Render explosions
            for (Explosion explosion : gameState.getExplosions()) {
                if (explosion.getModelInstance() != null) {
                    modelBatch.render(explosion.getModelInstance(), environment);
                }
            }
            
            modelBatch.end();
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Error rendering models: " + e.getMessage(), e);
        }
    }
    
    /**
     * Render 2D HUD elements
     */
    private void renderHUD() {
        // Update stage
        stage.act();
        stage.draw();
        
        // Render cooldown indicators
        renderCooldowns();
        
        // Render game over screen if needed
        if (gameOver) {
            renderGameOverScreen();
        }
    }
    
    /**
     * Render cooldown indicators
     */
    private void renderCooldowns() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Position cooldown indicators in top-right corner
        float startX = Gdx.graphics.getWidth() - 80;
        float startY = Gdx.graphics.getHeight() - 20;
        
        // Dash cooldown
        shapeRenderer.setColor(Color.LIGHT_GRAY);
        shapeRenderer.rect(startX, startY - 100, 20, 100);
        
        shapeRenderer.setColor(Color.SKY);
        shapeRenderer.rect(startX, startY - 100, 20, 100 * dashCooldownHeight);
        
        // Reload cooldown
        shapeRenderer.setColor(Color.LIGHT_GRAY);
        shapeRenderer.rect(startX + 30, startY - 100, 20, 100);
        
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.rect(startX + 30, startY - 100, 20, 100 * reloadCooldownHeight);
        
        // Grenade cooldown
        shapeRenderer.setColor(Color.LIGHT_GRAY);
        shapeRenderer.rect(startX + 60, startY - 100, 20, 100);
        
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(startX + 60, startY - 100, 20, 100 * grenadeCooldownHeight);
        
        shapeRenderer.end();
    }
    
    /**
     * Render game over screen
     */
    private void renderGameOverScreen() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Semi-transparent overlay
        shapeRenderer.setColor(0, 0, 0, 0.7f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        shapeRenderer.end();
        
        // Render game over text
        spriteBatch.begin();
        
        // Scale font up for game over message
        font.getData().setScale(3);
        font.setColor(Color.RED);
        font.draw(spriteBatch, "GAME OVER", 
                 Gdx.graphics.getWidth() / 2 - 150, 
                 Gdx.graphics.getHeight() / 2 + 50);
        
        font.getData().setScale(2);
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "Score: " + gameState.getScore(), 
                 Gdx.graphics.getWidth() / 2 - 80, 
                 Gdx.graphics.getHeight() / 2 - 20);
        
        font.getData().setScale(1.5f);
        font.draw(spriteBatch, "Press ESC to restart", 
                 Gdx.graphics.getWidth() / 2 - 120, 
                 Gdx.graphics.getHeight() / 2 - 80);
        
        // Reset font scale for next frame
        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);
        
        spriteBatch.end();
    }

    @Override
    public void show() {
        // Called when this screen becomes the current screen
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stage);
        inputMultiplexer.addProcessor(inputHandler);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }
    
    @Override
    public void resize(int width, int height) {
        // Update viewport
        stage.getViewport().update(width, height, true);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }
    
    @Override
    public void pause() {
        // Called when game is paused
    }
    
    @Override
    public void resume() {
        // Called when game is resumed
    }
    
    @Override
    public void hide() {
        // Called when this screen is no longer the current screen
    }
    
    @Override
    public void dispose() {
        try {
            // Dispose resources
            shapeRenderer.dispose();
            stage.dispose();
            font.dispose();
            
            // Dispose models (with null checks)
            if (groundModel != null) groundModel.dispose();
            if (arenaModel != null) arenaModel.dispose();
            if (playerModel != null) playerModel.dispose();
            if (enemyModel != null) enemyModel.dispose();
            if (fastEnemyModel != null) fastEnemyModel.dispose();
            if (projectileModel != null) projectileModel.dispose();
            if (explosiveProjectileModel != null) explosiveProjectileModel.dispose();
            if (explosionModel != null) explosionModel.dispose();
            
            // Dispose building models
            for (Building building : gameState.getBuildings()) {
                if (building.getModelInstance() != null && building.getModelInstance().model != null) {
                    building.getModelInstance().model.dispose();
                }
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Exception during dispose: " + e.getMessage(), e);
        }
    }
    
    // Getter for camera
    public PerspectiveCamera getCamera() {
        return camera;
    }
    
    // Getter for game over state
    public boolean isGameOver() {
        return gameOver;
    }
}