package tetris.network;

/** 대전 규칙 (기존 GameMode와 분리) */
public enum LobbyMode {
    NORMAL("일반"), ITEM("아이템"), TIME_LIMIT("제한시간");

    private final String displayName;

    LobbyMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
