import java.awt.*;

public class Spaceship extends Sprite2D {
    Image bulletImage;
    public Spaceship(Image i, Image bulletImage) {
        super(i,i); //invoke constructor on superclass Sprite2D
        this.bulletImage = bulletImage;
    }

    public void move() {
        x += xSpeed;

        if (x<=0) {
            x = 0;
            xSpeed = 0;
        }

        else if(x >= InvadersApplication.WindowSize.width-myImage.getWidth(null)) {
            x = InvadersApplication.WindowSize.width-myImage.getWidth(null);
            xSpeed = 0;
        }
    }

    public Bullet shootBullet() {
        //add new bullet to list
        Bullet b = new Bullet(bulletImage);
        b.setPosition(x+54/2, y);
        return b;

    }
}
