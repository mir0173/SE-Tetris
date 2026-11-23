package tetris.view;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import tetris.model.GameCommand;
import tetris.settings.BattleKeyBindings;

// AI-assisted code start
/**
 * 로컬 대전에서 창 전체로 들어온 키를 1P / 2P / 공용 명령으로 나눈다.
 * 키 이름(JavaFX KeyCode 이름) 문자열만 보고 판단하므로 JavaFX 없이 테스트할 수 있다.
 */
public final class BattleKeyRouter {

    /** 키를 받을 대상 */
    public enum Target {
        FIRST_PLAYER,
        SECOND_PLAYER,
        SHARED
    }

    /** 키 하나가 향할 곳: 누구에게, 어떤 명령으로 */
    public record Route(Target target, GameCommand command) {
    }

    private final Map<String, Route> routes = new HashMap<>(); // 키 이름 → 대상과 명령

    public BattleKeyRouter(BattleKeyBindings bindings) {
        Objects.requireNonNull(bindings, "bindings");
        addRoutes(bindings.getFirstPlayerKeys(), Target.FIRST_PLAYER);
        addRoutes(bindings.getSecondPlayerKeys(), Target.SECOND_PLAYER);
        addRoutes(bindings.getSharedKeys(), Target.SHARED);
    }

    /**
     * 키 이름에 해당하는 대상과 명령을 찾는다.
     *
     * @param keyName JavaFX KeyCode 이름 (예: "LEFT", "F")
     * @return 대전에서 쓰는 키면 대상과 명령, 아니면 빈 Optional
     */
    public Optional<Route> findRoute(String keyName) {
        return Optional.ofNullable(routes.get(keyName));
    }

    private void addRoutes(Map<GameCommand, String> keys, Target target) {
        for (Map.Entry<GameCommand, String> entry : keys.entrySet()) {
            routes.put(entry.getValue(), new Route(target, entry.getKey()));
        }
    }
}
// AI-assisted code end