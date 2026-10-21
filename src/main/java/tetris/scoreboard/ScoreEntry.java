package tetris.scoreboard;

import java.util.Objects;
import java.util.UUID;

import tetris.model.Difficulty;
import tetris.model.GameMode;

public class ScoreEntry {

    private final UUID id;
    private final String name;
    private final long score;
    private final GameMode mode;
    private final Difficulty difficulty;

    public ScoreEntry(UUID id, String name, long score, GameMode mode, Difficulty difficulty) {
        this.id = id;
        this.name = name;
        this.score = score;
        this.mode = Objects.requireNonNull(mode, "mode is null");
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty is null");
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

    public GameMode getMode() {
        return mode;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }
}