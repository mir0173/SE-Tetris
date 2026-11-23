package tetris.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tetris.model.GameCommand;

/**
 * 로컬 대전 키 설정
 * - 1P 7개, 2P 7개, 공용 2개(일시정지, 경기 중단)를 따로 가진다. 1인 게임 키와는 서로 영향이 없다.
 * - 기본값: 1P는 키보드 왼쪽(T F G H + A S D), 2P는 키보드 오른쪽(방향키 + , . /), 공용은 P와 ESC
 * - 값은 JavaFX KeyCode 이름이다.
 */
class BattleKeyBindingsTest {

    // AI-assisted code start
    private static final Set<GameCommand> PLAYER_COMMANDS = Set.of(
            GameCommand.MOVE_LEFT, GameCommand.MOVE_RIGHT, GameCommand.SOFT_DROP,
            GameCommand.ROTATE_CLOCKWISE, GameCommand.ROTATE_COUNTERCLOCKWISE,
            GameCommand.HARD_DROP, GameCommand.HOLD);
    private static final Set<GameCommand> SHARED_COMMANDS = Set.of(GameCommand.PAUSE, GameCommand.QUIT_GAME);

    // ---------- 명령 묶음 ----------

    @Test
    void providePlayerAndSharedCommandGroups() {
        // given

        // when
        Set<GameCommand> playerCommands = BattleKeyBindings.getPlayerCommands();
        Set<GameCommand> sharedCommands = BattleKeyBindings.getSharedCommands();

        // then: NONE을 뺀 모든 명령이 두 묶음 중 정확히 한 곳에 들어 있다
        assertEquals(PLAYER_COMMANDS, playerCommands);
        assertEquals(SHARED_COMMANDS, sharedCommands);
        for (GameCommand command : GameCommand.values()) {
            if (command != GameCommand.NONE) {
                assertTrue(playerCommands.contains(command) ^ sharedCommands.contains(command), command.name());
            }
        }
    }

    // ---------- 기본값 ----------

    @Test
    void useLeftSideOfKeyboardForFirstPlayerByDefault() {
        // given

        // when
        Map<GameCommand, String> keys = BattleKeyBindings.defaults().getFirstPlayerKeys();

        // then
        assertEquals(Map.of(
                GameCommand.MOVE_LEFT, "F",
                GameCommand.MOVE_RIGHT, "H",
                GameCommand.SOFT_DROP, "G",
                GameCommand.ROTATE_CLOCKWISE, "T",
                GameCommand.ROTATE_COUNTERCLOCKWISE, "A",
                GameCommand.HARD_DROP, "S",
                GameCommand.HOLD, "D"), keys);
    }

    @Test
    void useRightSideOfKeyboardForSecondPlayerByDefault() {
        // given

        // when
        Map<GameCommand, String> keys = BattleKeyBindings.defaults().getSecondPlayerKeys();

        // then
        assertEquals(Map.of(
                GameCommand.MOVE_LEFT, "LEFT",
                GameCommand.MOVE_RIGHT, "RIGHT",
                GameCommand.SOFT_DROP, "DOWN",
                GameCommand.ROTATE_CLOCKWISE, "UP",
                GameCommand.ROTATE_COUNTERCLOCKWISE, "COMMA",
                GameCommand.HARD_DROP, "PERIOD",
                GameCommand.HOLD, "SLASH"), keys);
    }

    @Test
    void usePauseAndEscapeAsSharedKeysByDefault() {
        // given

        // when
        Map<GameCommand, String> keys = BattleKeyBindings.defaults().getSharedKeys();

        // then
        assertEquals(Map.of(GameCommand.PAUSE, "P", GameCommand.QUIT_GAME, "ESCAPE"), keys);
    }

    @Test
    void haveNoDuplicateInDefaults() {
        // given

        // when
        boolean duplicated = BattleKeyBindings.defaults().hasDuplicate();

        // then
        assertFalse(duplicated);
    }

    // ---------- 생성 ----------

    @Test
    void keepGivenKeys() {
        // given
        Map<GameCommand, String> first = firstPlayerKeysWith(GameCommand.MOVE_LEFT, "Q");

        // when
        BattleKeyBindings bindings = new BattleKeyBindings(first, defaultSecond(), defaultShared());

        // then
        assertEquals(first, bindings.getFirstPlayerKeys());
        assertEquals(defaultSecond(), bindings.getSecondPlayerKeys());
        assertEquals(defaultShared(), bindings.getSharedKeys());
    }

    @Test
    void copyGivenMapsDefensively() {
        // given
        Map<GameCommand, String> first = firstPlayerKeysWith(GameCommand.MOVE_LEFT, "Q");
        BattleKeyBindings bindings = new BattleKeyBindings(first, defaultSecond(), defaultShared());

        // when: 만든 뒤에 원본 Map을 바꿔도
        first.put(GameCommand.MOVE_LEFT, "W");

        // then: 설정은 바뀌지 않는다
        assertEquals("Q", bindings.getFirstPlayerKeys().get(GameCommand.MOVE_LEFT));
    }

