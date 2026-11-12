package tetris.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

final class MessageCodec {
    static final int MAX_MESSAGE_BYTES = 16_384;
    private static final Gson GSON = new Gson();

    private MessageCodec() {
    }

    static void write(DataOutputStream output, LobbyMessage message) throws IOException {
        byte[] bytes = GSON.toJson(message).getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > MAX_MESSAGE_BYTES) {
            throw new IOException("잘못된 메시지 길이입니다.");
        }
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    static LobbyMessage read(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length <= 0 || length > MAX_MESSAGE_BYTES) {
            throw new IOException("잘못된 메시지 길이입니다.");
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        try {
            LobbyMessage message = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), LobbyMessage.class);
            if (message == null) {
                throw new IllegalArgumentException("빈 메시지입니다.");
            }
            return message;
        } catch (RuntimeException exception) {
            throw new IOException("잘못된 메시지입니다.", exception);
        }
    }
}
