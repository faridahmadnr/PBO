import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Rocket {
    private static final int SIZE = 20;
    private static final int SPEED = 3;
    private static final long LIFETIME = 3000; // 3 seconds in milliseconds
    
    private int x, y;
    private Player target;
    private long spawnTime;
    private BufferedImage rocketImage;
    
    public Rocket(int x, int y, Player target) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.spawnTime = System.currentTimeMillis();
        
        try {
            rocketImage = ImageIO.read(new File("c:\\Users\\ASV\\OneDrive - UGM 365\\Desktop\\New folder (2)\\pbofarid\\assets\\rocket.png"));
        } catch (IOException e) {
            System.err.println("Could not load rocket image");
            e.printStackTrace();
        }
    }
    
    public void update() {
        // Calculate direction toward target
        int targetX = target.getX() + target.getSize() / 2;
        int targetY = target.getY() + target.getSize() / 2;
        
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // Normalize and apply speed
        if (distance > 0) {
            dx = (dx / distance) * SPEED;
            dy = (dy / distance) * SPEED;
        }
        
        x += dx;
        y += dy;
    }
    
    public void draw(Graphics g) {
        if (rocketImage != null) {
            // Calculate angle of rocket
            double angle = Math.atan2(
                target.getY() + target.getSize() / 2 - y,
                target.getX() + target.getSize() / 2 - x
            );
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(x, y);
            g2d.rotate(angle);
            g2d.drawImage(rocketImage, -SIZE / 2, -SIZE / 2, SIZE, SIZE, null);
            g2d.rotate(-angle);
            g2d.translate(-x, -y);
        } else {
            // Fallback to a simple oval if image couldn't be loaded
            g.setColor(Color.ORANGE);
            g.fillOval(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
        }
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - spawnTime > LIFETIME;
    }
    
    public boolean collidesWith(Player player) {
        Rectangle rocketRect = new Rectangle(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
        Rectangle playerRect = new Rectangle(player.getX(), player.getY(), player.getSize(), player.getSize());
        return rocketRect.intersects(playerRect);
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getSize() {
        return SIZE;
    }
    
    public Player getTarget() {
        return target;
    }
}
