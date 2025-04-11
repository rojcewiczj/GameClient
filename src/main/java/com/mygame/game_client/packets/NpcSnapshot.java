package com.mygame.game_client.packets;

public class NpcSnapshot {
    public int id;
    public float x, y;

    public NpcSnapshot() {} // Kryo needs this

    public NpcSnapshot(int id, float x, float y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
}