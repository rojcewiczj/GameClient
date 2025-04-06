package com.mygame.game_client.items;

public class Item {
    public final String name;

    public Item(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
