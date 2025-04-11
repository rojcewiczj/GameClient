package com.mygame.game_client.entites;

public class ArrowEntity {
    public float x, y;
    private final float targetX, targetY;
    private final float vx, vy;
    private final float speed = 10f;
    private boolean stopped = false;
    private long spawnTime;
    public ArrowEntity(float startX, float startY, float targetX, float targetY) {
        this.spawnTime = System.currentTimeMillis();
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;

        float dx = targetX - startX;
        float dy = targetY - startY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        this.vx = dx / length * speed;
        this.vy = dy / length * speed;
    }

    public void update() {
        if (stopped) return;

        float dx = targetX - x;
        float dy = targetY - y;
        float distToTarget = (float) Math.sqrt(dx * dx + dy * dy);

        if (distToTarget <= speed) {
            x = targetX;
            y = targetY;
            stopped = true;
        } else {
            x += vx;
            y += vy;
        }

    }

    public boolean isStopped() {
        return stopped;
    }
    public boolean shouldDespawn() {
        return isStopped() && System.currentTimeMillis() - spawnTime > 500000;
    }
}