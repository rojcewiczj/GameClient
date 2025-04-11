package com.mygame.game_client;

import com.esotericsoftware.kryonet.Client;
import com.mygame.game_client.entites.ArrowEntity;
import com.mygame.game_client.entites.ArrowVisual;
import com.mygame.game_client.packets.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameWindow extends JFrame {
    private float prevCamX = 0;
    private float prevCamY = 0;
    private float camXf = 0;
    private float camYf = 0;
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    private final GamePanel gamePanel;
    private final Map<Integer, ArrowVisual> arrowMap = new ConcurrentHashMap<>();
    private MoveSender moveSender;
    private ChopSender chopSender;
    private ArrowSender arrowSender;
    private final List<PlayerSnapshot> otherPlayers = new CopyOnWriteArrayList<>();
    private final List<GamePoint> visibleTrees = new CopyOnWriteArrayList<>();
    private final Player localPlayer = new Player(100,100); // or however you instantiate it
    private int localPlayerId = -1;
    private long lastFiredArrowTime = 0;
    private int tempArrowId = -1;
    private int lastTempArrowId = Integer.MIN_VALUE;
    private int lastPredictedId = Integer.MIN_VALUE;
    private final List<NpcSnapshot> visibleNpcs = new CopyOnWriteArrayList<>();
    private NpcMoveSender npcMoveSender;
    private int selectedNpcId = -1;
    private final Map<Integer, PlayerSnapshot> playerMap = new ConcurrentHashMap<>();
    public void markArrowFired() {
        lastFiredArrowTime = System.currentTimeMillis();
    }

    public boolean isLikelyLocalArrow(float x, float y) {
        long now = System.currentTimeMillis();
        if (now - lastFiredArrowTime < 500) {
            return isNearLocalPlayer(x, y, 40);
        }
        return false;
    }
    public interface MoveSender {
        void sendMove(float x, float y);
    }
    public void setChopSender(ChopSender sender) {
        this.chopSender = sender;
    }
    public interface NpcMoveSender {
        void sendNpcMove(int npcId, int x, int y);
    }

    public void setNpcMoveSender(NpcMoveSender sender) {
        this.npcMoveSender = sender;
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

                int worldX = e.getX() + (int) prevCamX;
                int worldY = e.getY() + (int) prevCamY;

                if (localPlayer.shootingMode) {
                    if (localPlayer.hasArrow()) {
                        localPlayer.removeOneArrow();

                        float startX = localPlayer.x;
                        float startY = localPlayer.y;
                        float targetX = worldX;
                        float targetY = worldY;
                        float dx = targetX - startX;
                        float dy = targetY - startY;
                        float len = (float) Math.sqrt(dx * dx + dy * dy);

                        float speed = 10f; // or whatever speed your arrows use
                        float calcVX = dx / len * speed;
                        float calcVY = dy / len * speed;
                        // Create local predicted arrow
                        ArrowVisual predicted = new ArrowVisual(startX, startY, calcVX, calcVY);
                        arrowMap.put(tempArrowId, predicted);
                        lastTempArrowId = tempArrowId;
                        tempArrowId--; // for next use

                        // Send real arrow to server
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

                    if (dist < 30) {
                        if (chopSender != null) {
                            chopSender.sendChop(tree.x, tree.y);
                            System.out.println("tried to chop tree");
                            return; // don't move if chopping
                        }
                    }
                }

                for (NpcSnapshot npc : visibleNpcs) {
                    double dx = npc.x - worldX;
                    double dy = npc.y - worldY;
                    if (Math.sqrt(dx * dx + dy * dy) < 20) {
                        selectedNpcId = npc.id;
                        return;
                    }
                }

                if (selectedNpcId != -1) {
                    if (npcMoveSender != null) {
                        npcMoveSender.sendNpcMove(selectedNpcId, worldX, worldY);
                    }
                    selectedNpcId = -1;
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

            for (ArrowVisual arrow : arrowMap.values()) {
                arrow.update();
            }
            for (PlayerSnapshot p : playerMap.values()) {
                p.interpolate(0.15f); // adjust smoothing as needed
            }
            localPlayer.update(); // Smooth motion
            gamePanel.repaint();  // Redraw the screen

        });
        timer.start();
    }
    public void setVisibleNpcs(List<NpcSnapshot> npcs) {
        visibleNpcs.clear();
        visibleNpcs.addAll(npcs);
    }
    public void setMoveSender(MoveSender sender) {
        this.moveSender = sender;
    }

    public void updatePlayerPositions(List<PlayerSnapshot> snapshots) {
        Set<Integer> incomingIds = new HashSet<>();

        for (PlayerSnapshot incoming : snapshots) {
            incomingIds.add(incoming.id);

            PlayerSnapshot existing = playerMap.get(incoming.id);
            if (existing != null) {
                existing.updateServerPosition(incoming.x, incoming.y);
            } else {
                playerMap.put(incoming.id, new PlayerSnapshot(incoming.id, incoming.x, incoming.y));
            }
        }

        // 🧹 Remove players who no longer exist
        playerMap.keySet().removeIf(id -> !incomingIds.contains(id));
    }
    public Player getPlayer() {
        return localPlayer;
    }
    public void setVisibleTrees(List<GamePoint> trees) {
        visibleTrees.clear();
        visibleTrees.addAll(trees);
    }

    public void setVisibleArrows(List<ArrowData> arrows) {
        Set<Integer> incomingIds = new HashSet<>();

        for (ArrowData a : arrows) {
            incomingIds.add(a.id);

            ArrowVisual existing = arrowMap.get(a.id);
            if (existing != null) {
                existing.syncFromServer(a.x, a.y, a.vx, a.vy);
            } else {
                ArrowVisual arrow = new ArrowVisual(a.x, a.y, a.vx, a.vy);
                arrowMap.put(a.id, arrow);
            }
        }

        // Remove any arrows that aren't in this update
        arrowMap.keySet().removeIf(id -> !incomingIds.contains(id));
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    private boolean isNearLocalPlayer(float x, float y, float range) {
        float dx = localPlayer.x - x;
        float dy = localPlayer.y - y;
        return (dx * dx + dy * dy) <= (range * range);
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

            // Step 1. Smooth camera
            PlayerSnapshot self = playerMap.get(localPlayerId);

            if (self != null) {
                camXf = lerp(prevCamX, self.x - getWidth() / 2f, 0.1f);
                camYf = lerp(prevCamY, self.y - getHeight() / 2f, 0.1f);
                prevCamX = camXf;
                prevCamY = camYf;
            }

            int camX = (int) camXf;
            int camY = (int) camYf;

            // Step 2. Background
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Step 3. Draw Trees
            g2.setColor(Color.GREEN.darker());
            for (GamePoint tree : visibleTrees) {
                int tx = tree.x - camX;
                int ty = tree.y - camY;
                int size = 20;
                int[] xPoints = {tx, tx - size / 2, tx + size / 2};
                int[] yPoints = {ty - size, ty + size / 2, ty + size / 2};
                g2.fillPolygon(xPoints, yPoints, 3);
            }

            // Step 4. Draw Players
            for (PlayerSnapshot p : playerMap.values()) {
                if (p.id == localPlayerId) {
                    g2.setColor(Color.GREEN);
                } else {
                    g2.setColor(Color.BLUE);
                }
                int px = (int) p.x - camX;
                int py = (int) p.y - camY;
                g2.fillOval(px - 10, py - 10, 20, 20);
            }
            // Step X: Draw NPCs
            g2.setColor(Color.RED);
            for (NpcSnapshot npc : visibleNpcs) {
                int nx = (int) npc.x - camX;
                int ny = (int) npc.y - camY;
                g2.fillRect(nx - 6, ny - 6, 12, 12); // small red square for NPC
            }
            // Step 5. Draw Arrows
            g2.setColor(Color.YELLOW);
            for (ArrowVisual arrow : arrowMap.values()) {
                int ax = (int) arrow.getX() - camX;
                int ay = (int) arrow.getY() - camY;
                g2.fillOval(ax - 2, ay - 2, 5, 5);
            }
        }
    }
}