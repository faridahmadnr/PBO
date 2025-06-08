import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public class ExplosionAnimation {
    private int x, y;
    private int currentFrame = 0;
    private final int totalFrames = 5; // Adjust based on your actual explosion frames
    private final Image[] frames;
    private Timer animationTimer;
    private boolean isActive = true;
    
    public ExplosionAnimation(int x, int y) {
        this.x = x;
        this.y = y;
        frames = new Image[totalFrames];
        
        // Load explosion frames from assets
        for (int i = 0; i < totalFrames; i++) {
            String path = "c:\\Users\\ASV\\OneDrive - UGM 365\\Desktop\\New folder (2)\\pbofarid\\assets\\ledakan\\animasi" + i + ".png";
            frames[i] = new ImageIcon(path).getImage();
        }
        
        // Setup animation timer (100ms between frames)
        animationTimer = new Timer(100, e -> {
            currentFrame++;
            if (currentFrame >= totalFrames) {
                isActive = false;
                animationTimer.stop();
            }
        });
        
        animationTimer.start();
    }
    
    public void draw(Graphics g) {
        if (isActive && currentFrame < totalFrames) {
            g.drawImage(frames[currentFrame], x - 50, y - 50, 100, 100, null);
        }
    }
    
    public boolean isActive() {
        return isActive;
    }
}
