package tetris.view;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tetris.model.Difficulty;
import tetris.network.LobbyMode;
import tetris.network.LobbySettings;
import tetris.network.LobbyState;
import tetris.network.MatchStart;
import tetris.network.P2pLobbyController;
import tetris.network.PeerRole;

public final class JavaFxP2pLobbyView {
    private static final double WIDTH = 620;
    private static final double HEIGHT = 650;
    private static final double SPACING = 12;
    private static final double PADDING = 24;

    private final TextField addressField = new TextField("127.0.0.1");
    private final TextField portField = new TextField(String.valueOf(P2pLobbyController.DEFAULT_PORT));
    private final Button hostButton = new Button("서버 열기");
    private final Button connectButton = new Button("접속");
    private final Button cancelButton = new Button("연결 취소 / 종료");
    private final Button readyButton = new Button("게임 시작");
    private final Button resetButton = new Button("대기실로 돌아가기");
    private final Button backButton = new Button("메뉴로");
    private final ComboBox<LobbyMode> modeBox = new ComboBox<>();
    private final ComboBox<Difficulty> difficultyBox = new ComboBox<>();
    private final Spinner<Integer> durationSpinner = new Spinner<>(
            LobbySettings.MIN_TIME_LIMIT_SECONDS, LobbySettings.MAX_TIME_LIMIT_SECONDS,
            LobbySettings.DEFAULT_TIME_LIMIT_SECONDS, 10);
    private final Label statusLabel = new Label();
    private final Label localReadyLabel = new Label("나: 대기");
    private final Label remoteReadyLabel = new Label("상대: 대기");
    private final Label matchLabel = new Label();
    private final Scene scene;

    private P2pLobbyController controller;
    private boolean rendering;

    public JavaFxP2pLobbyView() {
        addressField.setId("p2p-address");
        portField.setId("p2p-port");
        hostButton.setId("p2p-host");
        connectButton.setId("p2p-connect");
        readyButton.setId("p2p-ready");
        modeBox.setId("p2p-mode");
        difficultyBox.setId("p2p-difficulty");
        durationSpinner.setId("p2p-duration");
        statusLabel.setId("p2p-status");
        localReadyLabel.setId("p2p-local-ready");
        remoteReadyLabel.setId("p2p-remote-ready");
        matchLabel.setId("p2p-match");
        resetButton.setId("p2p-reset");
        cancelButton.setId("p2p-cancel");
        backButton.setId("p2p-back");

        modeBox.getItems().setAll(LobbyMode.values());
        difficultyBox.getItems().setAll(Difficulty.values());
        statusLabel.setWrapText(true);
        matchLabel.setWrapText(true);
        Label ipLabel = new Label("내 IPv4: " + findLocalAddresses());
        ipLabel.setWrapText(true);

        GridPane fields = new GridPane();
        fields.setHgap(SPACING);
        fields.setVgap(SPACING);
        fields.addRow(0, new Label("상대 IPv4"), addressField);
        fields.addRow(1, new Label("포트"), portField);
        fields.addRow(2, new Label("대전 모드"), modeBox);
        fields.addRow(3, new Label("난이도"), difficultyBox);
        fields.addRow(4, new Label("제한시간(초)"), durationSpinner);

        VBox root = new VBox(SPACING, new Label("P2P 대전"), ipLabel,
                fields, new HBox(SPACING, hostButton, connectButton), statusLabel,
                localReadyLabel, remoteReadyLabel, readyButton, matchLabel, resetButton,
                new HBox(SPACING, cancelButton, backButton));
        root.setPadding(new Insets(PADDING));
        scene = new Scene(root, WIDTH, HEIGHT);
        modeBox.valueProperty().addListener((observable, previous, current) -> updateSettings());
        difficultyBox.valueProperty().addListener((observable, previous, current) -> updateSettings());
        durationSpinner.valueProperty().addListener((observable, previous, current) -> updateSettings());
    }

