package tetris.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import tetris.model.GameCommand;

class KeyBindingValidatorTest {

    private final KeyBindingValidator validator = new KeyBindingValidator();

    // AI-assisted code start
    @Test
    void findNoConflictWhenKeyIsUnused() {
        // given
        Map<GameCommand, String> keyBindings = GameSettings.getDefaultKeyBindings();

        // when
        Optional<GameCommand> conflict = validator.findConflict(keyBindings, GameCommand.HOLD, "X");

        // then
        assertTrue(conflict.isEmpty());
    }

    @Test
    void findConflictingCommandWhenKeyIsUsedByOtherCommand() {
        // given: 기본 설정에서 Z 는 ROTATE_COUNTERCLOCKWISE 가 쓰고 있다
        Map<GameCommand, String> keyBindings = GameSettings.getDefaultKeyBindings();

        // when
        Optional<GameCommand> conflict = validator.findConflict(keyBindings, GameCommand.HOLD, "Z");

        // then
        assertEquals(Optional.of(GameCommand.ROTATE_COUNTERCLOCKWISE), conflict);
    }

    @Test
    void ignoreCommandsOwnCurrentKey() {
        // given: HOLD 의 현재 키는 C 이다
        Map<GameCommand, String> keyBindings = GameSettings.getDefaultKeyBindings();

        // when: HOLD 에 같은 키 C 를 다시 배정하려 한다
        Optional<GameCommand> conflict = validator.findConflict(keyBindings, GameCommand.HOLD, "C");

        // then: 자기 자신과는 충돌하지 않는다
        assertTrue(conflict.isEmpty());
    }

    @Test
    void findNoConflictInEmptyKeyBindings() {
        // given
        Map<GameCommand, String> keyBindings = Map.of();

        // when
        Optional<GameCommand> conflict = validator.findConflict(keyBindings, GameCommand.HOLD, "Z");

        // then
        assertTrue(conflict.isEmpty());
    }

    @Test
    void notModifyKeyBindingsWhenFindingConflict() {
        // given
        Map<GameCommand, String> keyBindings = new HashMap<>(GameSettings.getDefaultKeyBindings());
        Map<GameCommand, String> original = Map.copyOf(keyBindings);

        // when
        validator.findConflict(keyBindings, GameCommand.HOLD, "Z");

        // then
        assertEquals(original, keyBindings);
    }

    @Test
    void reportNoDuplicateForDefaultKeyBindings() {
        // given: 기본 키 설정은 명령마다 서로 다른 키여야 한다
        Map<GameCommand, String> keyBindings = GameSettings.getDefaultKeyBindings();

        // when
        boolean hasDuplicate = validator.hasDuplicate(keyBindings);

        // then
        assertFalse(hasDuplicate);
    }

    @Test
    void reportDuplicateWhenSameKeyIsAssignedToTwoCommands() {
        // given: HOLD 에 ROTATE_COUNTERCLOCKWISE 와 같은 Z 를 배정한 설정
        Map<GameCommand, String> keyBindings = new HashMap<>(GameSettings.getDefaultKeyBindings());
        keyBindings.put(GameCommand.HOLD, "Z");

        // when
        boolean hasDuplicate = validator.hasDuplicate(keyBindings);

        // then
        assertTrue(hasDuplicate);
    }

    @Test
    void reportDuplicateWhenSameKeyIsAssignedToThreeCommands() {
        // given
        Map<GameCommand, String> keyBindings = Map.of(
            GameCommand.MOVE_LEFT, "A",
            GameCommand.MOVE_RIGHT, "A",
            GameCommand.HOLD, "A"
        );

        // when
        boolean hasDuplicate = validator.hasDuplicate(keyBindings);

        // then
        assertTrue(hasDuplicate);
    }

    @Test
    void reportNoDuplicateForEmptyKeyBindings() {
        // given
        Map<GameCommand, String> keyBindings = Map.of();

        // when
        boolean hasDuplicate = validator.hasDuplicate(keyBindings);

        // then
        assertFalse(hasDuplicate);
    }
    // AI-assisted code end
}
