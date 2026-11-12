package tetris.network;

import java.util.Objects;

import tetris.model.Difficulty;

/** 경기 설정 */
public record LobbySettings(LobbyMode mode, Difficulty difficulty, int timeLimitSeconds) {
    public static final int DEFAULT_TIME_LIMIT_SECONDS = 120;
    public static final int MIN_TIME_LIMIT_SECONDS = 10;
    public static final int MAX_TIME_LIMIT_SECONDS = 3_600;

    public LobbySettings {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(difficulty, "difficulty");
        if (mode == LobbyMode.TIME_LIMIT) {
            if (timeLimitSeconds < MIN_TIME_LIMIT_SECONDS || timeLimitSeconds > MAX_TIME_LIMIT_SECONDS) {
                throw new IllegalArgumentException("제한시간은 10~3600초 여야 합니다.");
            }
        } else if (timeLimitSeconds != 0) {
            throw new IllegalArgumentException("시간제한 모드 외에는 제한시간을 0으로 지정합니다.");
        }
    }
}
