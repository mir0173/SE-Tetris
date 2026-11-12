package tetris.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tetris.model.Difficulty;

// AI-assisted code start
class MessageCodecTest {
    @Test
    void preserveConsecutiveMessageBoundaries() throws Exception {
        LobbyMessage first = LobbyMessage.hello(PeerRole.HOST);
        LobbyMessage second = LobbyMessage.config(3,
                new LobbySettings(LobbyMode.TIME_LIMIT, Difficulty.HARD, 120));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(buffer);
        MessageCodec.write(output, first);
        MessageCodec.write(output, second);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        assertEquals(first, MessageCodec.read(input));
        assertEquals(second, MessageCodec.read(input));
        System.out.println("[P2P 메시지] 연속 메시지 2개: 경계·역할·모드·난이도·제한 시간 보존 PASS");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 16_385, Integer.MAX_VALUE})
    void rejectInvalidLengthBeforeAllocating(int length) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new DataOutputStream(buffer).writeInt(length);
        assertThrows(IOException.class, () -> MessageCodec.read(
                new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null", "{", "{}", "{\"type\":\"UNKNOWN\",\"version\":1}",
            "{\"type\":\"HELLO\",\"version\":2,\"role\":\"CLIENT\"}",
            "{\"type\":\"CONFIG\",\"version\":1,\"revision\":1}",
            "{\"type\":\"CONFIG\",\"version\":1,\"revision\":1,\"settings\":{\"mode\":\"TIME_LIMIT\",\"difficulty\":\"NORMAL\",\"timeLimitSeconds\":0}}"
    })
    void rejectInvalidJsonOrProtocol(String json) throws Exception {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(buffer);
        output.writeInt(payload.length);
        output.write(payload);
        assertThrows(IOException.class, () -> MessageCodec.read(
                new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()))));
    }

    @Test
    void rejectTruncatedPayload() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(buffer);
        output.writeInt(100);
        output.writeByte(1);
        assertThrows(EOFException.class, () -> MessageCodec.read(
                new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()))));
    }
}
// AI-assisted code end
