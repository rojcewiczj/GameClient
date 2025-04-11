package com.mygame.game_client.packets;

public class PlayerSnapshot {
    public int id;

    public float x, y;             // Current render position (interpolated)
    public float serverX, serverY; // Latest server-reported position

    private boolean initialized = false;

    public PlayerSnapshot() {} // Kryo requires no-arg constructor

    public PlayerSnapshot(int id, float x, float y) {
        this.id = id;
        this.serverX = x;
        this.serverY = y;
        this.x = x;
        this.y = y;
        this.initialized = true;
    }

    public void updateServerPosition(float sx, float sy) {
        this.serverX = sx;
        this.serverY = sy;

        if (!initialized) {
            this.x = sx;
            this.y = sy;
            initialized = true;
        }
    }

    public void interpolate(float smoothing) {
        this.x += (serverX - x) * smoothing;
        this.y += (serverY - y) * smoothing;
    }
}