package tetris.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * 가중치 기반 독립 추첨으로 다음 블록 종류를 생성한다.
 * 매번 이전 결과와 상관없이, 각 종류가 (자기 가중치 / 가중치 합)의 확률로 나온다.
 * 기본 생성자는 모든 가중치를 같게 두어 7종이 동일한 확률로 나온다.
 */
public class TetrominoGenerator {

    // 기본 가중치
    private static final int BASIC_WEIGHT = 10;

    // 추첨에 사용할 랜덤 객체
    private final Random random;

    // 종류별 가중치 (enum 선언 순서로 순회되도록)
    private final Map<TetrominoType, Integer> weights;

    // 가중치 합 (매번 다시 계산하지 않도록 저장)
    private final int totalWeight;

    /**
     * 모든 블록 종류를 같은 확률로 생성한다.
     * 
     * @param random 추첨에 사용할 랜덤 객체
     * @throws NullPointerException random이 null인 경우
     */
    public TetrominoGenerator(Random random) {
        this(random, equalWeights());
    }

    /**
     * 종류별 가중치에 비례한 확률로 생성한다.
     * 
     * @param random  추첨에 사용할 랜덤 객체
     * @param weights 종류별 가중치 (7종 모두 포함, 0 이상, 합은 1 이상)
     * @throws NullPointerException     random 또는 weights가 null인 경우
     * @throws IllegalArgumentException 빠진 종류가 있거나, 음수가 있거나, 합이 0인 경우
     */
    public TetrominoGenerator(Random random, Map<TetrominoType, Integer> weights) {
        if (random == null || weights == null) {
            throw new NullPointerException("Parameter is null : " + (random == null ? "random" : "weights"));
        }
        Map<TetrominoType, Integer> copiedWeights = new EnumMap<>(weights);
        this.totalWeight = validateAndSum(copiedWeights);
        this.random = random;
        this.weights = copiedWeights;
    }

    /**
     * 가중치에 따라 다음 블록 종류를 하나 뽑는다. 이전 결과와는 독립적이다.
     * 
     * @return 다음 블록 종류
     */
    public TetrominoType generateNext() {
        int r = random.nextInt(totalWeight);
        for (Map.Entry<TetrominoType, Integer> e : weights.entrySet()) {
            r -= e.getValue();
            if (r < 0) {
                return e.getKey();
            }
        }
        throw new IllegalStateException("unreachable");
    }

    // 7종 모두 같은 가중치를 담은 맵을 만든다.
    private static Map<TetrominoType, Integer> equalWeights() {
        Map<TetrominoType, Integer> weightMap = new EnumMap<>(TetrominoType.class);
        for (TetrominoType type : TetrominoType.values()) {
            weightMap.put(type, BASIC_WEIGHT);
        }
        return weightMap;
    }

    // 7종이 모두 있고 음수가 없는지 확인한 뒤 합을 반환한다.
    private static int validateAndSum(Map<TetrominoType, Integer> weights) {
        int sum = 0;
        for (TetrominoType type : TetrominoType.values()) {
            Integer weight = weights.get(type);
            if (weight == null || weight < 0) {
                throw new IllegalArgumentException("유효하지 않은 weight " + type + " : " + weight);
            }
            sum += weight;
        }
        if (sum <= 0) {
            throw new IllegalArgumentException("가중치 합이 0 이하");
        }
        return sum;
    }
}
