package tetris.persistence;

import java.io.IOException;
import java.util.List;
import tetris.scoreboard.ScoreEntry;

public interface ScoreRepository {

    /**
     * 저장된 기록을 불러온다.
     * 파일이 없으면 빈 목록을 반환한다.
     *
     * @throws IOException 
     */
    List<ScoreEntry> load() throws IOException;

    /**
     * 전달받은대로 기록을 교체한다.
     *
     * @throws IOException 
     */
    void save(List<ScoreEntry> entries) throws IOException;
}

// 테스크 맡은 사람이 구현