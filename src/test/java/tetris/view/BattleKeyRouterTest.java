package tetris.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tetris.model.GameCommand;
import tetris.settings.BattleKeyBindings;

/**
 * 로컬 대전 키 분배
 * 창 전체에서 받은 키 이름(JavaFX KeyCode 이름)을 보고 1P / 2P / 공용 중 누구의 어떤 명령인지 찾는다.
 * JavaFX 없이 키 이름 문자열만으로 판단한다.
 */
class BattleKeyRouterTest {

    // AI-assisted code start

    @Test
    void routeEveryFirstPlayerKeyToFirstPlayer() {
        // given
        BattleKeyBindings bindings = BattleKeyBindings.defaults();
        BattleKeyRouter router = new BattleKeyRouter(bindings);

        for (Map.Entry<GameCommand, String> binding : bindings.getFirstPlayerKeys().entrySet()) {
            // when
            Optional<BattleKeyRouter.Route> route = router.findRoute(binding.getValue());

            // then
            assertEquals(Optional.of(new BattleKeyRouter.Route(BattleKeyRouter.Target.FIRST_PLAYER,
                    binding.getKey())), route, binding.getValue());
        }
    }

    @Test
    void routeEverySecondPlayerKeyToSecondPlayer() {
        // given
        BattleKeyBindings bindings = BattleKeyBindings.defaults();
        BattleKeyRouter router = new BattleKeyRouter(bindings);

        for (Map.Entry<GameCommand, String> binding : bindings.getSecondPlayerKeys().entrySet()) {
            // when
            Optional<BattleKeyRouter.Route> route = router.findRoute(binding.getValue());

            // then
            assertEquals(Optional.of(new BattleKeyRouter.Route(BattleKeyRouter.Target.SECOND_PLAYER,
                    binding.getKey())), route, binding.getValue());
        }
    }

    @Test
    void routePauseAndQuitToShared() {
        // given
        BattleKeyRouter router = new BattleKeyRouter(BattleKeyBindings.defaults());

        // when
        Optional<BattleKeyRouter.Route> pause = router.findRoute("P");
        Optional<BattleKeyRouter.Route> quit = router.findRoute("ESCAPE");

        // then
        assertEquals(Optional.of(new BattleKeyRouter.Route(BattleKeyRouter.Target.SHARED, GameCommand.PAUSE)),
                pause);
        assertEquals(Optional.of(new BattleKeyRouter.Route(BattleKeyRouter.Target.SHARED, GameCommand.QUIT_GAME)),
                quit);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SPACE", "Z", "C", "ENTER", "K"})
    void ignoreKeysNotUsedInBattle(String keyName) {
        // given: 1인 게임 키(SPACE, Z, C)나 아무도 쓰지 않는 키
        BattleKeyRouter router = new BattleKeyRouter(BattleKeyBindings.defaults());

        // when
        Optional<BattleKeyRouter.Route> route = router.findRoute(keyName);

        // then
        assertTrue(route.isEmpty(), keyName);
    }

    @Test
    void followCustomBattleKeys() {
        // given: 1P 왼쪽 이동을 F에서 Q로 바꿨다
        Map<GameCommand, String> first = new EnumMap<>(GameCommand.class);
        first.putAll(BattleKeyBindings.defaults().getFirstPlayerKeys());
        first.put(GameCommand.MOVE_LEFT, "Q");
        BattleKeyRouter router = new BattleKeyRouter(new BattleKeyBindings(first,
                BattleKeyBindings.defaults().getSecondPlayerKeys(), BattleKeyBindings.defaults().getSharedKeys()));

        // when
        Optional<BattleKeyRouter.Route> newKey = router.findRoute("Q");
        Optional<BattleKeyRouter.Route> oldKey = router.findRoute("F");

        // then: 새 키는 1P 왼쪽 이동, 예전 키는 아무 일도 하지 않는다
        assertEquals(Optional.of(new BattleKeyRouter.Route(BattleKeyRouter.Target.FIRST_PLAYER,
                GameCommand.MOVE_LEFT)), newKey);
        assertTrue(oldKey.isEmpty());
    }

    @Test
    void keepSameRouteWhenSameKeyIsPressedAgain() {
        // given: 키를 누르고 있으면 같은 키 이름이 반복해서 들어온다
        BattleKeyRouter router = new BattleKeyRouter(BattleKeyBindings.defaults());

        // when
        Optional<BattleKeyRouter.Route> first = router.findRoute("LEFT");
        Optional<BattleKeyRouter.Route> second = router.findRoute("LEFT");

        // then: 분배기는 상태를 가지지 않으므로 매번 같은 답을 준다
        assertEquals(first, second);
    }

    @Test
    void rejectNullBindings() {
        // given

        // when & then
        assertThrows(NullPointerException.class, () -> new BattleKeyRouter(null));
    }
    // AI-assisted code end
}
