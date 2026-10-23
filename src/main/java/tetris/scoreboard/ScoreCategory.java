package tetris.scoreboard;

import tetris.model.Difficulty;
import tetris.model.GameMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record ScoreCategory(GameMode mode, Difficulty difficulty) {
    // AI-assisted code start
    private static final List<ScoreCategory> ALL_CATEGORIES = createAllCategories();

    public ScoreCategory {
        Objects.requireNonNull(mode, "mode is null");
        Objects.requireNonNull(difficulty, "difficulty is null");
    }

    public static List<ScoreCategory> getAllCategories() {
        return ALL_CATEGORIES;
    }

    private static List<ScoreCategory> createAllCategories() {
        List<ScoreCategory> categories = new ArrayList<>();
        for (GameMode mode : GameMode.values()) {
            for (Difficulty difficulty : Difficulty.values()) {
                categories.add(new ScoreCategory(mode, difficulty));
            }
        }
        return List.copyOf(categories);
    }

    /**
     * 난이도는 그대로 두고 모드만 offset만큼 이동한 조합을 반환한다.
     * 양 끝은 이어진다. (마지막 모드에서 다음은 첫 모드, 첫 모드에서 이전은 마지막 모드)
     *
     * @param offset 이동할 칸 수 (음수는 이전, 양수는 다음)
     * @return 모드만 바뀐 조합
     */
    public ScoreCategory moveMode(int offset) {
        GameMode[] modes = GameMode.values();
        int movedIndex = Math.floorMod(mode.ordinal() + offset, modes.length);
        return new ScoreCategory(modes[movedIndex], difficulty);
    }

    public ScoreCategory moveDifficulty(int offset) {
        Difficulty[] difficulties = Difficulty.values();
        int movedIndex = Math.floorMod(difficulty.ordinal() + offset, difficulties.length);
        return new ScoreCategory(mode, difficulties[movedIndex]);
    }
    // AI-assisted code end
}