    /** 화면 조작을 컨트롤러와 메뉴 복귀 동작에 연결한다. */
    public void bind(P2pLobbyController controller, Runnable onBack) {
        this.controller = controller;
        hostButton.setOnAction(event -> runAction(() -> controller.host(readPort())));
        connectButton.setOnAction(event -> runAction(() -> controller.connect(addressField.getText(), readPort())));
        cancelButton.setOnAction(event -> controller.cancel());
        readyButton.setOnAction(event -> controller.toggleReady());
        resetButton.setOnAction(event -> controller.returnToLobby());
        backButton.setOnAction(event -> onBack.run());
        controller.refresh();
    }

    public Scene getScene() {
        return scene;
    }

    /** 화면 스레드에서 대기실의 최신 상태를 표시한다. */
    public void render(LobbyState state) {
        rendering = true;
        try {
            modeBox.setValue(state.settings().mode());
            difficultyBox.setValue(state.settings().difficulty());
            if (state.settings().mode() == LobbyMode.TIME_LIMIT) {
                durationSpinner.getValueFactory().setValue(state.settings().timeLimitSeconds());
            }
            boolean idle = state.phase() == LobbyState.Phase.IDLE;
            boolean hostCanConfigure = state.role() == PeerRole.HOST
                    && (state.phase() == LobbyState.Phase.HOSTING || state.phase() == LobbyState.Phase.CONNECTED);
            addressField.setDisable(!idle);
            portField.setDisable(!idle);
            hostButton.setDisable(!idle);
            connectButton.setDisable(!idle);
            cancelButton.setDisable(idle);
            modeBox.setDisable(!hostCanConfigure);
            difficultyBox.setDisable(!hostCanConfigure);
            durationSpinner.setDisable(!hostCanConfigure || state.settings().mode() != LobbyMode.TIME_LIMIT);
            readyButton.setDisable(state.phase() != LobbyState.Phase.CONNECTED);
            readyButton.setText(state.localReady() ? "준비 취소" : "게임 시작");
            localReadyLabel.setText("나: " + (state.localReady() ? "준비 완료" : "대기"));
            remoteReadyLabel.setText("상대: " + (state.remoteReady() ? "준비 완료" : "대기"));
            statusLabel.setText(state.message());
            resetButton.setVisible(state.role() == PeerRole.HOST && state.phase() == LobbyState.Phase.STARTED);
            resetButton.setManaged(resetButton.isVisible());
            if (state.phase() != LobbyState.Phase.STARTED) {
                matchLabel.setText("");
            }
        } finally {
            rendering = false;
        }
    }

    /** 실제 대전 시작 콜백을 연결하기 전까지 준비 완료 결과만 표시한다. */
    public void showMatchReady(MatchStart start) {
        matchLabel.setText("준비 완료 · " + start.settings().mode()
                + " / " + start.settings().difficulty().getDisplayName());
    }

    private void updateSettings() {
        if (rendering || controller == null || modeBox.getValue() == null || difficultyBox.getValue() == null) {
            return;
        }
        runAction(() -> controller.updateSettings(new LobbySettings(modeBox.getValue(), difficultyBox.getValue(),
                modeBox.getValue() == LobbyMode.TIME_LIMIT ? durationSpinner.getValue() : 0)));
    }

    private int readPort() {
        try {
            int port = Integer.parseInt(portField.getText().strip());
            if (port < 1 || port > 65_535) {
                throw new NumberFormatException();
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("포트는 1~65535 사이의 숫자로 입력해주세요.");
        }
    }

    private void runAction(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private static String findLocalAddresses() {
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return "주소를 확인할 수 없습니다.";
            }

            for (var network : Collections.list(interfaces)) {
                if (!network.isUp() || network.isLoopback()) {
                    continue;
                }

                for (var address : Collections.list(network.getInetAddresses())) {
                    if (address instanceof Inet4Address
                            && !address.isLoopbackAddress()
                            && !address.isLinkLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }

            return "LAN 주소 없음";
        } catch (SocketException exception) {
            return "주소 조회 실패";
        }
    }
}