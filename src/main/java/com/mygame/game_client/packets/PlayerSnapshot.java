package com.mygame.game_client.packets;

public class PlayerSnapshot {
    public int id;
    public float x;
    public float y;

    public PlayerSnapshot() {} // Kryo requires a no-arg constructor

    public PlayerSnapshot(int id, float x, float y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
}