    @Test
    void keepKeyMapsUnmodifiable() {
        // given
        BattleKeyBindings bindings = BattleKeyBindings.defaults();

        // when & then
        assertThrows(UnsupportedOperationException.class,
                () -> bindings.getFirstPlayerKeys().put(GameCommand.HOLD, "X"));
        assertThrows(UnsupportedOperationException.class,
                () -> bindings.getSecondPlayerKeys().put(GameCommand.HOLD, "X"));
        assertThrows(UnsupportedOperationException.class,
                () -> bindings.getSharedKeys().put(GameCommand.PAUSE, "X"));
    }

    @Test
    void rejectNullMaps() {
        // given

        // when & then
        assertThrows(NullPointerException.class,
                () -> new BattleKeyBindings(null, defaultSecond(), defaultShared()));
        assertThrows(NullPointerException.class,
                () -> new BattleKeyBindings(defaultFirst(), null, defaultShared()));
        assertThrows(NullPointerException.class,
                () -> new BattleKeyBindings(defaultFirst(), defaultSecond(), null));
    }

    @Test
    void rejectPlayerKeysWithMissingCommand() {
        // given: 1P 키에서 홀드가 빠졌다
        Map<GameCommand, String> first = new EnumMap<>(defaultFirst());
        first.remove(GameCommand.HOLD);

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> new BattleKeyBindings(first, defaultSecond(), defaultShared()));
    }

    @Test
    void rejectSharedCommandInPlayerKeys() {
        // given: 2P 키에 공용 명령(일시정지)이 섞였다
        Map<GameCommand, String> second = new EnumMap<>(defaultSecond());
        second.put(GameCommand.PAUSE, "Q");

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> new BattleKeyBindings(defaultFirst(), second, defaultShared()));
    }

    @Test
    void rejectPlayerCommandInSharedKeys() {
        // given: 공용 키에 플레이어 명령이 섞였다
        Map<GameCommand, String> shared = new EnumMap<>(defaultShared());
        shared.put(GameCommand.HOLD, "Q");

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> new BattleKeyBindings(defaultFirst(), defaultSecond(), shared));
    }

    // ---------- 중복 검사 (16개 전체) ----------

    @Test
    void findDuplicateBetweenFirstAndSecondPlayer() {
        // given: 1P 왼쪽 이동을 2P 왼쪽 이동과 같은 LEFT로 바꿨다
        BattleKeyBindings bindings = new BattleKeyBindings(
                firstPlayerKeysWith(GameCommand.MOVE_LEFT, "LEFT"), defaultSecond(), defaultShared());

        // when & then
        assertTrue(bindings.hasDuplicate());
    }

    @Test
    void findDuplicateBetweenPlayerAndSharedKeys() {
        // given: 2P 홀드를 공용 일시정지와 같은 P로 바꿨다
        Map<GameCommand, String> second = new EnumMap<>(defaultSecond());
        second.put(GameCommand.HOLD, "P");
        BattleKeyBindings bindings = new BattleKeyBindings(defaultFirst(), second, defaultShared());

        // when & then
        assertTrue(bindings.hasDuplicate());
    }

    @Test
    void findDuplicateInsideOnePlayer() {
        // given: 1P 홀드를 1P 하드 드롭과 같은 S로 바꿨다
        BattleKeyBindings bindings = new BattleKeyBindings(
                firstPlayerKeysWith(GameCommand.HOLD, "S"), defaultSecond(), defaultShared());

        // when & then
        assertTrue(bindings.hasDuplicate());
    }

    @Test
    void allowSameKeyAsSinglePlayerSettings() {
        // given: 1P 하드 드롭을 1인 게임 하드 드롭 키(SPACE)로 바꿔도 대전 키끼리만 비교한다
        BattleKeyBindings bindings = new BattleKeyBindings(
                firstPlayerKeysWith(GameCommand.HARD_DROP, "SPACE"), defaultSecond(), defaultShared());

        // when & then
        assertFalse(bindings.hasDuplicate());
    }

    // ---------- 도우미 ----------

    private static Map<GameCommand, String> defaultFirst() {
        return BattleKeyBindings.defaults().getFirstPlayerKeys();
    }

    private static Map<GameCommand, String> defaultSecond() {
        return BattleKeyBindings.defaults().getSecondPlayerKeys();
    }

    private static Map<GameCommand, String> defaultShared() {
        return BattleKeyBindings.defaults().getSharedKeys();
    }

    // 기본 1P 키에서 한 명령만 바꾼 수정 가능한 Map
    private static Map<GameCommand, String> firstPlayerKeysWith(GameCommand command, String keyName) {
        Map<GameCommand, String> keys = new EnumMap<>(defaultFirst());
        keys.put(command, keyName);
        return keys;
    }
    // AI-assisted code end
}
