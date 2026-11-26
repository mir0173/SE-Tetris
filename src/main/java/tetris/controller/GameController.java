package tetris.controller;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import tetris.model.ControlledGame;
import tetris.model.Game;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.view.GameView;

/**
 * 게임 모델(Game)과 화면(GameView), 타이머(GameTimer)를 연결한다.
 * 화면에서 들어온 명령과 자동 낙하를 게임에 전달하고, 바뀐 상태를 화면에 다시 그린다
 */
public class GameController {
    public static final long MIN_LINE_CLEAR_DELAY_MILLIS = 100L; // 줄 삭제 연출 후 마무리까지 기다릴 최소 시간(밀리초)
    public static final long MAX_LINE_CLEAR_DELAY_MILLIS = 200L; // 줄 삭제 연출 후 마무리까지 기다릴 최대 시간(밀리초)

    private final ControlledGame game; // 게임 모델
    private final GameView view; // 게임 화면
    private final GameTimer timer; // 블록을 떨어트리는 게임 타이머
    private final DelayScheduler delayScheduler; // 블록 고정과 줄 삭제 연출 후 마무리를 예약하는 장치
    private final LongSupplier nanoTime;
    private final Runnable onReturnToMenu; // 사용자가 게임을 종료했을 때 실행할 동작
    private final Consumer<Long> onGameOver;

    private long appliedDropInterval = Game.getInitialDropInterval();
    private boolean timerRunning;
    private boolean started;
    private boolean terminalHandled;
    private boolean lineClearScheduled; // 삭제 마무리를 예약했는지 여부
    private LockDelay lockDelay;
    private Runnable onUpdate = () -> {}; // 화면을 갱신할 때마다 실행할 동작 (대전에서 MatchManager가 등록)

    /**
     * 게임, 화면, 타이머를 연결하는 컨트롤러를 생성한다.
     * 
     * @param game           진행할 게임
     * @param view           게임을 표시할 화면
     * @param timer          자동 낙하에 사용할 타이머
     * @param delayScheduler 블록 고정과 줄 삭제 연출 후 마무리를 예약하는 장치
     * @param onReturnToMenu 게임 종료(QUIT) 시 실행할 동작
     * @param onGameOver     게임 오버 시 최종 점수를 받을 동작
     */
    public GameController(ControlledGame game, GameView view, GameTimer timer, DelayScheduler delayScheduler,
            Runnable onReturnToMenu, Consumer<Long> onGameOver) {
        this(game, view, timer, delayScheduler, onReturnToMenu, onGameOver, System::nanoTime);
    }

    GameController(ControlledGame game, GameView view, GameTimer timer, DelayScheduler delayScheduler,
            Runnable onReturnToMenu, Consumer<Long> onGameOver, LongSupplier nanoTime) {
        this.game = game;
        this.view = view;
        this.timer = timer;
        this.delayScheduler = delayScheduler;
        this.nanoTime = nanoTime;
        this.onReturnToMenu = onReturnToMenu;
        this.onGameOver = onGameOver;
    }

    /**
     * 게임을 시작한다.
     * 키 입력 처리기를 등록하고, 첫 블록을 내보낸 뒤 화면을 띄우고, 자동 낙하 타이머를 시작한다.
     */
    public void start() {
        if (started) {
            return;
        }
        started = true;
        view.setCommandHandler(this::handleCommand);
        game.start();
        view.show();

        synchronizeState();
    }

    /**
     * 바깥에서 게임 갱신을 요청한다.
     */
    public void refresh() {
        if (terminalHandled) {
            return;
        }

        synchronizeState();
    }

    /** 모델의 현재 상태를 다시 그린다. 판정이나 타이머는 갱신하지 않는다. */
    public void refreshView() {
        if (started && !terminalHandled) {
            view.render(game.createSnapshot());
        }
    }

    /**
     * onUpdate 필드를 설정한다.
     */
    public void setOnUpdate(Runnable onUpdate) {
        if (onUpdate == null) {
            throw new NullPointerException("onUpdate is null");
        }

        this.onUpdate = onUpdate;
    }

    /**
     * 줄 삭제 연출 후 마무리까지 기다릴 시간을 계산한다.
     * 게임이 빨라질수록 짧아진다
     * 
     * @param dropInterval 블록 낙하 간격(밀리초)
     * @return 줄 삭제 연출 후 마무리까지 기다릴 시간(밀리초)
     */
    public static long calculateLineClearDelay(long dropInterval) {
        return Math.max(MIN_LINE_CLEAR_DELAY_MILLIS, Math.min(MAX_LINE_CLEAR_DELAY_MILLIS, dropInterval));
    }

    // 타이머 간격마다 호출 : 블록을 한 칸 내리고 화면 갱신, 게임 오버가 되면 타이머를 멈춘다.
    private void handleTick() {
        if (terminalHandled) {
            return;
        }

        game.tick();
        synchronizeState();
    }

