package com.mygame.game_client;


import com.esotericsoftware.kryonet.Client;
import com.mygame.game_client.items.Arrow;
import com.mygame.game_client.packets.*;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Connection;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class GameClient {
    private final Client client;
    private final GameWindow gameWindow;
    private Timer positionSyncTimer;
    private float lastSentX = -1;
    private float lastSentY = -1;
    public GameClient() {
        client = new Client();
        gameWindow = new GameWindow();
        registerPackets(client.getKryo());

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof WorldState state) {
                    gameWindow.updatePlayerPositions(state.players);
                } else if (object instanceof WorldChunkUpdate update) {
//                    System.out.println("📦 Client received " + update.trees.size() + " trees");
                    gameWindow.setVisibleTrees(update.trees);
                    gameWindow.setVisibleArrows(update.arrows);
                    gameWindow.setVisibleNpcs(update.npcs);
                }
            }
        });

        gameWindow.setMoveSender((x, y) -> {
            MoveCommand move = new MoveCommand();
            move.playerId = 1;
            move.x = x;
            move.y = y;
            client.sendTCP(move);
        });
    }

    private void registerPackets(Kryo kryo) {
        kryo.register(RegisterName.class);
        kryo.register(MoveCommand.class);
        kryo.register(PlayerSnapshot.class);
        kryo.register(WorldState.class);
        kryo.register(WorldChunkUpdate.class);
        kryo.register(GamePoint.class);
        kryo.register(ArrayList.class);
        kryo.register(ChopTreeCommand.class);
        kryo.register(Arrow.class);
        kryo.register(ArrowFiredCommand.class);
        kryo.register(ArrowData.class);
        kryo.register(MoveNPCCommand.class);
        kryo.register(NpcSnapshot.class);

    }

    public void start() throws IOException {
        client.start();
        client.connect(5000, "localhost", 54555, 54777);
        // Start syncing position every 100ms
        positionSyncTimer = new Timer(100, e -> {
            float currentX = gameWindow.getPlayer().x;
            float currentY = gameWindow.getPlayer().y;

// Only send if position changed
            if (currentX != lastSentX || currentY != lastSentY) {
                MoveCommand move = new MoveCommand();
                move.playerId = 1;
                move.x = currentX;
                move.y = currentY;
                client.sendUDP(move);

                System.out.println("📤 Sending move: " + currentX + ", " + currentY);

                lastSentX = currentX;
                lastSentY = currentY;
            }
        });
        positionSyncTimer.start();
        RegisterName register = new RegisterName();
        register.name = "Player_" + System.currentTimeMillis();
        client.sendTCP(register);
        gameWindow.setLocalPlayerId(1);
        // ✅ Set up chop sender
        gameWindow.setChopSender((x, y) -> {
            ChopTreeCommand chop = new ChopTreeCommand(x, y);
            client.sendTCP(chop);
        });
        gameWindow.setArrowSender((x, y, tx, ty) -> {
            client.sendTCP(new ArrowFiredCommand(x, y, tx, ty));
            gameWindow.markArrowFired();
        });
        gameWindow.setNpcMoveSender((id, x, y) -> {
            client.sendTCP(new MoveNPCCommand(id, x, y));
        });
        SwingUtilities.invokeLater(() -> gameWindow.setVisible(true));
    }

    public static void main(String[] args) throws IOException {
        new GameClient().start();
    }
}