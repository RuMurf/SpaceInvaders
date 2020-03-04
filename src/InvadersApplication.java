import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Iterator;

public class InvadersApplication extends JFrame implements Runnable, KeyListener {
    //member variables
    public static final Dimension WindowSize = new Dimension(800,600);
    private BufferStrategy strategy;
    private Graphics offscreenBuffer;
    private static final int NUMALIENS = 30;
    private Alien[] AliensArray = new Alien[NUMALIENS];
    private Spaceship PlayerShip;
    private Image bulletImage;
    private ArrayList bulletsList = new ArrayList();
    private boolean isInitialized = false;
    private static String workingDirectory;
    private boolean isGameInProgress = false;
    private int enemyWave = 1;
    private int score = 0;
    private int highscore = 0;

    //constructor
    public InvadersApplication() {
        //display window, centered on the screen
        Dimension screensize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int x = screensize.width/2 - WindowSize.width/2;
        int y = screensize.height/2 - WindowSize.height/2;
        setBounds(x, y, WindowSize.width, WindowSize.height);
        setVisible(true);
        this.setTitle("Space Invaders");

        //load images from disk
        ImageIcon icon = new ImageIcon(workingDirectory + "\\images\\alien_ship_1.png");
        Image alienImage = icon.getImage();
        icon = new ImageIcon(workingDirectory + "\\images\\alien_ship_2.png");
        Image alienImage2 = icon.getImage();
        icon = new ImageIcon(workingDirectory + "\\images\\bullet.png");
        bulletImage = icon.getImage();

        //create and initialize aliens
        for (int i = 0; i < NUMALIENS; i++) {
            AliensArray[i] = new Alien(alienImage, alienImage2);
        }

        //create and initialize spaceship
        icon = new ImageIcon(workingDirectory + "\\images\\player_ship.png");
        Image shipImage = icon.getImage();
        PlayerShip = new Spaceship(shipImage, bulletImage);

        //create and start animation thread
        Thread t = new Thread(this);
        t.start();

        //send keyboard events sent into this JFrame back to their own event handlers
        addKeyListener(this);

        //initialize double-buffering
        createBufferStrategy(2);
        strategy = getBufferStrategy();
        offscreenBuffer = strategy.getDrawGraphics();

        isInitialized = true;
    }

    //thread entry point
    public void run() {
        while(true){
            //1: sleep for 1/50 second
            try {
                Thread.sleep(20);
            } catch (InterruptedException e){}

            //2: animate game objects if game is in progress
            if (isGameInProgress) {
                boolean anyAliensAlive = false;
                boolean alienDirectionReversalNeeded = false;
                for (int i = 0; i < NUMALIENS; i++) {
                    if (AliensArray[i].isAlive) {
                        anyAliensAlive = true;
                        if (AliensArray[i].move()) {
                            alienDirectionReversalNeeded = true;
                        }

                        //check for alien collision with ship (game over)
                        //need to pass objects into function instead of dimensions
                        if (isCollision(PlayerShip.x, AliensArray[i].x,PlayerShip.y, AliensArray[i].y, 54, 50, 32,32)) {
                            isGameInProgress = false;
                        }
                    }
                }
                if (alienDirectionReversalNeeded) {
                    for (int i = 0; i < NUMALIENS; i++) {
                        if (AliensArray[i].isAlive) {
                            AliensArray[i].reverseDirection();
                            //if passed bottom of screen, game over
                            if (AliensArray[i].y > WindowSize.height - 20) {
                                isGameInProgress = false;
                            }
                        }
                    }
                }
                if (!anyAliensAlive) {
                    enemyWave++;
                    startNewWave();
                }
                PlayerShip.move();

                Iterator iterator = bulletsList.iterator();
                while(iterator.hasNext()) {
                    Bullet b = (Bullet) iterator.next();
                    if (b.move()) {
                        //move returns true if bullet goes offscreen
                        iterator.remove();
                    }
                    else {
                        //check for collision between bullet and aliens
                        double x2 = b.x, y2 = b.y, w1 = AliensArray[0].myImage.getWidth(null), h1 = AliensArray[0].myImage.getHeight(null), w2 = bulletImage.getWidth(null), h2 = bulletImage.getHeight(null);
                        for (int i = 0; i < NUMALIENS; i++) {
                            if (AliensArray[i].isAlive) {
                                double x1 = AliensArray[i].x, y1 = AliensArray[i].y;
                                if (isCollision(x1, x2, y1, y2, w1, w2, h1, h2)) {
                                    //destroy alien and bullet
                                    AliensArray[i].isAlive = false;
                                    iterator.remove();
                                    score += 10;
                                    if (score > highscore) highscore = score;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            //3: force an application repaint
            this.repaint();
        }
    }

    //three keyboard event handler functions
    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        if (isGameInProgress) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) PlayerShip.setXSpeed(-4);
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) PlayerShip.setXSpeed(4);
            if (e.getKeyCode() == KeyEvent.VK_SPACE) bulletsList.add(PlayerShip.shootBullet());
        }
        else startNewGame();
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) PlayerShip.setXSpeed(0);
    }

    public void startNewGame() {
        enemyWave = 1;
        score = 0;
        isGameInProgress = true;
        startNewWave();
    }

    public void startNewWave() {
        //reposition aliens and player ship
        for (int i = 0; i < NUMALIENS; i++) {
            double xx = (i%5) * 80 + 70;
            double yy = (i/5) * 40 + 50;
            AliensArray[i].setPosition(xx, yy);
            AliensArray[i].setXSpeed(1+enemyWave);
            AliensArray[i].framesDrawn = 0;
            AliensArray[i].isAlive = true;
        }
        //PlayerShip.setPosition(300, 530);
    }

    private boolean isCollision(double x1, double x2, double y1, double y2, double w1, double w2, double h1, double h2) {
        if (((x1<x2 && x1+w1>x2) || (x2<x1 && x2+w2>x1) ) && ((y1<y2 && y1+h1>y2) || (y2<y1 && y2+h2>y1))) return true;
        else return false;
    }

    //helper method to draw strings centered at certain position
    private void writeString(Graphics g, int x, int y, int fontSize, String message) {
        Font f = new Font("Times", Font.PLAIN, fontSize);
        g.setFont(f);
        FontMetrics fm = getFontMetrics(f);
        int width = fm.stringWidth(message);
        g.drawString(message,x-width/2, y);
    }

    public void paint(Graphics g) {
        if (!isInitialized) return;

        g = offscreenBuffer; //draw to offscreen buffer

        //clear canvas with black rectangle
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WindowSize.width, WindowSize.height);

        if (isGameInProgress) {
            //redraw all game objects
            for (int i = 0; i < NUMALIENS; i++) {
                AliensArray[i].paint(g);
            }

            PlayerShip.paint(g);

            Iterator iterator = bulletsList.iterator();
            while (iterator.hasNext()) {
                Bullet b = (Bullet) iterator.next();
                b.paint(g);
            }

            //score
            g.setColor(Color.WHITE);
            writeString(g, WindowSize.width / 2, 60, 30, "Score: " + score + "    Best: " + highscore);
        } else { //redraw the menu screen
            g.setColor(Color.WHITE);
            writeString(g, WindowSize.width / 2, 200, 60, "GAME OVER");
            writeString(g, WindowSize.width / 2, 300, 30, "Press any key to play");
            writeString(g, WindowSize.width / 2, 350, 25, "[Arrow keys to move, space to fire]");
        }

        //flip the buffers
        strategy.show();
    }

    public static void main(String[] args) {
        workingDirectory = System.getProperty("user.dir");
        InvadersApplication game = new InvadersApplication();
    }
}
