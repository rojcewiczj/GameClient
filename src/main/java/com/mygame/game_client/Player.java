package com.mygame.game_client;

public class Player {
    public float x, y;
    public float targetX, targetY;
    public float speed = 4f; // pixels per frame

    public Player(float x, float y) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
    }

    public void update() {
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > speed) {
            x += speed * dx / dist;
            y += speed * dy / dist;
        } else {
            x = targetX;
            y = targetY;
        }
    }
}