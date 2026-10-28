package tetris.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 1차 요구사항 15쪽의 플레이 성능을 모델 처리 시간으로 부분 검증한다.
 * 시간 제한은 팀의 판정 기준이며, 최소 사양 PC나 JavaFX 화면 성능을 증명하지는 않는다.
 */
class GamePerformanceTest {
    // AI-assisted code start
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int WARMUP_COUNT = 1_000;
    private static final int OPERATION_COUNT = 10_000;
    private static final long RANDOM_SEED = 42L;
    private static final long TOTAL_BUDGET_MILLIS = 5_000;
    private static final long P95_BUDGET_MILLIS = 16;
    private static final double P95_RATIO = 0.95;
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    @BeforeAll
    static void printEnvironment() {
        System.out.printf(Locale.ROOT, "[NFR-ENV] Java=%s, OS=%s %s, JVM heapLimit=%dMiB%n",
                System.getProperty("java.version"), System.getProperty("os.name"),
                System.getProperty("os.arch"), Runtime.getRuntime().maxMemory() / (1024 * 1024));
    }

    @ParameterizedTest(name = "mode={0}, difficulty={1}")
    @MethodSource("gameConfigurations")
    void processCommandsAndSnapshotsWithinTimeBudget(
            GameMode mode, Difficulty difficulty) {
        measureOperations(WARMUP_COUNT, mode, difficulty);
        PerformanceResult result = measureOperations(OPERATION_COUNT, mode, difficulty);

        boolean progressed = result.completedCycles() == OPERATION_COUNT;
        boolean totalWithinBudget = result.elapsedNanos()
                < TimeUnit.MILLISECONDS.toNanos(TOTAL_BUDGET_MILLIS);
        boolean p95WithinBudget = result.p95Nanos()
                <= TimeUnit.MILLISECONDS.toNanos(P95_BUDGET_MILLIS);

        System.out.printf(Locale.ROOT,
                "[NFR-PERF][%s/%s] cycles=%d/%d, total=%.3fms (<%dms), "
                        + "p95=%.3fms (<=%dms), max=%.3fms, heapLimit=%dMiB, result=%s%n",
                mode, difficulty, result.completedCycles(), OPERATION_COUNT,
                result.elapsedNanos() / NANOS_PER_MILLISECOND, TOTAL_BUDGET_MILLIS,
                result.p95Nanos() / NANOS_PER_MILLISECOND, P95_BUDGET_MILLIS,
                result.maxNanos() / NANOS_PER_MILLISECOND,
                Runtime.getRuntime().maxMemory() / (1024 * 1024),
                progressed && totalWithinBudget && p95WithinBudget ? "PASS" : "FAIL");

        assertAll(
                () -> assertEquals(OPERATION_COUNT, result.completedCycles(),
                        "모든 반복에서 실제 블록 고정이 진행되어야 합니다."),
                () -> assertTrue(totalWithinBudget,
                        "전체 처리 시간이 " + TOTAL_BUDGET_MILLIS + "ms 미만이어야 합니다."),
                () -> assertTrue(p95WithinBudget,
                        "반복 처리 시간의 95백분위가 " + P95_BUDGET_MILLIS + "ms 이하여야 합니다."));
    }

    private static Stream<Arguments> gameConfigurations() {
        return Arrays.stream(GameMode.values())
                .flatMap(mode -> Arrays.stream(Difficulty.values())
                        .map(difficulty -> Arguments.of(mode, difficulty)));
    }

    /** 한 반복은 tick, 좌측 이동, 회전, 하드 드롭, 후속 고정 처리와 스냅샷 생성을 포함한다. */
    private PerformanceResult measureOperations(
            int count, GameMode mode, Difficulty difficulty) {
        Random random = new Random(RANDOM_SEED);
        Game game = newGame(mode, difficulty, random);
        long[] samples = new long[count];
        int completedCycles = 0;
        long startedAt = System.nanoTime();

        for (int index = 0; index < count; index++) {
            long cycleStartedAt = System.nanoTime();
            if (game.isGameOver()) {
                game = newGame(mode, difficulty, random);
            }
            long previousLockedCount = game.getLockedPieceCount();

            game.tick();
            game.handleCommand(GameCommand.MOVE_LEFT);
            game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
            game.handleCommand(GameCommand.HARD_DROP);
            finishPendingLock(game, previousLockedCount);

            GameSnapshot snapshot = game.createSnapshot();
            if (game.getLockedPieceCount() > previousLockedCount && snapshot.getScore() > 0) {
                completedCycles++;
            }
            samples[index] = System.nanoTime() - cycleStartedAt;
        }

        long elapsedNanos = System.nanoTime() - startedAt;
        Arrays.sort(samples);
        int p95Index = (int) Math.ceil(count * P95_RATIO) - 1;
        return new PerformanceResult(
                elapsedNanos, samples[p95Index], samples[count - 1], completedCycles);
    }

    /** 줄 삭제 대기나 무게추 드릴이 끝나도록 보드 높이만큼까지 진행한다. */
    private void finishPendingLock(Game game, long previousLockedCount) {
        for (int step = 0; step <= BOARD_HEIGHT; step++) {
            if (game.createSnapshot().getStatus() == GameStatus.CLEARING) {
                game.finishLineClear();
            }
            if (game.getLockedPieceCount() > previousLockedCount || game.isGameOver()) {
                return;
            }
            game.tick();
        }
        throw new AssertionError("블록 고정이 정해진 단계 안에 완료되지 않았습니다.");
    }

    private Game newGame(GameMode mode, Difficulty difficulty, Random random) {
        Game game = new Game(
                new Board(BOARD_WIDTH, BOARD_HEIGHT),
                new TetrominoGenerator(random, difficulty.createWeights()),
                new ItemGenerator(random), difficulty, mode);
        game.start();
        return game;
    }

    private record PerformanceResult(
            long elapsedNanos, long p95Nanos, long maxNanos, int completedCycles) {
    }
    // AI-assisted code end
}
