package tetris.settings;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import tetris.model.GameCommand;

// AI-assisted code start
/**
 * 로컬 대전 키 설정 (1P 7개, 2P 7개, 공용 2개). 1인 게임 키와 따로 저장한다.
 * 값은 JavaFX KeyCode 이름이다.
 */
public final class BattleKeyBindings {

    private static final Set<GameCommand> PLAYER_COMMANDS = Set.of(
            GameCommand.MOVE_LEFT, GameCommand.MOVE_RIGHT, GameCommand.SOFT_DROP,
            GameCommand.ROTATE_CLOCKWISE, GameCommand.ROTATE_COUNTERCLOCKWISE,
            GameCommand.HARD_DROP, GameCommand.HOLD);
    private static final Set<GameCommand> SHARED_COMMANDS = Set.of(GameCommand.PAUSE, GameCommand.QUIT_GAME);

    // 1P: 키보드 왼쪽 (오른손 T F G H, 왼손 A S D)
    private static final Map<GameCommand, String> DEFAULT_FIRST_PLAYER_KEYS = Map.of(
            GameCommand.MOVE_LEFT, "F",
            GameCommand.MOVE_RIGHT, "H",
            GameCommand.SOFT_DROP, "G",
            GameCommand.ROTATE_CLOCKWISE, "T",
            GameCommand.ROTATE_COUNTERCLOCKWISE, "A",
            GameCommand.HARD_DROP, "S",
            GameCommand.HOLD, "D");
    // 2P: 키보드 오른쪽 (오른손 방향키, 왼손 , . /)
    private static final Map<GameCommand, String> DEFAULT_SECOND_PLAYER_KEYS = Map.of(
            GameCommand.MOVE_LEFT, "LEFT",
            GameCommand.MOVE_RIGHT, "RIGHT",
            GameCommand.SOFT_DROP, "DOWN",
            GameCommand.ROTATE_CLOCKWISE, "UP",
            GameCommand.ROTATE_COUNTERCLOCKWISE, "COMMA",
            GameCommand.HARD_DROP, "PERIOD",
            GameCommand.HOLD, "SLASH");
    private static final Map<GameCommand, String> DEFAULT_SHARED_KEYS = Map.of(
            GameCommand.PAUSE, "P",
            GameCommand.QUIT_GAME, "ESCAPE");

    private final Map<GameCommand, String> firstPlayerKeys;
    private final Map<GameCommand, String> secondPlayerKeys;
    private final Map<GameCommand, String> sharedKeys;

    public BattleKeyBindings(Map<GameCommand, String> firstPlayerKeys, Map<GameCommand, String> secondPlayerKeys,
            Map<GameCommand, String> sharedKeys) {
        this.firstPlayerKeys = copyGroup(firstPlayerKeys, PLAYER_COMMANDS, "firstPlayerKeys");
        this.secondPlayerKeys = copyGroup(secondPlayerKeys, PLAYER_COMMANDS, "secondPlayerKeys");
        this.sharedKeys = copyGroup(sharedKeys, SHARED_COMMANDS, "sharedKeys");
    }

    public static BattleKeyBindings defaults() {
        return new BattleKeyBindings(DEFAULT_FIRST_PLAYER_KEYS, DEFAULT_SECOND_PLAYER_KEYS, DEFAULT_SHARED_KEYS);
    }

    /** 플레이어마다 하나씩 가지는 명령 7개 */
    public static Set<GameCommand> getPlayerCommands() {
        return PLAYER_COMMANDS;
    }

    /** 두 사람이 함께 쓰는 명령 2개 (일시정지, 경기 중단) */
    public static Set<GameCommand> getSharedCommands() {
        return SHARED_COMMANDS;
    }

    public Map<GameCommand, String> getFirstPlayerKeys() {
        return firstPlayerKeys;
    }

    public Map<GameCommand, String> getSecondPlayerKeys() {
        return secondPlayerKeys;
    }

    public Map<GameCommand, String> getSharedKeys() {
        return sharedKeys;
    }

    /**
     * 1P, 2P, 공용 키 16개 중 같은 키가 둘 이상의 명령에 배정되어 있는지 확인한다.
     * 키보드 하나를 둘이 나눠 쓰므로 묶음이 달라도 같은 키는 쓸 수 없다.
     *
     * @return 중복이 있으면 true
     */
    public boolean hasDuplicate() {
        Set<String> usedKeys = new HashSet<>();

        for (Map<GameCommand, String> group : List.of(firstPlayerKeys, secondPlayerKeys, sharedKeys)) {
            for (String keyName : group.values()) {
                if (!usedKeys.add(keyName)) {
                    return true;
                }
            }
        }

        return false;
    }

    // 정해진 명령을 정확히 다 가진 Map인지 확인하고 불변 복사본을 만든다
    private static Map<GameCommand, String> copyGroup(Map<GameCommand, String> keys, Set<GameCommand> commands,
            String name) {
        Objects.requireNonNull(keys, name);
        if (!keys.keySet().equals(commands)) {
            throw new IllegalArgumentException(name + " must have exactly " + commands);
        }
        return Map.copyOf(keys);
    }
}
// AI-assisted code end