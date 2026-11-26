package tetris.battle;

import java.util.List;
import java.util.Objects;

import tetris.model.Difficulty;
import tetris.model.GameKind;

/** 대전 설정(로컬 대전과 P2P 대전 함께 사용) */
public record BattleSettings(BattleMode mode, Difficulty difficulty, int timeLimitSeconds,
        GameKind gameKind) {
    public BattleSettings(BattleMode mode, Difficulty difficulty, int timeLimitSeconds) {
        this(mode, difficulty, timeLimitSeconds, GameKind.TETRIS);
    }

    public static final int DEFAULT_TIME_LIMIT_SECONDS = 120;
    public static final int MIN_TIME_LIMIT_SECONDS = 10;
    public static final int MAX_TIME_LIMIT_SECONDS = 3_600;

    public BattleSettings {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(gameKind, "gameKind");
        if (!modesFor(gameKind).contains(mode)) {
            throw new IllegalArgumentException("아이템 대전은 테트리스에서만 선택할 수 있습니다.");
        }
        if (mode == BattleMode.TIME_LIMIT) {
            if (timeLimitSeconds < MIN_TIME_LIMIT_SECONDS || timeLimitSeconds > MAX_TIME_LIMIT_SECONDS) {
                throw new IllegalArgumentException("제한시간은 10~3600초 여야 합니다.");
            }
        } else if (timeLimitSeconds != 0) {
            throw new IllegalArgumentException("시간제한 모드 외에는 제한시간을 0으로 지정합니다.");
        }
    }

    public static List<BattleMode> modesFor(GameKind kind) {
        return kind == GameKind.PUYO
                ? List.of(BattleMode.NORMAL, BattleMode.TIME_LIMIT)
                : List.of(BattleMode.values());
    }
}
