package com.mygame.game_client.entites;


public class ArrowVisual {
    private float x, y;
    private float vx, vy;
    private boolean stopped = false;

    public ArrowVisual(float x, float y, float vx, float vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.stopped = (vx == 0 && vy == 0);
    }

    public void update() {
        if (stopped) return;
        x += vx;
        y += vy;
    }

    public void syncFromServer(float x, float y, float vx, float vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.stopped = (vx == 0 && vy == 0);
    }

    public float getX() { return x; }
    public float getY() { return y; }
}