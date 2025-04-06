package com.mygame.game_client.entites;

public class ArrowEntity {
    public float x, y;
    private final float vx, vy;
    private final float speed = 10f;

    public ArrowEntity(float startX, float startY, float targetX, float targetY) {
        this.x = startX;
        this.y = startY;

        float dx = targetX - startX;
        float dy = targetY - startY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        this.vx = dx / length * speed;
        this.vy = dy / length * speed;
    }

    public void update() {
        x += vx;
        y += vy;
    }
}