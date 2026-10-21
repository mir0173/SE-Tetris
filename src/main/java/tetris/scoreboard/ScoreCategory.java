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
     * offset만큼 이동한 카테고리를 반환한다.
     * 마지막에서 다음은 첫 번째 카테고리로, 첫 번째에서 이전은 마지막 카테고리로 이동한다.
     * 
     * @param offset 이동할 카테고리 수 (음수는 이전, 양수는 다음)
     * @return 이동한 카테고리
     */
    public ScoreCategory move(int offset) {
        int index = ALL_CATEGORIES.indexOf(this);
        int movedIndex = Math.floorMod(index + offset, ALL_CATEGORIES.size());
        return ALL_CATEGORIES.get(movedIndex);
    }
    // AI-assisted code end
}