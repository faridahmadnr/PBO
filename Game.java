import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class Game extends JPanel implements Runnable {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int FPS = 60;
    
    // Game states
    private static final int STATE_MENU = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_GAMEOVER = 2;
    
    private int gameState;
    private Player player1;
    private Player player2;
    private ArrayList<Rocket> rockets;
    private ArrayList<ExplosionAnimation> explosions;
    private Random random;
    private boolean gameRunning;
    private boolean gameOver;
    private Clip backgroundMusic;
    private Clip collisionSound;
    private Clip gameOverSound;
    
    // Timer variables
    private long gameStartTime;
    private long currentSurvivalTime;
    private long finalSurvivalTime; // To store the final time when game ends
    private int difficultyLevel = 1;
    private int maxRocketsPerPlayer = 2; // Starting rockets per player
    
    public Game() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        
        // Initialize in menu state
        gameState = STATE_MENU;
        
        // Use image assets for players
        player1 = new Player(100, 300, Color.BLUE, 3, "c:\\Users\\ASV\\OneDrive - UGM 365\\Desktop\\New folder (2)\\pbofarid\\assets\\player2.png");
        player2 = new Player(700, 300, Color.RED, 3, "c:\\Users\\ASV\\OneDrive - UGM 365\\Desktop\\New folder (2)\\pbofarid\\assets\\player1.png");
        rockets = new ArrayList<>();
        explosions = new ArrayList<>();
        random = new Random();
        gameRunning = true;
        gameOver = false;
        
        // Load sound effects
        loadSoundEffects();
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                
                // Menu controls
                if (gameState == STATE_MENU && key == KeyEvent.VK_SPACE) {
                    startGame();
                }
                
                // Game controls (only active during gameplay)
                if (gameState == STATE_PLAYING) {
                    // Player 1 controls (WASD)
                    if (key == KeyEvent.VK_W) player1.setMovingUp(true);
                    if (key == KeyEvent.VK_S) player1.setMovingDown(true);
                    if (key == KeyEvent.VK_A) player1.setMovingLeft(true);
                    if (key == KeyEvent.VK_D) player1.setMovingRight(true);
                    
                    // Player 2 controls (Arrow keys)
                    if (key == KeyEvent.VK_UP) player2.setMovingUp(true);
                    if (key == KeyEvent.VK_DOWN) player2.setMovingDown(true);
                    if (key == KeyEvent.VK_LEFT) player2.setMovingLeft(true);
                    if (key == KeyEvent.VK_RIGHT) player2.setMovingRight(true);
                }
                
                // Restart game from game over state
                if (gameState == STATE_GAMEOVER && key == KeyEvent.VK_ENTER) {
                    gameState = STATE_MENU;
                }
                
                // Add test key for spawning explosions (for testing only)
                if (key == KeyEvent.VK_X) {
                    createExplosion(player1.getX(), player1.getY());
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                
                if (gameState == STATE_PLAYING) {
                    // Player 1 controls (WASD)
                    if (key == KeyEvent.VK_W) player1.setMovingUp(false);
                    if (key == KeyEvent.VK_S) player1.setMovingDown(false);
                    if (key == KeyEvent.VK_A) player1.setMovingLeft(false);
                    if (key == KeyEvent.VK_D) player1.setMovingRight(false);
                    
                    // Player 2 controls (Arrow keys)
                    if (key == KeyEvent.VK_UP) player2.setMovingUp(false);
                    if (key == KeyEvent.VK_DOWN) player2.setMovingDown(false);
                    if (key == KeyEvent.VK_LEFT) player2.setMovingLeft(false);
                    if (key == KeyEvent.VK_RIGHT) player2.setMovingRight(false);
                }
            }
        });
        
        Thread gameThread = new Thread(this);
        gameThread.start();
    }
    
    /**
     * Plays background music that loops continuously
     * @param musicFilePath Path to the music file (WAV format)
     */
    private void playBackgroundMusic(String musicFilePath) {
        try {
            File musicFile = new File(musicFilePath);
            if (!musicFile.exists()) {
                System.err.println("Music file does not exist: " + musicFilePath);
                return;
            }
            
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicFile);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioInput);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusic.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing background music: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Loads sound effects that will be used in the game
     */
    private void loadSoundEffects() {
        try {
            // Load collision sound
            File collisionFile = new File("c:\\Users\\ASV\\OneDrive - UGM 365\\Desktop\\New folder (2)\\pbofarid\\assets\\collision.wav");
            if (collisionFile.exists()) {
                AudioInputStream collisionInput = AudioSystem.getAudioInputStream(collisionFile);
                collisionSound = AudioSystem.getClip();
                collisionSound.open(collisionInput);
            } else {
                System.err.println("Collision sound file does not exist");
            }
            
            // Load game over sound
            File gameOverFile = new File("c:\\Users\\ASV\\OneDrive - UGM 365\\Desktop\\New folder (2)\\pbofarid\\assets\\gameover.wav");
            if (gameOverFile.exists()) {
                AudioInputStream gameOverInput = AudioSystem.getAudioInputStream(gameOverFile);
                gameOverSound = AudioSystem.getClip();
                gameOverSound.open(gameOverInput);
            } else {
                System.err.println("Game over sound file does not exist");
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error loading sound effects: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Plays a sound effect once
     * @param clip The sound clip to play
     */
    private void playSoundEffect(Clip clip) {
        if (clip != null) {
            try {
                // Stop and reset the clip before playing
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
                System.out.println("Playing sound effect");
            } catch (Exception e) {
                System.err.println("Error playing sound effect: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Attempted to play null sound clip");
        }
    }
    
    /**
     * Start the game from menu state
     */
    private void startGame() {
        // Reset game elements
        player1 = new Player(100, 300, Color.BLUE, 3, "c:\\Users\\ASV\\OneDrive - UGM 365\\Desktop\\New folder (2)\\pbofarid\\assets\\player2.png");
        player2 = new Player(700, 300, Color.RED, 3, "c:\\Users\\ASV\\OneDrive - UGM 365\\Desktop\\New folder (2)\\pbofarid\\assets\\player1.png");
        rockets.clear();
        explosions.clear();
        gameOver = false;
        
        // Reset difficulty
        difficultyLevel = 1;
        maxRocketsPerPlayer = 2;
        
        // Start the timer
        gameStartTime = System.currentTimeMillis();
        currentSurvivalTime = 0;
        
        // Change state to playing
        gameState = STATE_PLAYING;
        
        // Start playing background music
        playBackgroundMusic("c:\\Users\\ASV\\OneDrive - UGM 365\\Desktop\\New folder (2)\\pbofarid\\assets\\background_music.wav");
    }
    
    private void restartGame() {
        // Change back to menu state instead of directly restarting
        gameState = STATE_MENU;
        gameOver = false;
    }
    
    // We should also add clean-up code to stop the music when the game is closed
    public void stopMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
            backgroundMusic.close();
        }
        
        if (collisionSound != null) {
            collisionSound.close();
        }
        
        if (gameOverSound != null) {
            gameOverSound.close();
        }
    }
    
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double ns = 1000000000 / FPS;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;
        
        // Game loop
        while (gameRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            
            while (delta >= 1) {
                update();
                delta--;
            }
            
            repaint();
            frames++;
            
            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                frames = 0;
                spawnRockets();
            }
            
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void update() {
        // Only update game elements if we're playing
        if (gameState != STATE_PLAYING) return;
        
        // Update survival time and check difficulty increase
        updateSurvivalTime();
        
        // Check if all players are dead at the beginning of the frame
        checkAllPlayersDead();
        
        // Only update living players
        if (player1.getLives() > 0) {
            player1.update(WIDTH, HEIGHT);
        }
        
        if (player2.getLives() > 0) {
            player2.update(WIDTH, HEIGHT);
        }
        
        // Check player collision only if both are alive
        if (player1.getLives() > 0 && player2.getLives() > 0 && player1.collidesWith(player2)) {
            createExplosion(player1.getX(), player1.getY());
            createExplosion(player2.getX(), player2.getY());
            player1.setLives(0);
            player2.setLives(0);
            playSoundEffect(collisionSound);
            setGameOverState();
            return;
        }
        
        // Update rockets and check collisions
        ArrayList<Rocket> rocketsToRemove = new ArrayList<>();
        for (Rocket rocket : rockets) {
            rocket.update();
            
            // Remove rockets targeting dead players
            Player target = rocket.getTarget();
            if ((target == player1 && player1.getLives() <= 0) || 
                (target == player2 && player2.getLives() <= 0)) {
                rocketsToRemove.add(rocket);
                continue;
            }
            
            // Check if rocket has expired
            if (rocket.isExpired()) {
                rocketsToRemove.add(rocket);
                continue;
            }
            
            // Check if rocket hit player1 (only if alive)
            if (player1.getLives() > 0 && rocket.collidesWith(player1)) {
                rocketsToRemove.add(rocket);
                player1.decreaseLives();
                playSoundEffect(collisionSound);
                
                // Create explosion animation if player dies
                if (player1.getLives() <= 0) {
                    createExplosion(player1.getX(), player1.getY());
                    
                    // Check if we should trigger game over (both players dead)
                    if (player2.getLives() <= 0) {
                        setGameOverState();
                    }
                }
                
                continue;
            }
            
            // Check if rocket hit player2 (only if alive)
            if (player2.getLives() > 0 && rocket.collidesWith(player2)) {
                rocketsToRemove.add(rocket);
                player2.decreaseLives();
                playSoundEffect(collisionSound);
                
                // Create explosion animation if player dies
                if (player2.getLives() <= 0) {
                    createExplosion(player2.getX(), player2.getY());
                    
                    // Check if we should trigger game over (both players dead)
                    if (player1.getLives() <= 0) {
                        setGameOverState();
                    }
                }
            }
        }
        
        rockets.removeAll(rocketsToRemove);
        
        // Remove finished explosions
        explosions.removeIf(explosion -> !explosion.isActive());
        
        // Double check if all players are dead at the end of the frame
        checkAllPlayersDead();
    }
    
    private void createExplosion(int x, int y) {
        explosions.add(new ExplosionAnimation(x, y));
    }
    
    /**
     * Changes to game over state and stops background music
     */
    private void setGameOverState() {
        // Save the final survival time when game ends
        finalSurvivalTime = currentSurvivalTime;
        
        gameOver = true;
        gameState = STATE_GAMEOVER;
        
        // Stop background music but play game over sound
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
        
        playSoundEffect(gameOverSound);
        
        System.out.println("Game Over! Final survival time: " + finalSurvivalTime + " seconds");
    }
    
    /**
     * Updates the survival time and increases difficulty every 5 seconds
     */
    private void updateSurvivalTime() {
        // Only update time if the game is still active
        if (gameState == STATE_PLAYING && !gameOver) {
            long currentTime = System.currentTimeMillis();
            currentSurvivalTime = (currentTime - gameStartTime) / 1000; // Convert to seconds
            
            // Increase difficulty every 5 seconds
            int newDifficultyLevel = (int)(currentSurvivalTime / 5) + 1;
            
            if (newDifficultyLevel > difficultyLevel) {
                difficultyLevel = newDifficultyLevel;
                maxRocketsPerPlayer = 2 + (difficultyLevel - 1); // Add one more rocket per difficulty level
                System.out.println("Difficulty increased to " + difficultyLevel + "! Max rockets: " + maxRocketsPerPlayer);
            }
        }
    }
    
    /**
     * Checks if all players are dead and triggers game over if needed
     */
    private void checkAllPlayersDead() {
        // If both players are dead, trigger game over
        if (player1.getLives() <= 0 && player2.getLives() <= 0 && !gameOver) {
            setGameOverState();
        }
    }
    
    private void spawnRockets() {
        // Only spawn rockets when in playing state
        if (gameState != STATE_PLAYING) return;
        
        // Only spawn rockets targeting living players
        if (player1.getLives() > 0) {
            int p1Targeting = 0;
            
            for (Rocket rocket : rockets) {
                if (rocket.getTarget() == player1) p1Targeting++;
            }
            
            // Spawn new rockets if less than the current max are targeting player1
            while (p1Targeting < maxRocketsPerPlayer) {
                spawnRocket(player1);
                p1Targeting++;
            }
        }
        
        if (player2.getLives() > 0) {
            int p2Targeting = 0;
            
            for (Rocket rocket : rockets) {
                if (rocket.getTarget() == player2) p2Targeting++;
            }
            
            // Spawn new rockets if less than the current max are targeting player2
            while (p2Targeting < maxRocketsPerPlayer) {
                spawnRocket(player2);
                p2Targeting++;
            }
        }
    }
    
    private void spawnRocket(Player target) {
        // Only spawn rockets for living players
        if (target.getLives() <= 0) return;
        
        int side = random.nextInt(4); // 0: top, 1: right, 2: bottom, 3: left
        int x = 0, y = 0;
        
        switch (side) {
            case 0: // top
                x = random.nextInt(WIDTH);
                y = 0;
                break;
            case 1: // right
                x = WIDTH;
                y = random.nextInt(HEIGHT);
                break;
            case 2: // bottom
                x = random.nextInt(WIDTH);
                y = HEIGHT;
                break;
            case 3: // left
                x = 0;
                y = random.nextInt(HEIGHT);
                break;
        }
        
        rockets.add(new Rocket(x, y, target));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Draw different screens based on game state
        if (gameState == STATE_MENU) {
            drawMenuScreen(g);
        } else if (gameState == STATE_PLAYING || gameState == STATE_GAMEOVER) {
            drawGameScreen(g);
        }
    }
    
    /**
     * Draws the main game screen with players, rockets, and UI
     */
    private void drawGameScreen(Graphics g) {
        // Draw players
        if (player1.getLives() > 0) {
            player1.draw(g);
        }
        
        if (player2.getLives() > 0) {
            player2.draw(g);
        }
        
        // Draw rockets
        for (Rocket rocket : rockets) {
            rocket.draw(g);
        }
        
        // Draw explosions   
        for (ExplosionAnimation explosion : explosions) {
            explosion.draw(g);
        }
        
        // Draw UI
        g.setColor(Color.WHITE);
        g.drawString("Player 1 Lives: " + player1.getLives(), 10, 20);
        g.drawString("Player 2 Lives: " + player2.getLives(), WIDTH - 120, 20);
        
        // Draw survival time and difficulty information
        if (gameState == STATE_PLAYING || gameState == STATE_GAMEOVER) {
            g.setColor(Color.YELLOW);
            String timeText = String.format("Time: %d seconds", currentSurvivalTime);
            g.drawString(timeText, WIDTH / 2 - 60, 20);
            
            g.setColor(Color.ORANGE);
            String difficultyText = String.format("Difficulty: %d", difficultyLevel);
            g.drawString(difficultyText, WIDTH / 2 - 60, 40);
        }
        
        // Draw game over text if in game over state
        if (gameState == STATE_GAMEOVER) {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.setColor(Color.RED);
            g.drawString("GAME OVER", WIDTH / 2 - 100, HEIGHT / 2 - 20);
            
            g.setFont(new Font("Arial", Font.BOLD, 25));
            g.setColor(Color.YELLOW);
            String survivalText = String.format("You survived for %d seconds!", finalSurvivalTime);
            int survivalWidth = g.getFontMetrics().stringWidth(survivalText);
            g.drawString(survivalText, WIDTH / 2 - survivalWidth / 2, HEIGHT / 2 + 20);
            
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.setColor(Color.WHITE);
            g.drawString("Press ENTER to return to menu", WIDTH / 2 - 140, HEIGHT / 2 + 60);
        }
    }
    
    /**
     * Draws the menu screen
     */
    private void drawMenuScreen(Graphics g) {
        // Draw title
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        String title = "ROCKET DODGE";
        FontMetrics fm = g.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g.drawString(title, (WIDTH - titleWidth) / 2, HEIGHT / 3);
        
        // Draw instructions
        g.setFont(new Font("Arial", Font.PLAIN, 25));
        g.setColor(Color.WHITE);
        String startText = "Press SPACE to Start";
        int startWidth = g.getFontMetrics().stringWidth(startText);
        g.drawString(startText, (WIDTH - startWidth) / 2, HEIGHT / 2);
        
        // Draw game description
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.setColor(Color.CYAN);
        String gameDesc = "SURVIVAL MODE: Dodge rockets as long as possible!";
        int descWidth = g.getFontMetrics().stringWidth(gameDesc);
        g.drawString(gameDesc, (WIDTH - descWidth) / 2, HEIGHT / 2 + 40);
        
        // Draw difficulty info
        g.setFont(new Font("Arial", Font.ITALIC, 18));
        g.setColor(Color.ORANGE);
        String diffText = "Difficulty increases every 5 seconds!";
        int diffWidth = g.getFontMetrics().stringWidth(diffText);
        g.drawString(diffText, (WIDTH - diffWidth) / 2, HEIGHT / 2 + 70);
        
        // Draw controls info
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.setColor(Color.WHITE);
        g.drawString("Player 1: WASD to move", 50, HEIGHT - 120);
        g.drawString("Player 2: Arrow keys to move", 50, HEIGHT - 90);
        g.drawString("Avoid rockets and don't crash into each other!", 50, HEIGHT - 60);
    }
    
    
}
