package com.mygame.game_client;

import com.esotericsoftware.kryonet.Client;
import com.mygame.game_client.entites.ArrowEntity;
import com.mygame.game_client.packets.ArrowFiredCommand;
import com.mygame.game_client.packets.GamePoint;
import com.mygame.game_client.packets.PlayerSnapshot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameWindow extends JFrame {

    private final GamePanel gamePanel;
    private MoveSender moveSender;
    private ChopSender chopSender;
    private ArrowSender arrowSender;
    private final List<PlayerSnapshot> otherPlayers = new CopyOnWriteArrayList<>();
    private final List<GamePoint> visibleTrees = new CopyOnWriteArrayList<>();
    private final Player localPlayer = new Player(100,100); // or however you instantiate it
    private int localPlayerId = -1;
    private final List<ArrowEntity> arrows = new CopyOnWriteArrayList<>();
    public interface MoveSender {
        void sendMove(float x, float y);
    }
    public void setChopSender(ChopSender sender) {
        this.chopSender = sender;
    }

    public interface ChopSender {
        void sendChop(int x, int y);
    }
    public interface ArrowSender {
        void sendArrow(float x, float y, float tx, float ty);
    }
    public void setArrowSender(ArrowSender sender) {
        this.arrowSender = sender;
    }
    public GameWindow() {

        setTitle("Multiplayer Top-Down Game");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        setContentPane(gamePanel);
        setVisible(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int worldX = e.getX() + (int) localPlayer.x - getWidth() / 2;
                int worldY = e.getY() + (int) localPlayer.y - getHeight() / 2;

                if (localPlayer.shootingMode) {
                    if (localPlayer.hasArrow()) {
                        localPlayer.removeOneArrow();

                        float startX = localPlayer.x;
                        float startY = localPlayer.y;
                        float targetX = worldX;
                        float targetY = worldY;

                        ArrowEntity arrow = new ArrowEntity(startX, startY, targetX, targetY);
                        arrows.add(arrow);

                        if (arrowSender != null) {
                            arrowSender.sendArrow(startX, startY, targetX, targetY);
                        }

                        System.out.println("🏹 Arrow fired at " + targetX + ", " + targetY);
                    } else {
                        System.out.println("❌ No arrows!");
                    }
                    return;
                }
                // 1. Try to chop a tree
                for (GamePoint tree : visibleTrees) {
                    double dx = tree.x - worldX;
                    double dy = tree.y - worldY;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < 20) {
                        if (chopSender != null) {
                            chopSender.sendChop(tree.x, tree.y);
                            return; // don't move if chopping
                        }
                    }
                }

// 2. Otherwise, move
                if (moveSender != null) {
                    moveSender.sendMove(worldX, worldY);
                }
                localPlayer.targetX = worldX;
                localPlayer.targetY = worldY;
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_1) {
                    Player player = getPlayer();
                    if (player.hasBow()) {
                        player.shootingMode = !player.shootingMode;
                        System.out.println(player.shootingMode ? "🎯 Shooting mode ON" : "🛑 Shooting mode OFF");
                    }
                }
            }
        });
        setFocusable(true); // 🔑 needed to receive keyboard input
        // Refresh display ~60 FPS
        Timer timer = new Timer(16, e -> {
            for (ArrowEntity arrow : arrows) {
                arrow.update();
            }
            localPlayer.update(); // Smooth motion
            gamePanel.repaint();  // Redraw the screen

        });
        timer.start();
    }

    public void setMoveSender(MoveSender sender) {
        this.moveSender = sender;
    }

    public void updatePlayerPositions(List<PlayerSnapshot> snapshots) {
        otherPlayers.clear();
        otherPlayers.addAll(snapshots);
    }

    public Player getPlayer() {
        return localPlayer;
    }
    public void setVisibleTrees(List<GamePoint> trees) {
        visibleTrees.clear();
        visibleTrees.addAll(trees);
    }
    public void setLocalPlayerId(int id) {
        this.localPlayerId = id;
    }

    private class GamePanel extends JPanel {

        private Image spriteSheet;
        private final int frameWidth = 64;
        private final int frameHeight = 64;

        public GamePanel() {
            setDoubleBuffered(true);
            try {
                spriteSheet = new ImageIcon(getClass().getResource("/sprites/main_character_archer_walking.png")).getImage();
            } catch (Exception e) {
                System.err.println("❌ Failed to load sprite: " + e.getMessage());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // Step 1. Camera: calculate top-left corner of visible area
            int camX = (int) localPlayer.x - getWidth() / 2;
            int camY = (int) localPlayer.y - getHeight() / 2;

            // Step 2. Background
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Step 3. Draw Trees relative to camera
            g2.setColor(Color.GREEN.darker());
            for (GamePoint tree : visibleTrees) {
                int tx = tree.x - camX;
                int ty = tree.y - camY;
                int size = 20;

                int[] xPoints = {tx, tx - size / 2, tx + size / 2};
                int[] yPoints = {ty - size, ty + size / 2, ty + size / 2};
                g2.fillPolygon(xPoints, yPoints, 3);
            }

            // Step 4. Draw Other Players (excluding local player)
            g2.setColor(Color.BLUE);
            for (PlayerSnapshot p : otherPlayers) {
                if (p.id == localPlayerId) continue; // Optional: skip rendering yourself
                int px = (int) p.x - camX;
                int py = (int) p.y - camY;
                g2.fillOval(px - 10, py - 10, 20, 20);
            }
            g2.setColor(Color.YELLOW);
            for (ArrowEntity arrow : arrows) {
                int sx = (int) arrow.x - camX;
                int sy = (int) arrow.y - camY;
                g2.fillOval(sx - 2, sy - 2, 5, 5); // small circle for arrow
            }

            // Step 5. Draw Local Player centered on screen
            g2.setColor(Color.GREEN);
            g2.fillOval(getWidth() / 2 - 10, getHeight() / 2 - 10, 20, 20);
        }
    }
}