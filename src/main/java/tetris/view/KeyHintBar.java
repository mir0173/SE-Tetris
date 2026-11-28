package tetris.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

// AI-assisted code start
/**
 * 화면 맨 아래에 "지금 쓸 수 있는 키"를 키캡 모양으로 보여 주는 안내 줄.
 * 1차 요구사항 "다른 키를 사용하는 경우 화면에 사용 가능한 키를 표시"를 모든 화면에서 지키기 위한 부품이다.
 * 사용 예: {@code new KeyHintBar().add("이동", "UP", "DOWN").add("선택", "ENTER")}
 */
public final class KeyHintBar extends FlowPane {

    public static final String ID = "key-hint-bar";
    public static final String STYLE_CLASS = "key-hint-bar";
    public static final String HINT_STYLE_CLASS = "key-hint";
    public static final String KEYCAP_STYLE_CLASS = "keycap";
    public static final String TEXT_STYLE_CLASS = "key-hint-text";

    private static final double HINT_GAP = 14;
    private static final double ROW_GAP = 4;
    private static final double KEY_GAP = 3;

    public KeyHintBar() {
        super(HINT_GAP, ROW_GAP);
        setId(ID);
        getStyleClass().add(STYLE_CLASS);
        setAlignment(Pos.CENTER);
    }

    /**
     * 안내 한 개를 덧붙인다.
     *
     * @param description 키를 눌렀을 때 하는 일 (예: "이동")
     * @param keyNames    JavaFX KeyCode 이름들 (예: "UP", "DOWN"). 화면에는 {@link KeyNames}로 바꿔 보인다
     * @return 이어서 부를 수 있도록 자기 자신
     */
    public KeyHintBar add(String description, String... keyNames) {
        HBox hint = new HBox(KEY_GAP);
        hint.getStyleClass().add(HINT_STYLE_CLASS);
        hint.setAlignment(Pos.CENTER_LEFT);
        for (String keyName : keyNames) {
            Label keycap = new Label(KeyNames.display(keyName));
            keycap.getStyleClass().add(KEYCAP_STYLE_CLASS);
            hint.getChildren().add(keycap);
        }
        Label text = new Label(description);
        text.getStyleClass().add(TEXT_STYLE_CLASS);
        hint.getChildren().add(text);
        getChildren().add(hint);
        return this;
    }

    /**
     * 보이던 안내를 모두 지운다. 화면 상태가 바뀌어 쓸 수 있는 키가 달라질 때 쓴다.
     *
     * @return 이어서 부를 수 있도록 자기 자신
     */
    public KeyHintBar clear() {
        getChildren().clear();
        return this;
    }
}
// AI-assisted code end
