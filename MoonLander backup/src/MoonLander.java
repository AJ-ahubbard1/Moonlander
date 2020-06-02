import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

public class MoonLander implements ActionListener, MouseListener, KeyListener {
    public static Renderer renderer;
    public static boolean crash = false, running = false, touchDown = false;
    public static MoonLander moonLander = new MoonLander();
    public static Ship ship = new Ship();
    final static int WIDTH = 1200, HEIGHT = 600, CENTERX = WIDTH/2, CENTERY = HEIGHT/2;
    public static int ticks;
    public static Random rnd = new Random();
    boolean [] keys = new boolean[100];
    public static int numStars = 300;
    public static int[] stars = new int[numStars];
    public static int[] starSize = new int[numStars];
    final static float GRAVITY = .1f;
    final static float THRUST = .3f;
    final static double PI = 3.1415926535;
    final static int GAP = 20;
    final static int surfaceSize = (WIDTH/GAP) + 1;    //61, [0..60]
    public static int[] surface = new int[surfaceSize];
    public static int[] surfaceX = new int[surfaceSize];
    final static int MAX= 150;
    final static int MIN = 50;
    public static int landingpad;
    public static int landingpadSize = 100;
    public static int score;
    public static int highscore;

    // avoid null pointer exceptions
    public MoonLander() {}
    public MoonLander(boolean start) {
        Timer timer = new Timer(20,this);
        renderer = new Renderer();
        JFrame jframe = new JFrame();
        jframe.add(renderer);
        jframe.setTitle("MOONLANDER");
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.addKeyListener(this);
        jframe.setSize(WIDTH,HEIGHT);
        jframe.setResizable(false);
        jframe.setVisible(true);
        timer.start();
        reload(start);
        running = false;
        score = 0;
        highscore = 0;
    }
    public static void reload(boolean start) {
        ship = new Ship(start);
        for (int i = 0; i < numStars; i++) {
            stars[i] = rnd.nextInt(WIDTH * HEIGHT);
            starSize[i] = rnd.nextInt(2) + 1;
        }
        int j = surfaceSize - 1;  //60
        surfaceX[0] = 0;
        surface[0] = HEIGHT;
        surfaceX[j] = WIDTH;
        surface[j] = HEIGHT;
        int landingpadStop = landingpadSize/GAP;
        landingpad = rnd.nextInt((j+1) - 3*landingpadStop) + landingpadStop;
        int landingpadElevation = HEIGHT - rnd.nextInt(MAX-MIN) - MIN;
        for (int i = 1; i < j; i++) {
            surfaceX[i] = i*GAP;
            if (i == landingpad-1)
                surface[i] = HEIGHT - 10;
            else if (i == landingpad + landingpadStop + 1)
                surface[i] = HEIGHT - 10;
            else if (i >= landingpad && i <= landingpad + landingpadStop)
                surface[i] = landingpadElevation;
            else
                surface[i] = HEIGHT - rnd.nextInt(MAX-MIN) - MIN;
        }
        running = true; crash = false; touchDown = false;
    }

