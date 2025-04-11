package com.mygame.game_client;

import com.mygame.game_client.items.Arrow;
import com.mygame.game_client.items.Bow;
import com.mygame.game_client.items.Item;

import java.util.ArrayList;
import java.util.List;

public class Player {
    public float x, y;
    public float targetX, targetY;
    public float speed = 4f;

    public final List<Item> inventory = new ArrayList<>();
    public boolean shootingMode = false;

    public Player(float x, float y) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;

        // ✅ Add starting bow and arrows for local testing
        inventory.add(new Bow());
        for (int i = 0; i < 100; i++) {
            inventory.add(new Arrow());
        }
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

    public boolean hasBow() {
        return inventory.stream().anyMatch(item -> item instanceof Bow);
    }

    public boolean hasArrow() {
        return inventory.stream().anyMatch(item -> item instanceof Arrow);
    }

    public void removeOneArrow() {
        for (Item item : inventory) {
            if (item instanceof Arrow) {
                inventory.remove(item);
                return;
            }
        }
    }
}