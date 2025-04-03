package io.github.tavuc.platformer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.tavuc.platformer.PlatformerGame;
import io.github.tavuc.platformer.utils.Logger;

/**
 * The main menu screen for the game.
 * Provides options to start the game, adjust settings, or exit.
 */
public class MainMenuScreen implements Screen {
    private final PlatformerGame game;
    private final Logger logger;
    private final Stage stage;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final Skin skin;
    
    /**
     * Creates a new main menu screen.
     * 
     * @param game The main game instance
     */
    public MainMenuScreen(PlatformerGame game) {
        this.game = game;
        this.logger = new Logger("MainMenuScreen");
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.font.getData().setScale(2f);
        
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        if (!Gdx.files.internal("uiskin.json").exists()) {
            createBasicSkin();
        }
        
        this.stage = new Stage(new FitViewport(
                PlatformerGame.WORLD_WIDTH, 
                PlatformerGame.WORLD_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        
        createUI();
        
        logger.info("Main menu screen initialized");
    }
    
    /**
     * Creates a basic skin for UI elements if the default skin is not available.
     */
    private void createBasicSkin() {
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.fontColor = Color.WHITE;
        textButtonStyle.downFontColor = Color.LIGHT_GRAY;
        
        skin.add("default", textButtonStyle);
        
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", labelStyle);
        
        logger.info("Created basic skin for UI elements");
    }
    
    /**
     * Creates the UI elements for the main menu.
     */
    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);
        table.center();
        
        // Title
        Label titleLabel = new Label("Mystery Platformer", skin);
        titleLabel.setFontScale(3f);
        table.add(titleLabel).padBottom(50).row();
        
        // Start game button
        TextButton startButton = new TextButton("Start Game", skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                logger.info("Start game button clicked");
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });
        table.add(startButton).width(300).height(60).padBottom(20).row();
        
        // Options button
        TextButton optionsButton = new TextButton("Options", skin);
        optionsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                logger.info("Options button clicked");
                // TODO: Implement options screen
            }
        });
        table.add(optionsButton).width(300).height(60).padBottom(20).row();
        
        // Exit button
        TextButton exitButton = new TextButton("Exit", skin);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                logger.info("Exit button clicked");
                Gdx.app.exit();
            }
        });
        table.add(exitButton).width(300).height(60).row();
        
        stage.addActor(table);
        
        logger.info("UI elements created");
    }
    
    @Override
    public void show() {
        logger.info("Main menu screen shown");
    }
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1); 
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        stage.act(delta);
        stage.draw();
    }
    
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        logger.info("Screen resized to " + width + "x" + height);
    }
    
    @Override
    public void pause() {
        logger.info("Main menu screen paused");
    }
    
    @Override
    public void resume() {
        logger.info("Main menu screen resumed");
    }
    
    @Override
    public void hide() {
        logger.info("Main menu screen hidden");
    }
    
    @Override
    public void dispose() {
        logger.info("Disposing main menu screen resources");
        stage.dispose();
        batch.dispose();
        font.dispose();
        skin.dispose();
    }
}