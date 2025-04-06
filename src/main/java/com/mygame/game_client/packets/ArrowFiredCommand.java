package com.mygame.game_client.packets;

public class ArrowFiredCommand {
    public float x;
    public float y;
    public float targetX;
    public float targetY;

    public ArrowFiredCommand() {} // Required for Kryo

    public ArrowFiredCommand(float x, float y, float targetX, float targetY) {
        this.x = x;
        this.y = y;
        this.targetX = targetX;
        this.targetY = targetY;
    }
}