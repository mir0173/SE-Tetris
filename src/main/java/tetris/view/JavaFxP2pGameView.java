package tetris.view;

import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tetris.battle.MatchResult;
import tetris.model.GameSnapshot;
import tetris.model.GameCommand;
import tetris.network.PeerHealth;
import tetris.settings.GameSettings;

public final class JavaFxP2pGameView {
    private static final double SPACING = 12;
    private static final double PADDING = 16;
    private final JavaFxGameView localView;
    private final JavaFxGameView remoteView;
    private final Label status = new Label();
    private final Label remoteStatus = new Label("상대 상태 정보를 기다립니다.");
    private final Label resultLabel = new Label();
    private final Label timeLeft = new Label();
    private final Label health = new Label();
    private final Button back = new Button("기권하고 종료");
    private final Scene scene;

    public JavaFxP2pGameView(GameSettings settings, Runnable onReturnToLobby) {
        localView = JavaFxGameView.createEmbedded(settings);
        remoteView = JavaFxGameView.createEmbedded(settings);
        localView.setKeyHelpText(settings.getKeyBindings().get(GameCommand.HOLD) + ": 홀드\n"
                + settings.getKeyBindings().get(GameCommand.QUIT_GAME) + ": 기권 / 대기실");
        remoteView.setKeyHelpText("상대 화면");
        localView.getContent().setId("p2p-local-panel");
        remoteView.getContent().setId("p2p-remote-panel");
        remoteView.getContent().setFocusTraversable(false);
        remoteView.getContent().setMouseTransparent(true);
        localView.getContent().setOnMousePressed(event -> localView.getContent().requestFocus());
        back.setId("p2p-battle-back");
        back.setOnAction(event -> onReturnToLobby.run());
        status.setId("p2p-battle-status");
        status.setWrapText(true);
        remoteStatus.setWrapText(true);
        resultLabel.setWrapText(true);
        resultLabel.setId("p2p-battle-result");
        timeLeft.setId("p2p-battle-time-left");
        health.setId("p2p-battle-health");
        for (Label label : new Label[]{status, resultLabel, timeLeft, health}) {
            label.visibleProperty().bind(label.textProperty().isNotEmpty());
            label.managedProperty().bind(label.visibleProperty());
        }
        HBox boards = new HBox(SPACING,
                new VBox(SPACING, new Label("나"), localView.getContent()),
                new VBox(SPACING, new Label("상대"), remoteView.getContent()));
        ScrollPane scroll = new ScrollPane(boards);
        scroll.setPannable(false);
        scroll.setFocusTraversable(false);
        HBox header = new HBox(SPACING, remoteStatus, timeLeft, health);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox root = new VBox(SPACING, header, resultLabel, status, scroll, back);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.setPadding(new Insets(PADDING));
        scene = new Scene(root, 980 * settings.getWindowSize().getScale(),
                680 * settings.getWindowSize().getScale());
    }

    public GameView getLocalView() { return localView; }
    public Scene getScene() { return scene; }
    public void setStatus(String text) { status.setText(text); }

    public void renderResult(MatchResult result) {
        String outcome = switch (result.winner()) {
            case PLAYER_ONE -> "승리";
            case PLAYER_TWO -> "패배";
            case DRAW -> "무승부";
        };
        String reason = switch (result.reason()) {
            case GAME_OVER -> "게임오버";
            case TIME_UP -> "시간 종료";
            case STOPPED -> "기권";
        };
        resultLabel.setText(String.format("%s %s (나 %d : 상대 %d)",
                outcome, reason, result.firstScore(), result.secondScore()));
        status.setText("");
        back.setText("대기실로");
    }

    public void renderRemainingSeconds(int seconds) {
        timeLeft.setText(String.format(Locale.ROOT, "남은 시간 %d:%02d", seconds / 60, seconds % 60));
    }

    public void renderConnectionHealth(PeerHealth result) {
        health.setText(result == null ? "" : result.displayText());
    }

    public void renderRemote(GameSnapshot snapshot) {
        remoteView.render(snapshot);
        remoteStatus.setText("상대 점수:" + snapshot.getScore() + "점, 상태: " + snapshot.getStatus());
    }
}
