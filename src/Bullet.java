import java.awt.*;

public class Bullet extends Sprite2D {
    public Bullet(Image i) {
        super(i,i);
    }

    public boolean move() {
        y -= 5;
        return (y < 0); //return true if bullet is offscreen and needs destroying
    }
}