    public static class Renderer extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            MoonLander.moonLander.repaint(g);
        }
    }
    public static class Ship {
        public float x, y;
        //off sets to make hitbox rect for bufferedimage
        final int xos = 18;
        final int yos = 11;
        final int wos = 34;
        final int hos = 25;
        public double angle;
        public float fuel;
        public float velX, velY;
        public BufferedImage rocket = getBufferedImage();
        public Rectangle rect = new Rectangle((int)x,(int)y, rocket.getWidth() - wos, rocket.getHeight() - hos);

        public Ship() { }
        public Ship(boolean start) {
            if (start) {
                x = CENTERX;
                y = 100;
                fuel = 100;
                angle = 0;
            }
        }
        public BufferedImage getBufferedImage() {
            try { return ImageIO.read(getClass().getResource("/resources/rocket.png")); }
            catch (IOException e) { e.printStackTrace(); }
            return null;
        }
    }

    @Override
    //physics
    public void actionPerformed(ActionEvent e) {
        ticks++;
        renderer.repaint();
    }
    public void paintRocket(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        double rotationRequired = ship.angle;
        double locationX =  1d * ship.rocket.getWidth() / 2;
        double locationY = 1d * ship.rocket.getHeight() / 2;
        AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        g2d.drawImage(op.filter(ship.rocket, null), (int)ship.x, (int)ship.y, null);
    }
    public void repaint(Graphics g) {
        //background
        g.setColor(Color.black);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.WHITE);
        for (int i = 0; i < numStars; i++)
            g.fillOval(stars[i]%WIDTH,stars[i]/WIDTH,starSize[i],starSize[i]);

        update();

        //thrust
        if (keys[87] && ship.fuel > 0) {
            g.setColor(Color.yellow);
            float i = -10;
            while (i <= 10) {
                double a = i * PI / 180;
                int thruster = rnd.nextInt(100);
                int x1 = (int)(ship.x + 1 + ship.rocket.getWidth() / 2);
                int y1 = (int)(ship.y + ship.rocket.getHeight() / 2);
                int x2 = (int) (x1 + Math.sin(ship.angle + PI + a) * thruster);
                int y2 = (int) (y1 - Math.cos(ship.angle + PI + a) * thruster);
                int color = rnd.nextInt(3);
                switch (color) {
                    case 0:
                        g.setColor(Color.yellow);
                        break;
                    case 1:
                        g.setColor(Color.orange);
                        break;
                    case 2:
                        g.setColor(Color.red);
                        break;
                }
                g.drawLine(x1, y1, x2, y2);
                i += .5;
            }
        }
        //rocket
        paintRocket(g);

        if (running || (crash || touchDown)) {
            /* rect, uncomment when troubleshooting
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(Color.white);
            g2d.rotate(ship.angle, ship.rect.x + ship.rect.width *.5, ship.rect.y + ship.rect.height * .5);
            g2d.drawRect(ship.rect.x, ship.rect.y, ship.rect.width, ship.rect.height);
            g2d.dispose();
             */

            //fuelbar
            int gasX = CENTERX - 50, gasY = 5, gasW = 100, gasH = 5;
            g.setColor(Color.green);
            g.drawRect(gasX,gasY,gasW,gasH);
            if (ship.fuel <= 60)
                g.setColor(Color.yellow);
            if (ship.fuel <= 40)
                g.setColor(Color.orange);
            if (ship.fuel <= 20)
                g.setColor(Color.red);
            if (ship.fuel <= 10)
                g.setColor(Color.red.darker());
            g.fillRect(CENTERX - 50, 5, (int) ship.fuel, 5);

            //surface
            g.setColor(Color.GRAY);
            g.fillPolygon(surfaceX, surface, surfaceSize);

            //Landingpad
            int[][] pts = new int[2][4];
            pts[0][0] = surfaceX[landingpad - 1];
            pts[1][0] = surface[landingpad - 1];
            pts[0][1] = surfaceX[landingpad];
            pts[1][1] = surface[landingpad];
            pts[0][2] = surfaceX[landingpad + landingpadSize / GAP];
            pts[1][2] = surface[landingpad + landingpadSize / GAP];
            pts[0][3] = surfaceX[landingpad + 1 + landingpadSize / GAP];
            pts[1][3] = surface[landingpad + 1 + landingpadSize / GAP];

            g.setColor(Color.WHITE);
            g.fillPolygon(pts[0], pts[1], 4);
        }

        //messages
        if(touchDown) {
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 88));
            String l = "LANDING" + (score == 1 ? "" : "S");
            String s = String.format("TOUCHDOWN! %d " + l, score);
            g.drawString(s, 20, 150);
        }
        if(crash) {
            g.setColor(Color.red);
            g.setFont(new Font("Arial", Font.BOLD, 100));
            g.drawString("GAME OVER!", 250, 150);
            score=0;
        }
        if (!running) {
            g.setColor(Color.white);
            g.setFont(new Font("Arial",Font.BOLD, 100));
            g.drawString("Spacebar to start", 182, CENTERY);
            if (!crash && ! touchDown) {
                g.drawString("MOONLANDER", 236, 90);
                animate();
            }
        }
        if (running) {
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            String s = String.format("pos[%.2f,%.2f] vel[%.2f,%.2f]", ship.x, ship.y, ship.velX, ship.velY);
            g.drawString(s, CENTERX-140, 40);
        }
    }
    public void animate() {
        keys[87] = true;
        ship.angle = PI/2;
        ship.x += 3;
    }
    public void stopAnimate() {
        keys[87] = false;
        ship = new Ship(true);
    }

    public static void main(String[] args) {
	    moonLander = new MoonLander(true);
    }
    //input
    public void update() {
        //spacebar
        if (keys[32]) {
            if (!running) {
                running = true;
                stopAnimate();
            }
            if (touchDown || crash)
                reload(true);
            keys[32] = false;
        }
        //w = 87
        if (running && !crash && !touchDown && ship.fuel > 0) {
            if (keys[87]) {
                ship.velX += Math.sin(ship.angle) * THRUST;
                ship.velY -= Math.cos(ship.angle) * THRUST;
                ship.fuel -= THRUST;
            }
            //a = 65
            if (keys[65])
                ship.angle -= .05;
            //d = 68
            if (keys[68])
                ship.angle += .05;
        }
        // update position
        if (running) {
            ship.velY += GRAVITY;
            ship.x += ship.velX;
            ship.y += ship.velY;
        }

        //Out of bounds
        if (ship.x + ship.xos + ship.rect.width < 0)
            ship.x = WIDTH-1;
        if (ship.x > WIDTH)
            ship.x = 1 - ship.xos - ship.rect.width;

        //Crash or Land
        if (!crash && !touchDown) {
            int xStart = (int) (ship.x + ship.xos);
            if (xStart <= 0)
                xStart = 1;
            int xEnd = (int) (ship.x + ship.xos + ship.rect.width + GAP);
            if (xEnd > WIDTH)
                xEnd = WIDTH;
            while (xStart < xEnd) {
                if (xStart % GAP == 0) {
                    int i = xStart / GAP;
                    if (ship.rect.intersectsLine(surfaceX[i - 1], surface[i - 1], surfaceX[i], surface[i])) {
                        if (i < landingpad || i > landingpad + landingpadSize / GAP) {
                            crash = true;
                        } else {
                            boolean tooHard = (ship.velX < 2 && ship.velY < 2) ? false : true;
                            boolean badAngle = (ship.angle <= PI / 36 && ship.angle >= -PI / 36) ? false: true;
                            if (!tooHard && !badAngle) {
                                touchDown = true;
                                ship.angle = 0;
                                score++;
                            } else {
                                crash = true;
                                if (tooHard)
                                    System.out.println("TOO HARD!");
                                if (badAngle)
                                    System.out.println("BAD ANGLE!");
                            }
                        }
                        ship.velX = 0;
                        ship.velY = 0;
                        running = false;
                        System.out.println(score);
                        break;
                    }
                }
                xStart++;
            }
        }

        //update rect pos from floats ship.xy
        ship.rect.x = (int)(ship.x + ship.xos);
        ship.rect.y = (int)(ship.y + ship.yos);
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }
    @Override
    public void keyTyped(KeyEvent e) { }
    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
    }
    @Override
    public void mousePressed(MouseEvent e) {
        keys[e.getButton()] = true;
        System.out.printf("click = %d\n",e.getButton());
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        keys[e.getButton()] = false;
    }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
}