    // 화면에서 들어온 명령을 게임에 적용하고 화면 갱신. QUIT이면 메뉴로 돌아가고, 게임 오버면 타이머를 멈춘다.
    private void handleCommand(GameCommand command) {
        if (terminalHandled) {
            return;
        }

        game.handleCommand(command);
        synchronizeState();
    }

    /**
     * 모델이 바뀐 뒤, 화면과 타이머를 지금 상태에 맞춘다.
     * View, MatchManager, Timer, 화면전환의 상태를 동기화한다.
     */
    private void synchronizeState() {

        // View 동기화 : snapshot을 찍어 화면에 출력한다.
        GameSnapshot snapshot = game.createSnapshot();
        view.render(snapshot);

        // MatchManager 동기화 : MatchManager에게 알린다.
        onUpdate.run();

        // (#41-2) onUpdate 안에서 심판이 stop() -> refresh로 이 메서드를 다시 실행했다면
        // 안쪽에서 이미 (1) 끝 처리를 했으므로 멈춘다. 이 코드가 없을 시 onGameOver가 두 번 불리는 문제 발생
        if (terminalHandled) {
            return;
        }

        // (1) 게임종료 || 게임오버시 타이머 정지, 끝 처리 표시 후 콜백을 부른다.
        // terminalHandled 가 true일 시 tick/키입력/refresh는 모두 무시된다.
        if (game.isQuit() || game.isGameOver()) {
            lockDelay = null;
            stopTimer();
            terminalHandled = true;
            if (game.isGameOver()) {
                onGameOver.accept(snapshot.getScore());
            } else {
                onReturnToMenu.run();
            }
            return;
        }

        // (2) 줄 삭제 연출중일 시 블록이 떨어지지 않게 타이머 정지
        // 연출 시간이 지난 뒤 finishLineClear로 실제 삭제하도록 한번만 예약한다.
        if (snapshot.getStatus() == GameStatus.CLEARING) {
            lockDelay = null;
            stopTimer();
            if (!lineClearScheduled) {
                lineClearScheduled = true;
                long defaultDelay = calculateLineClearDelay(snapshot.getDropInterval());
                delayScheduler.runAfter(game.clearStepDelayMillis(defaultDelay), this::finishLineClear);
            }
            return;
        }

        // (3) 일시정지 시 낙하를 멈추고 고정 대기 시간을 보존한다.
        if (game.isPaused()) {
            stopTimer();
            synchronizeLockDelay();
            return;
        }

        // (4) 게임 진행중일 시
        // 낙하 간격이 바뀌었으면 타이머에 반영
        long dropInterval = snapshot.getDropInterval();

        if (dropInterval != appliedDropInterval) {
            timer.setInterval(dropInterval);
            appliedDropInterval = dropInterval;
        }

        // 타이머가 꺼져있으면 켠다 : 게임 시작, 줄 삭제 애니메이션 마무리, 일시정지 재개 후
        if (!timerRunning) {
            timer.start(this::handleTick);
            timerRunning = true;
        }
        synchronizeLockDelay();
    }

    private void synchronizeLockDelay() {
        long id = game.getPendingLockDelayId();
        if (id == 0) {
            lockDelay = null;
            return;
        }
        if (lockDelay == null || lockDelay.id != id) {
            lockDelay = new LockDelay(id, game.lockDelayMillis());
        }
        LockDelay pending = lockDelay;
        if (!game.isLockDelayRunning()) {
            if (pending.attempt != null) {
                long elapsed = nanoTime.getAsLong() - pending.attempt.startedAtNanos();
                pending.remainingNanos = Math.max(0, pending.remainingNanos - elapsed);
                pending.attempt = null;
            }
            return;
        }
        if (pending.attempt != null) {
            return;
        }
        LockAttempt attempt = new LockAttempt(nanoTime.getAsLong());
        pending.attempt = attempt;
        long delayMillis = Math.max(1, Math.ceilDiv(pending.remainingNanos, TimeUnit.MILLISECONDS.toNanos(1)));
        delayScheduler.runAfter(delayMillis, () -> {
            if (lockDelay == pending && pending.attempt == attempt && game.finishLockDelay(pending.id)) {
                lockDelay = null;
                synchronizeState();
            }
        });
    }

    private static final class LockDelay {
        private final long id;
        private long remainingNanos;
        private LockAttempt attempt;

        private LockDelay(long id, long delayMillis) {
            this.id = id;
            remainingNanos = TimeUnit.MILLISECONDS.toNanos(delayMillis);
        }
    }

    private record LockAttempt(long startedAtNanos) {
    }

    private void finishLineClear() {
        lineClearScheduled = false;
        if (terminalHandled) {
            return;
        }
        game.finishLineClear();
        synchronizeState();
    }

    private void stopTimer() {
        if (timerRunning) {
            timer.stop();
            timerRunning = false;
        }
    }
}
