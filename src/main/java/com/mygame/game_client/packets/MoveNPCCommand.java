package com.mygame.game_client.packets;

public class MoveNPCCommand {
    public int npcId;
    public float targetX, targetY;

    public MoveNPCCommand(int id, float x, float y) {
        this.npcId = id;
        this.targetX = x;
        this.targetY = y;
    }

    // Optional: No-arg constructor for KryoNet (required for serialization)
    public MoveNPCCommand() {}
}