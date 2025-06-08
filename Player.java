import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Player {
    private static final int SIZE = 40;
    private static final int SPEED = 5;
    
    private int x, y;
    private Color color;
    private int lives;
    private boolean movingUp, movingDown, movingLeft, movingRight;
    private BufferedImage playerImage;
    
    public Player(int x, int y, Color color, int lives, String imagePath) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.lives = lives;
        this.movingUp = false;
        this.movingDown = false;
        this.movingLeft = false;
        this.movingRight = false;
        
        try {
            playerImage = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            System.err.println("Could not load player image: " + imagePath);
            e.printStackTrace();
        }
    }
    
    public void update(int screenWidth, int screenHeight) {
        if (movingUp) y = Math.max(y - SPEED, 0);
        if (movingDown) y = Math.min(y + SPEED, screenHeight - SIZE);
        if (movingLeft) x = Math.max(x - SPEED, 0);
        if (movingRight) x = Math.min(x + SPEED, screenWidth - SIZE);
    }
    
    public void draw(Graphics g) {
        if (playerImage != null) {
            g.drawImage(playerImage, x, y, SIZE, SIZE, null);
        } else {
            // Fallback to a colored rectangle if image couldn't be loaded
            g.setColor(color);
            g.fillRect(x, y, SIZE, SIZE);
        }
        
        // Draw lives as small indicators
        g.setColor(color);
        for (int i = 0; i < lives; i++) {
            g.fillRect(x + i * 10, y - 15, 8, 8);
        }
    }
    
    public boolean collidesWith(Player other) {
        Rectangle thisRect = new Rectangle(x, y, SIZE, SIZE);
        Rectangle otherRect = new Rectangle(other.x, other.y, SIZE, SIZE);
        return thisRect.intersects(otherRect);
    }
    
    public boolean collidesWith(Rocket rocket) {
        Rectangle thisRect = new Rectangle(x, y, SIZE, SIZE);
        Rectangle rocketRect = new Rectangle(rocket.getX(), rocket.getY(), rocket.getSize(), rocket.getSize());
        return thisRect.intersects(rocketRect);
    }
    
    public void decreaseLives() {
        lives--;
    }
    
    // Getters and setters
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getSize() {
        return SIZE;
    }
    
    public int getLives() {
        return lives;
    }
    
    public void setMovingUp(boolean movingUp) {
        this.movingUp = movingUp;
    }
    
    public void setMovingDown(boolean movingDown) {
        this.movingDown = movingDown;
    }
    
    public void setMovingLeft(boolean movingLeft) {
        this.movingLeft = movingLeft;
    }
    
    public void setMovingRight(boolean movingRight) {
        this.movingRight = movingRight;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }
}
