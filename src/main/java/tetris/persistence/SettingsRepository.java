package tetris.persistence;

import java.io.IOException;
import tetris.settings.GameSettings;

public interface SettingsRepository {

    /**
     * 설정을 불러온다.
     * 파일이 없으면 기본 설정을 반환한다.
     *
     * @throws IOException 
     */
    GameSettings load() throws IOException;

    /**
     * 전달받은 설정을 저장한다.
     *
     * @throws IOException 
     */
    void save(GameSettings settings) throws IOException;
}

// 테스크 맡은 사람이 구현