package tetris.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Consumer;
import java.util.Random;

import org.junit.jupiter.api.Test;

import tetris.model.Board;
import tetris.model.Difficulty;
import tetris.model.Game;
import tetris.model.GameCommand;
import tetris.model.GameMode;
import tetris.model.GameSnapshot;
import tetris.model.TetrominoGenerator;
import tetris.model.PieceType;
import tetris.view.GameView;

class GameControllerTest {

    // AI-assisted code start
    private static final DelayScheduler IMMEDIATE_SCHEDULER = (delayMillis, task) -> task.run();

    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int GAME_OVER_BOARD_WIDTH = 4;
    private static final int GAME_OVER_BOARD_HEIGHT = 2;
    private static final int EXPECTED_RENDER_COUNT_AFTER_START = 1;
    private static final int EXPECTED_ONE_CALLBACK = 1;
    private static final long EXPECTED_ONE_DROP = 1L;

    @Test
    void startConnectsViewAndTimer() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        GameController controller = newController(new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL), view, timer, new Callback());

        controller.start();

        assertNotNull(view.commandHandler);
        assertTrue(view.showCalled);
        assertTrue(view.renderCount >= EXPECTED_RENDER_COUNT_AFTER_START);
        assertEquals(EXPECTED_ONE_CALLBACK, timer.startCount);
        assertTrue(timer.running);
    }

    @Test
    void runTimerTaskTicksAndRenders() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Game game = new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL);
        GameController controller = newController(game, view, timer, new Callback());
        controller.start();
        int renderCountBeforeTick = view.renderCount;

        timer.task.run();

        assertEquals(EXPECTED_ONE_DROP, game.getGravityDropCells());
        assertTrue(view.renderCount > renderCountBeforeTick);
    }

    @Test
    void renderAfterCommand() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        GameController controller = newController(
                new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL),
                view, timer, new Callback());
        controller.start();
        int renderCountBeforeCommand = view.renderCount;

        view.commandHandler.accept(GameCommand.MOVE_LEFT);

        assertTrue(view.renderCount > renderCountBeforeCommand);
    }

    @Test
    void stopTimerAndReturnToMenuOnQuit() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Callback callback = new Callback(timer);
        GameController controller = newController(
                new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL),
                view, timer, callback);
        controller.start();

        view.commandHandler.accept(GameCommand.QUIT_GAME);

        assertFalse(timer.running);
        assertEquals(EXPECTED_ONE_CALLBACK, callback.count);
        assertFalse(callback.runningWhenCalled);
    }

    @Test
    void stopTimerWithoutReturningToMenuOnGameOverCommand() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Callback callback = new Callback();
        Game game = new Game(new Board(GAME_OVER_BOARD_WIDTH, GAME_OVER_BOARD_HEIGHT), sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL);
        GameController controller = newController(game, view, timer, callback);
        controller.start();

        view.commandHandler.accept(GameCommand.HARD_DROP);

        assertTrue(game.isGameOver());
        assertFalse(timer.running);
        assertEquals(0, callback.count);
    }

    @Test
    void stopTimerWhenTimerTaskCausesGameOver() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Callback callback = new Callback();
        Game game = new Game(new Board(GAME_OVER_BOARD_WIDTH, GAME_OVER_BOARD_HEIGHT), sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL);
        GameController controller = newController(game, view, timer, callback);
        controller.start();
        timer.task.run();

        assertTrue(game.isGameOver());
        assertFalse(timer.running);
        assertEquals(0, callback.count);
    }

    @Test
    void ignoreCommandAfterQuit() {
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Callback callback = new Callback();
        GameController controller = newController(
                new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL),
                view, timer, callback);
        controller.start();
        view.commandHandler.accept(GameCommand.QUIT_GAME);
        int renderCountAfterQuit = view.renderCount;

        view.commandHandler.accept(GameCommand.MOVE_LEFT);

        assertEquals(EXPECTED_ONE_CALLBACK, callback.count);
        assertEquals(renderCountAfterQuit, view.renderCount);
    }

    @Test // 게임 시작과 타이머 틱의 모든 화면 렌더마다 onUpdate가 실행되는지 확인하는 테스트
    void runOnUpdateOnEveryRender() {
        // given: 화면, 타이머와 갱신 콜백을 연결한 컨트롤러를 준비한다.
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        GameController controller = newController(
                new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL),
                view, timer, new Callback());
        Callback updates = new Callback();
        controller.setOnUpdate(updates);

        // when: 게임을 시작하고 타이머 틱을 한 번 발생시킨다.
        controller.start();
        timer.task.run();

        // then: 콜백 횟수는 화면 렌더 횟수와 같다.
        assertEquals(view.renderCount, updates.count);
    }

    @Test // 게임오버를 발생시킨 마지막 렌더에서도 onUpdate가 실행되는지 확인하는 테스트
    void runOnUpdateOnGameOverRender() {
        // given: 작은 보드에서 O 블록으로 게임오버가 되는 컨트롤러를 준비한다.
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Game game = new Game(new Board(GAME_OVER_BOARD_WIDTH, GAME_OVER_BOARD_HEIGHT),
                sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL);
        GameController controller = newController(game, view, timer, new Callback());
        Callback updates = new Callback();
        controller.setOnUpdate(updates);

        // when: 게임을 시작하고 게임오버가 될 때까지 하드 드롭을 보낸다.
        controller.start();
        while (!game.isGameOver()) {
            view.commandHandler.accept(GameCommand.HARD_DROP);
        }

        // then: 마지막 게임오버 렌더를 포함해 모든 렌더마다 갱신 콜백이 실행된다.
        assertEquals(view.renderCount, updates.count);
        assertTrue(game.isGameOver());
    }

    @Test // 일시정지 후 정지하고 재개 후 다시 시작하는 타이머 상태를 refresh가 반영하는지 확인하는 테스트
    void restartTimerAfterResumeWithRefresh() {
        // given: 실행 중인 게임과 타이머를 준비한다.
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Game game = new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL);
        GameController controller = newController(game, view, timer, new Callback());
        controller.start();

        // when: 게임 상태를 바꾼 뒤 refresh로 컨트롤러에 동기화를 요청한다.
        game.pause();
        controller.refresh(); // pause()만으로는 타이머를 제어하지 않으므로 refresh가 필요하다.
        assertFalse(timer.running);
        game.resume();
        controller.refresh(); // resume() 뒤에도 컨트롤러만 타이머를 다시 시작할 수 있다.

        // then: 일시정지 중에는 멈추고 재개 후에는 실행 중이다.
        assertTrue(timer.running);
        assertEquals(EXPECTED_ONE_CALLBACK + EXPECTED_ONE_CALLBACK, timer.startCount);
        // 위의 모든 assert를 통과했을 시 아래 출력
        System.out.println("[REQ3-컨트롤러] PAUSED에서 정지 1회, PLAYING에서 재시작 1회 PASS");
    }

    @Test // 게임오버로 컨트롤러가 종료된 뒤 refresh가 추가 렌더를 발생시키지 않는지 확인하는 테스트
    void doNothingOnRefreshAfterGameOver() {
        // given: 작은 보드에서 하드 드롭으로 게임오버가 되는 컨트롤러를 준비한다.
        FakeGameView view = new FakeGameView();
        FakeGameTimer timer = new FakeGameTimer();
        Game game = new Game(new Board(GAME_OVER_BOARD_WIDTH, GAME_OVER_BOARD_HEIGHT),
                sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL);
        GameController controller = newController(game, view, timer, new Callback());
        Callback updates = new Callback();
        controller.setOnUpdate(updates);
        controller.start(); // 시작 렌더를 발생시킨다.
        view.commandHandler.accept(GameCommand.HARD_DROP);
        int renderCount = view.renderCount;
        int updateCount = updates.count;

        // when: 이미 종료된 컨트롤러를 다시 refresh한다.
        controller.refresh();

        // then: 종료 뒤에는 렌더 횟수가 늘지 않는다.
        assertEquals(renderCount, view.renderCount);
        assertEquals(updateCount, updates.count);
    }

    @Test // null 갱신 콜백을 등록하려는 시도를 거부하는지 확인하는 테스트
    void rejectNullOnUpdate() {
        // given: 콜백을 등록할 컨트롤러를 준비한다.
        GameController controller = newController(
                new Game(new Board(BOARD_WIDTH, BOARD_HEIGHT), sequenceGenerator(PieceType.O), Difficulty.NORMAL, GameMode.NORMAL),
                new FakeGameView(), new FakeGameTimer(), new Callback());

        // when / then: null 등록은 즉시 NullPointerException을 발생시킨다.
        assertThrows(NullPointerException.class, () -> controller.setOnUpdate(null));
    }

    private GameController newController(
            Game game, FakeGameView view, FakeGameTimer timer, Callback callback) {
        return new GameController(game, view, timer, IMMEDIATE_SCHEDULER, callback, score -> 
            {}
        );
    }

    private TetrominoGenerator sequenceGenerator(PieceType... types) {
        return new TetrominoGenerator(new Random(0L)) {
            private int index;

            @Override
            public PieceType generateNext() {
                PieceType type = types[Math.min(index, types.length - 1)];
                index++;
                return type;
            }
        };
    }

    private static class FakeGameView implements GameView {
        private Consumer<GameCommand> commandHandler;
        private int renderCount;
        private boolean showCalled;

        @Override
        public void setCommandHandler(Consumer<GameCommand> commandHandler) {
            this.commandHandler = commandHandler;
        }

        @Override
        public void render(GameSnapshot snapshot) {
            renderCount++;
        }

        @Override
        public void show() {
            showCalled = true;
        }

        @Override
        public void close() {
        }
    }

    private static class FakeGameTimer implements GameTimer {
        private Runnable task;
        private int startCount;
        private boolean running;

        @Override
        public void start(Runnable task) {
            this.task = task;
            startCount++;
            running = true;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public void setInterval(long intervalMillis) {
        }
    }

    private static class Callback implements Runnable {
        private int count;
        private boolean runningWhenCalled;
        private final FakeGameTimer timer;

        private Callback() {
            this(null);
        }

        private Callback(FakeGameTimer timer) {
            this.timer = timer;
        }

        @Override
        public void run() {
            count++;
            if (timer != null) {
                runningWhenCalled = timer.running;
            }
        }
    }
    // AI-assisted code end (테스트 코드)
}
