package com.mygame.game_client.packets;

import java.util.List;

public class WorldChunkUpdate {
    public List<GamePoint> trees;
    public List<ArrowData> arrows;
    public List<NpcSnapshot> npcs;
}