package tetris.scoreboard;

import java.util.UUID;

public class ScoreEntry {

    private final UUID id;
    private final String name;
    private final long score;

    public ScoreEntry(UUID id, String name, long score) {
        this.id = id;
        this.name = name;
        this.score = score;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getScore() {
        return score;
    }
}