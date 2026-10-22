package tetris.scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ScoreboardGroup {
    private final Map<ScoreCategory, Scoreboard> scoreboards;

    // AI-assisted code start
    public ScoreboardGroup(List<ScoreEntry> entries) {
        Map<ScoreCategory, List<ScoreEntry>> categorizedEntries = new HashMap<>();

        for (ScoreCategory category : ScoreCategory.getAllCategories()) {
            categorizedEntries.put(category, new ArrayList<>());
        }
        for (ScoreEntry entry : entries) {
            categorizedEntries.get(entry.getCategory()).add(entry);
        }
        
        Map<ScoreCategory, Scoreboard> createdScoreboards = new HashMap<>();
        for (ScoreCategory category : ScoreCategory.getAllCategories()) {
            createdScoreboards.put(category, new Scoreboard(categorizedEntries.get(category)));
        }
        this.scoreboards = Map.copyOf(createdScoreboards);
    }

    public Scoreboard getScoreboard(ScoreCategory category) {
        return scoreboards.get(category);
    }

    /**
     * 주어진 점수가 그 조합의 스코어보드에 들어갈 수 있는지 확인한다.
     * 다른 조합의 기록은 영향을 주지 않는다.
     *
     * @param category 확인할 조합
     * @param score 확인할 점수
     * @return 순위권이면 true
     */
    public boolean isRankIn(ScoreCategory category, long score) {
        return scoreboards.get(category).isRankIn(score);
    }

    /**
     * 모든 조합의 기록을 하나의 목록으로 합친다. 저장할 때 사용한다.
     * 조합 순서대로, 각 조합 안에서는 순위대로 나온다.
     * 이 목록으로 다시 만들면 같은 순위(동점 순서 포함)가 복원된다.
     *
     * @return 모든 조합의 기록 (수정 불가)
     */
    public List<ScoreEntry> getAllEntries() {
        List<ScoreEntry> allEntries = new ArrayList<>();
        for (ScoreCategory category : ScoreCategory.getAllCategories()) {
            allEntries.addAll(scoreboards.get(category).getEntries());
        }
        return List.copyOf(allEntries);
    }

    /**
     * 기록을 추가한 새 그룹을 반환한다. 원본은 변경되지 않는다.
     * 기록은 자기 조합의 스코어보드에 들어가며, 같은 점수면 기존 기록이 위에 온다.
     *
     * @param entry 추가할 점수 기록
     * @return 기록이 추가된 새 ScoreboardGroup
     */
    public ScoreboardGroup addEntry(ScoreEntry entry) {
        List<ScoreEntry> allEntries = new ArrayList<>(getAllEntries());
        allEntries.add(entry);
        return new ScoreboardGroup(allEntries);
    }
    // AI-assisted code end
}
