package tetris.settings;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import tetris.model.GameCommand;

public class KeyBindingValidator {

    // AI-assisted code start
    /**
     * 주어진 키가 다른 명령에 이미 배정되어 있는지 찾는다.
     * 검사 대상 명령 자신에게 배정된 키는 충돌로 보지 않는다.
     *
     * @param keyBindings 현재 키 설정 (명령 -> 키 이름)
     * @param command 키를 바꾸려는 명령
     * @param keyName 새로 배정하려는 키 이름
     * @return 그 키를 이미 쓰고 있는 다른 명령, 없으면 빈 Optional
     */
    public Optional<GameCommand> findConflict(Map<GameCommand, String> keyBindings, GameCommand command, String keyName) {
        for (Map.Entry<GameCommand, String> binding : keyBindings.entrySet()) {
            if (binding.getKey() != command && binding.getValue().equals(keyName)) {
                return Optional.of(binding.getKey());
            }
        }

        return Optional.empty();
    }
    // AI-assisted code end

    // AI-assisted code start
    /**
     * 키 설정 안에 같은 키가 둘 이상의 명령에 배정되어 있는지 확인한다.
     *
     * @param keyBindings 확인할 키 설정 (명령 -> 키 이름)
     * @return 같은 키가 중복 배정되어 있으면 true, 아니면 false
     */
    public boolean hasDuplicate(Map<GameCommand, String> keyBindings) {
        Set<String> usedKeys = new HashSet<>();

        for(String keyName : keyBindings.values()) {
            if (!usedKeys.add(keyName)) {
                return true; 
            }
        }

        return false;
    }
    // AI-assisted code end
}
