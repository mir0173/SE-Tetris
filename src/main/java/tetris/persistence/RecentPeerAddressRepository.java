package tetris.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

// AI-assisted code start
/** 마지막으로 접속에 성공한 서버 IPv4를 로컬 파일에 저장한다. */
public final class RecentPeerAddressRepository {
    private final Path path;

    public RecentPeerAddressRepository(Path path) {
        this.path = path;
    }

    public Optional<String> load() throws IOException {
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        String address = Files.readString(path).strip();
        return isIpv4(address) ? Optional.of(address) : Optional.empty();
    }

    public void save(String address) throws IOException {
        if (!isIpv4(address)) {
            throw new IllegalArgumentException("저장할 IPv4 주소가 잘못되었습니다.");
        }
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, address + System.lineSeparator());
    }

    private static boolean isIpv4(String address) {
        return address.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")
                && Arrays.stream(address.split("\\.")).allMatch(part -> Integer.parseInt(part) <= 255);
    }
}
// AI-assisted code end
