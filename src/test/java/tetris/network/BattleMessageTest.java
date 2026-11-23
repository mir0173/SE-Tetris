package tetris.network;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tetris.battle.MatchResult;
import tetris.model.Board;
import tetris.model.GameSnapshot;
import tetris.model.GameStatus;
import tetris.model.Item;
import tetris.model.ItemType;
import tetris.model.PieceType;
import tetris.model.Position;

// AI-assisted code start
class BattleMessageTest {
    @Test
    void roundTripBoardGhostItemsAndPendingRows() throws Exception {
        GameSnapshot expected = snapshot(321);
        BattleMessage payload = BattleMessage.state(1, BattleStateData.fromSnapshot(expected));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessageCodec.write(new DataOutputStream(bytes), LobbyMessage.battle(1, 7, payload));
        LobbyMessage decoded = MessageCodec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        GameSnapshot actual = decoded.battle().state().toSnapshot();
        assertEquals(7, decoded.matchId());
        assertArrayEquals(expected.getCells()[19], actual.getCells()[19]);
        assertEquals(expected.getGhostPieceCells(), actual.getGhostPieceCells());
        assertEquals(expected.getNextPieces(), actual.getNextPieces());
        assertEquals(expected.getHeldPiece(), actual.getHeldPiece());
        assertEquals(expected.getNextPieceItem(), actual.getNextPieceItem());
        assertEquals(expected.getHeldPieceItem(), actual.getHeldPieceItem());
        assertEquals(expected.getClearingRows(), actual.getClearingRows());
        assertArrayEquals(expected.getPendingGarbageRows().get(0), actual.getPendingGarbageRows().get(0));
        assertEquals(expected.getScore(), actual.getScore());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getDropInterval(), actual.getDropInterval());
        System.out.println("[T20 상태 변환] 보드·점수·고스트·다음/홀드 아이템·삭제 줄·공격 대기 보존 PASS");
    }

    @Test
    void isolateArraysAtConstructionAndAccess() {
        BattleStateData state = BattleStateData.fromSnapshot(snapshot(1));
        state.cells()[19][0] = 0;
        state.pendingRows().get(0)[0] = 0;
        assertEquals(Board.GARBAGE_CELL, state.cells()[19][0]);
        assertEquals(Board.GARBAGE_CELL, state.pendingRows().get(0)[0]);
    }

    @Test
    void preserveAttackHolesAndRowOrderWithoutAliasing() {
        int[] first = {99, 0, 99, 99};
        int[] second = {0, 99, 0, 99};
        BattleMessage message = BattleMessage.attack(1, List.of(first, second));
        first[0] = 0;
        assertArrayEquals(new int[]{99, 0, 99, 99}, message.attackRows().get(0));
        assertArrayEquals(second, message.attackRows().get(1));
        message.attackRows().get(1)[0] = 99;
        assertEquals(0, message.attackRows().get(1)[0]);
        System.out.println("[T20 공격 전송 형식] 구멍 위치·위아래 순서·방어적 복사 PASS (공격 규칙 판정 아님)");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 31})
    void rejectInvalidAttackWidth(int width) {
        assertThrows(IllegalArgumentException.class,
                () -> new BattleMessage(BattleMessage.Type.ATTACK, 1, null, width, List.of(1), null, null));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1_024})
    void rejectBitsOutsideBoard(int mask) {
        assertThrows(IllegalArgumentException.class,
                () -> new BattleMessage(BattleMessage.Type.ATTACK, 1, null, 10, List.of(mask), null, null));
    }

    @Test
    void roundTripResultAndForfeitWithMatchId() throws Exception {
        MatchResult outcome = new MatchResult(MatchResult.Winner.PLAYER_TWO,
                MatchResult.EndReason.STOPPED, 32, 64);
        for (BattleMessage payload : List.of(BattleMessage.result(1, outcome),
                BattleMessage.control(BattleMessage.Type.FORFEIT, 2),
                BattleMessage.control(BattleMessage.Type.RESULT_ACK, 3),
                BattleMessage.timeLeft(4, 0))) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            LobbyMessage original = LobbyMessage.battle(1, 7, payload);
            MessageCodec.write(new DataOutputStream(bytes), original);
            assertEquals(original, MessageCodec.read(
                    new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
        }
    }

    @Test
    void rejectMissingResultAndOutOfRangeTime() {
        assertThrows(IllegalArgumentException.class, () -> BattleMessage.result(1, null));
        assertThrows(IllegalArgumentException.class, () -> BattleMessage.timeLeft(1, -1));
    }

    @Test
    void rejectUnknownCellBeforeRendering() {
        int[][] cells = new int[20][10];
        cells[0][0] = 98;
        assertThrows(IllegalArgumentException.class, () -> new BattleStateData(cells, List.of(), null, 0,
                GameStatus.PLAYING, 1_000, null, null, List.of(), List.of(), List.of()));
    }

    static GameSnapshot snapshot(long score) {
        int[][] cells = new int[20][10];
        cells[19][0] = Board.GARBAGE_CELL;
        cells[19][2] = PieceType.I.getCellId();
        return new GameSnapshot(cells, List.of(PieceType.I), PieceType.T, score, GameStatus.PLAYING,
                800, new Item(ItemType.SLOW, Item.NOT_EMBEDDED), new Item(ItemType.LINE_CLEAR, 0),
                List.of(19), Map.of(new Position(17, 4), PieceType.I.getCellId()), List.of(new int[]{99, 0, 99, 99, 99, 99, 99, 99, 99, 99}));
    }
}
// AI-assisted code end
