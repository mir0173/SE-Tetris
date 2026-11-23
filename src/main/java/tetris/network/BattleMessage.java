package tetris.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import tetris.battle.BattleSettings;
import tetris.battle.MatchResult;
import tetris.model.Board;

public record BattleMessage(Type type, long sequence, BattleStateData state,
        int width, List<Integer> rowMasks, MatchResult result, Integer remainingSeconds) {
    public enum Type { STATE, ATTACK, FORFEIT, RESULT, RESULT_ACK, TIME_LEFT }

    public BattleMessage {
        Objects.requireNonNull(type, "type");
        if (sequence <= 0) {
            throw new IllegalArgumentException("메시지 순서 번호가 필요합니다.");
        }
        rowMasks = List.copyOf(rowMasks);
        if ((type == Type.STATE) != (state != null)
                || (type != Type.ATTACK && (width != 0 || !rowMasks.isEmpty()))
                || (type == Type.RESULT) != (result != null)
                || (type == Type.TIME_LEFT) != (remainingSeconds != null)) {
            throw new IllegalArgumentException("경기 메시지의 유형과 내용이 일치하지 않습니다.");
        }
        if (result != null && (result.firstScore() < 0 || result.secondScore() < 0)) {
            throw new IllegalArgumentException("경기 점수는 0 이상이어야 합니다.");
        }
        if (remainingSeconds != null
                && (remainingSeconds < 0 || remainingSeconds > BattleSettings.MAX_TIME_LIMIT_SECONDS)) {
            throw new IllegalArgumentException("남은 경기 시간이 범위를 벗어났습니다.");
        }
        if (type == Type.ATTACK) {
            if (width <= 0 || width > 30 || rowMasks.isEmpty() || rowMasks.size() > 40) {
                throw new IllegalArgumentException("공격 데이터 크기가 잘못되었습니다.");
            }
            int allowedMask = (1 << width) - 1;
            for (int mask : rowMasks) {
                if (mask < 0 || (mask & ~allowedMask) != 0) {
                    throw new IllegalArgumentException("공격 줄 비트마스크가 잘못되었습니다.");
                }
            }
        }
    }

    static BattleMessage state(long sequence, BattleStateData state) {
        return new BattleMessage(Type.STATE, sequence, state, 0, List.of(), null, null);
    }

    static BattleMessage attack(long sequence, List<int[]> rows) {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("빈 공격은 전송하지 않습니다.");
        }
        int width = rows.get(0).length;
        List<Integer> masks = new ArrayList<>();
        for (int[] row : rows) {
            if (row.length != width || width > 30) {
                throw new IllegalArgumentException("공격 줄 길이가 다릅니다.");
            }
            int mask = 0;
            for (int column = 0; column < width; column++) {
                if (row[column] == Board.GARBAGE_CELL) {
                    mask |= 1 << column;
                } else if (row[column] != Board.EMPTY_CELL) {
                    throw new IllegalArgumentException("빈칸과 공격 셀만 사용할 수 있습니다.");
                }
            }
            masks.add(mask);
        }
        return new BattleMessage(Type.ATTACK, sequence, null, width, masks, null, null);
    }

    static BattleMessage control(Type type, long sequence) {
        return new BattleMessage(type, sequence, null, 0, List.of(), null, null);
    }

    static BattleMessage result(long sequence, MatchResult result) {
        return new BattleMessage(Type.RESULT, sequence, null, 0, List.of(), result, null);
    }

    static BattleMessage timeLeft(long sequence, int seconds) {
        return new BattleMessage(Type.TIME_LEFT, sequence, null, 0, List.of(), null, seconds);
    }

    public List<int[]> attackRows() {
        List<int[]> rows = new ArrayList<>();
        for (int mask : rowMasks) {
            int[] row = new int[width];
            for (int column = 0; column < width; column++) {
                row[column] = (mask & (1 << column)) == 0 ? Board.EMPTY_CELL : Board.GARBAGE_CELL;
            }
            rows.add(row);
        }
        return List.copyOf(rows);
    }
}
