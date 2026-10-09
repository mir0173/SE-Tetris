package tetris.scoreboard;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;

public class Scoreboard {

    private static final int MAX_ENTRIES = 10;
    private static final int RANK_NOT_FOUND = -1;
    private static final int FIRST_RANK = 1;

    private final List<ScoreEntry> entries;

    // AI-assisted code start
    /**
     * 점수 기록 목록으로 스코어보드를 만든다.
     * 점수가 높은 순으로 정렬하고 상위 10개의 점수만 유지한다.
     * 점수가 같으면 입력 목록에서 앞에 있던 기록이 위에 온다.
     * 
     * @param entries 점수 기록 목록 (순서는 기록한 순서)
     */
    public Scoreboard(List<ScoreEntry> entries) {
        List<ScoreEntry> sortedEntries = new ArrayList<>(entries);
        sortedEntries.sort(Comparator.comparingLong(ScoreEntry::getScore).reversed());

        int size = Math.min(sortedEntries.size(), MAX_ENTRIES);
        this.entries = List.copyOf(sortedEntries.subList(0, size));
    }
    // AI-assisted code end

    public List<ScoreEntry> getEntries() {
        return entries;
    }

    // AI-assisted code start
    /**
     * 주어진 점수가 스코어보드에 포함될 수 있는지 확인한다.
     * 
     * @param score 확인할 점수
     * @return 점수가 스코어보드에 포함될 수 있으면 true, 그렇지 않으면 false
     */
    public boolean isRankIn(long score) {
        if (entries.size() < MAX_ENTRIES) {
            return true;
        }

        ScoreEntry lowest = entries.get(entries.size() - 1);
        return score > lowest.getScore();
    }
    // AI-assisted code end

    /**
     * 기록을 추가한 새 스코어보드를 반환한다.
     * 원본 스코어보드는 변경되지 않는다.
     * 점수가 낮으면 스코어보드에 포함되지 않는다.
     * 점수가 같으면 기존 기록이 우선한다.
     * 
     * @param entry 추가할 점수 기록
     * @return 기록이 추가된 새로운 스코어보드
     */
    public Scoreboard addEntry(ScoreEntry entry) {
        List<ScoreEntry> newEntries = new ArrayList<>(entries);
        newEntries.add(entry);
        return new Scoreboard(newEntries);
    }

    // AI-assisted code start
    /**
     * 주어진 점수 기록의 순위를 반환한다. 순위는 1등부터 센다.
     * 기록이 스코어보드에 없으면 -1을 반환한다.
     * 스코어보드에서 방금 기록한 항목을 강조할 때 사용한다.
     * 
     * @param entryId 순위를 확인할 점수 기록의 ID
     * @return 점수 기록의 순위, 없으면 -1
     */
    public int getRank(UUID entryId) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId().equals(entryId)) {
                return i + FIRST_RANK;
            }
        }

        return RANK_NOT_FOUND;
    }
    // AI-assisted code end

}
