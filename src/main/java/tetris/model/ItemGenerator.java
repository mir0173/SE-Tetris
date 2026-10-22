package tetris.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * 모든 아이템을 생성하여 반환하는 클래스
 * 아이템 또는 아이템이 포함된 블록의 모양은 정하지 않는다.
 */
public class ItemGenerator {

    // 추첨에 사용할 랜덤 객체
    private final Random random;

    // 종류별 가중치 (enum 선언 순서로 순회되도록)
    private final Map<ItemType, Integer> weights;

    // 가중치 합 (매번 다시 계산하지 않도록 저장)
    private final int totalWeight;

    /**
     * 모든 아이템을 같은 확률로 생성한다.
     * 
     * @param random 추첨에 사용할 랜덤 객체
     * @throws NullPointerException random이 null인 경우
     */
    public ItemGenerator(Random random) {
        this(random, createDefaultWeights());
    }

    /**
     * 종류별 가중치에 비례한 확률로 생성한다.
     * 
     * @param random  추첨에 사용할 랜덤 객체
     * @param weights 종류별 가중치 (5종 모두 포함, 0 이상, 합은 1 이상)
     * @throws NullPointerException     random 또는 weights가 null인 경우
     * @throws IllegalArgumentException 빠진 종류가 있거나, 음수가 있거나, 합이 0인 경우
     */
    public ItemGenerator(Random random, Map<ItemType, Integer> weights) {
        if (random == null || weights == null) {
            throw new NullPointerException("Parameter is null : " + (random == null ? "random" : "weights"));
        }
        Map<ItemType, Integer> copiedWeights = new EnumMap<>(weights);
        this.totalWeight = validateAndSum(copiedWeights);
        this.random = random;
        this.weights = copiedWeights;
    }

    /**
     * 가중치로 아이템 종류를 고르고, Embbedded 아이템이라면 무작위 칸을 정해 반환한다.
     * 
     * @return record형 Item
     */
    public Item generateNext() {
        ItemType type = selectItemType();
        int cellIndex = selectCellIndex(type);
        return new Item(type, cellIndex);
    }

    // 가중치에 따라 다음 아이템 종류를 하나 뽑는다. 이전 결과와는 독립적이다.
    private ItemType selectItemType() {
        int remaining = random.nextInt(totalWeight);
        for (Map.Entry<ItemType, Integer> entry : weights.entrySet()) {
            remaining -= entry.getValue();
            if (remaining < 0) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("unreachable");
    }

    // 아이템 종류를 받고 Embedded 아이템이라면 0 ~ 3 무작위 칸, 아니면 -1 반환
    private int selectCellIndex(ItemType type) {
        if (type.isEmbedded()) {
            return random.nextInt(Item.MAX_CELL_INDEX + 1);
        } else {
            return Item.NOT_EMBEDDED;
        }
    }

    // ItemType에서 정한 가중치로 가중치 맵을 생성한다.
    private static Map<ItemType, Integer> createDefaultWeights() {
        Map<ItemType, Integer> weightMap = new EnumMap<>(ItemType.class);
        for (ItemType type : ItemType.values()) {
            weightMap.put(type, type.getWeight());
        }
        return weightMap;
    }

    // 5종이 모두 있고 음수가 없는지 확인한 뒤 합을 반환한다.
    private static int validateAndSum(Map<ItemType, Integer> weights) {
        int sum = 0;
        for (ItemType type : ItemType.values()) {
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
