package tetris.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 7-bag 방식으로 다음 블록 종류를 생성한다.
 * 7종을 한 번씩 담은 가방을 섞어 하나씩 꺼내고, 가방이 비면 다시 채운다.
 * 따라서 7개를 연속으로 꺼낼 때마다 7종이 정확히 한 번씩 나온다.
 */
public class TetrominoGenerator {

    private final Random random;

    // 아직 꺼내지 않은 블록 종류
    private final List<TetrominoType> bag;

    /**
     * random을 주입받아 테스트에서 시드로 블록 순서를 고정할 수 있게 한다.
     * 
     * @param random 가방을 섞는데 활용할 랜덤 객체
     * @throws NullPointerException Random이 null인 경우
     */ 
    public TetrominoGenerator(Random random) {
        if (random == null) {
            throw new NullPointerException("random is null");
        }
        this.random = random;
        this.bag = new ArrayList<>();
    }

    /**
     * 가방에서 다음 블록 종류를 꺼내 반환한다.
     * 가방이 비어 있으면 7종을 새로 채워 섞은 뒤 꺼낸다.
     * 
     * @return 다음 블록 종류
     */
    public TetrominoType generateNext() {
        if (bag.isEmpty()) {
            refillBag();
        }
        return bag.removeLast();
    }

    // 7종을 한 번씩 가방에 넣고 섞는다
    private void refillBag() {
        List<TetrominoType> blocks = List.of(TetrominoType.values());
        bag.addAll(blocks);
        Collections.shuffle(bag, random);
    }
}
