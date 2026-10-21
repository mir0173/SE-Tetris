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

    public Map<PieceType, Integer> createWeights() {
        Map<PieceType, Integer> weights = new EnumMap<>(PieceType.class);
        for (PieceType type : PieceType.values()) {
            if (type.isBasic()) {
                weights.put(type, type == PieceType.I ? iWeight : otherWeight);
            }
        }
        return Map.copyOf(weights);
    }

    public long getLineDropIntervalStep() {
        return lineDropIntervalStep;
    }
}