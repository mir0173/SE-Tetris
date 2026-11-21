package tetris.view;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tetris.model.GameSnapshot;
import tetris.model.GameCommand;
import tetris.network.PeerHealth;
import tetris.settings.GameSettings;

public final class JavaFxP2pGameView {
    private static final double SPACING = 12;
    private static final double PADDING = 16;
    private final JavaFxGameView localView;
    private final JavaFxGameView remoteView;
    private final Label status = new Label("상대 상태 정보를 기다립니다.");
    private final Label health = new Label();
    private final Scene scene;

    public JavaFxP2pGameView(GameSettings settings, Runnable onReturnToLobby) {
        localView = JavaFxGameView.createEmbedded(settings);
        remoteView = JavaFxGameView.createEmbedded(settings);
        localView.setKeyHelpText(settings.getKeyBindings().get(GameCommand.HOLD) + ": 홀드\n"
                + settings.getKeyBindings().get(GameCommand.QUIT_GAME) + ": 대기실");
        remoteView.setKeyHelpText("상대 화면");
        localView.getContent().setId("p2p-local-panel");
        remoteView.getContent().setId("p2p-remote-panel");
        remoteView.getContent().setFocusTraversable(false);
        remoteView.getContent().setMouseTransparent(true);
        localView.getContent().setOnMousePressed(event -> localView.getContent().requestFocus());
        Button back = new Button("대기실로");
        back.setId("p2p-battle-back");
        back.setOnAction(event -> onReturnToLobby.run());
        status.setId("p2p-battle-status");
        status.setWrapText(true);
        health.setId("p2p-battle-health");
        Label scope = new Label("통신 확인 단계: 공격 적용, 승패, 시간제한 판정은 아직 연결되지 않았습니다.");
        scope.setWrapText(true);
        HBox boards = new HBox(SPACING,
                new VBox(SPACING, new Label("나"), localView.getContent()),
                new VBox(SPACING, new Label("상대"), remoteView.getContent()));
        ScrollPane scroll = new ScrollPane(boards);
        scroll.setPannable(false);
        scroll.setFocusTraversable(false);
        VBox root = new VBox(SPACING, scope, status, health, scroll, back);
        root.setPadding(new Insets(PADDING));
        scene = new Scene(root, 980 * settings.getWindowSize().getScale(),
                680 * settings.getWindowSize().getScale());
    }

    public GameView getLocalView() { return localView; }
    public Scene getScene() { return scene; }
    public void setStatus(String text) { status.setText(text); }


    public void renderConnectionHealth(PeerHealth result) {
        health.setText(result == null ? "" : result.displayText());
    }

    public void renderRemote(GameSnapshot snapshot) {
        remoteView.render(snapshot);
        status.setText("상대 점수:" + snapshot.getScore() + "점, 상태: " + snapshot.getStatus());
    }
}
