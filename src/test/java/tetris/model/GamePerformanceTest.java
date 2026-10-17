package tetris.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

// AI-assisted code start
class GamePerformanceTest {
    private static final int WARMUP_COUNT = 1_000;
    private static final int OPERATION_COUNT = 10_000;
    private static final long BUDGET_MILLIS = 5_000;

    @Test
    void processCommandsAndSnapshotsWithinTimeBudget() {
        runOperations(WARMUP_COUNT);
        long startedAt = System.nanoTime();
        long scoreSum = runOperations(OPERATION_COUNT);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        System.out.printf("T10 performance: operations=%d, elapsed=%dms, maxHeap=%dMiB%n",
                OPERATION_COUNT, elapsedMillis,
                Runtime.getRuntime().maxMemory() / (1024 * 1024));
        assertTrue(scoreSum > 0);
        assertTrue(elapsedMillis < BUDGET_MILLIS,
                "10,000회 명령·스냅샷 처리에 " + elapsedMillis + "ms가 걸렸습니다.");
    }

    private long runOperations(int count) {
        Game game = newGame();
        long scoreSum = 0;
        for (int index = 0; index < count; index++) {
            if (game.isGameOver()) { game = newGame(); }
            game.tick();
            game.handleCommand(GameCommand.MOVE_LEFT);
            game.handleCommand(GameCommand.ROTATE_CLOCKWISE);
            game.handleCommand(GameCommand.HARD_DROP);
            GameSnapshot snapshot = game.createSnapshot();
            scoreSum += snapshot.getScore();
        }
        return scoreSum;
    }

    private Game newGame() {
        Game game = new Game(new Board(10, 20), new TetrominoGenerator(new Random(42L)), Difficulty.NORMAL);
        game.start();
        return game;
    }
}
// AI-assisted code end
