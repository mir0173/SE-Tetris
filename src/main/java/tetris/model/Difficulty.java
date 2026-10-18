package tetris.model;

import java.util.EnumMap;
import java.util.Map;

public enum Difficulty {
    EASY("Easy", 36, 29, 80L),
    NORMAL("Normal", 30, 30, 100L),
    HARD("Hard", 24, 31, 120L);

    private final String displayName;
    private final int iWeight;
    private final int otherWeight;
    private final long lineDropIntervalStep;

    Difficulty(String displayName, int iWeight, int otherWeight, long lineDropIntervalStep) {
        this.displayName = displayName;
        this.iWeight = iWeight;
        this.otherWeight = otherWeight;
        this.lineDropIntervalStep = lineDropIntervalStep;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Map<TetrominoType, Integer> createWeights() {
        Map<TetrominoType, Integer> weights = new EnumMap<>(TetrominoType.class);
        for (TetrominoType type : TetrominoType.values()) {
            weights.put(type, type == TetrominoType.I ? iWeight : otherWeight);
        }
        return Map.copyOf(weights);
    }

    public long getLineDropIntervalStep() {
        return lineDropIntervalStep;
    }
}