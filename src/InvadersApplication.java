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
    private Alien[] aliensArray = new Alien[NUMALIENS];
    private Spaceship playerShip;
    private Image bulletImage;
    private ArrayList bulletsList = new ArrayList();
    private boolean isInitialized = false;
    private static String workingDirectory;
    private boolean gameInProgress = false;
    private boolean mainMenu = true;
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
            aliensArray[i] = new Alien(alienImage, alienImage2);
        }

        //create and initialize spaceship
        icon = new ImageIcon(workingDirectory + "\\images\\player_ship.png");
        Image shipImage = icon.getImage();
        playerShip = new Spaceship(shipImage, bulletImage);

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
                Thread.sleep(5);
            } catch (InterruptedException e){}

            //2: animate game objects if game is in progress
            if (gameInProgress) {
                boolean anyAliensAlive = false;
                boolean alienDirectionReversalNeeded = false;
                for (int i = 0; i < NUMALIENS; i++) {
                    if (aliensArray[i].isAlive) {
                        anyAliensAlive = true;
                        if (aliensArray[i].move()) {
                            alienDirectionReversalNeeded = true;
                        }

                        //check for alien collision with ship (game over)
                        if (collision(playerShip, aliensArray[i])) gameInProgress = false;
                    }
                }
                if (alienDirectionReversalNeeded) {
                    for (int i = 0; i < NUMALIENS; i++) {
                        if (aliensArray[i].isAlive) {
                            aliensArray[i].reverseDirection();
                            //if passed bottom of screen, game over
                            if (aliensArray[i].y > WindowSize.height - 20) {
                                gameInProgress = false;
                            }
                        }
                    }
                }
                if (!anyAliensAlive) {
                    enemyWave++;
                    startNewWave();
                }
                playerShip.move();

                Iterator iterator = bulletsList.iterator();
                while(iterator.hasNext()) {
                    Bullet bullet = (Bullet) iterator.next();
                    if (bullet.move()) {
                        //move returns true if bullet goes offscreen
                        iterator.remove();
                    }
                    else {
                        //check for collision between bullet and aliens
                        for (int i = 0; i < NUMALIENS; i++) {
                            if (aliensArray[i].isAlive) {
                                if (collision(aliensArray[i], bullet)) {
                                    //destroy alien and bullet
                                    aliensArray[i].isAlive = false;
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
        if (gameInProgress) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) playerShip.setXSpeed(-4);
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) playerShip.setXSpeed(4);
            if (e.getKeyCode() == KeyEvent.VK_SPACE) bulletsList.add(playerShip.shootBullet());
        }
        else startNewGame();
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) playerShip.setXSpeed(0);
    }

    public void startNewGame() {
        mainMenu = false;
        enemyWave = 1;
        score = 0;
        gameInProgress = true;
        playerShip.setPosition(300, 530);
        startNewWave();
    }

    public void startNewWave() {
        //reposition aliens and player ship
        for (int i = 0; i < NUMALIENS; i++) {
            double xx = (i%5) * 80 + 70;
            double yy = (i/5) * 40 + 70;
            aliensArray[i].setPosition(xx, yy);
            aliensArray[i].setXSpeed(0.1+enemyWave);
            aliensArray[i].framesDrawn = 0;
            aliensArray[i].isAlive = true;
        }
    }

    private boolean collision(Sprite2D object1, Sprite2D object2) {
        double x1 = object1.x, y1 = object1.y, w1 = object1.myImage.getWidth(null), h1 = object1.myImage.getHeight(null);
        double x2 = object2.x, y2 = object2.y, w2 = object2.myImage.getWidth(null), h2 = object2.myImage.getHeight(null);
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

        if (gameInProgress) {
            //redraw all game objects
            for (int i = 0; i < NUMALIENS; i++) {
                aliensArray[i].paint(g);
            }

            playerShip.paint(g);

            Iterator iterator = bulletsList.iterator();
            while (iterator.hasNext()) {
                Bullet b = (Bullet) iterator.next();
                b.paint(g);
            }

            //print score
            g.setColor(Color.WHITE);
            writeString(g, WindowSize.width / 2, 60, 30, "Score: " + score + "    Best: " + highscore);
        }
        else if (mainMenu) {
            g.setColor(Color.WHITE);
            writeString(g, WindowSize.width / 2, 200, 60, "SPACE INVADERS!");
            writeString(g, WindowSize.width / 2, 300, 30, "Press any key to play");
            writeString(g, WindowSize.width / 2, 350, 25, "[Arrow keys to move, space to fire]");
        }
        else { //redraw the menu screen
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
