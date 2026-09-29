package tetris.controller;

import tetris.model.Game;
import tetris.model.GameCommand;
import tetris.model.GameSnapshot;
import tetris.view.GameView;

public class GameController {

    private final Game game;
    private final GameView view;
    private final Runnable onReturnToMenu;

    public GameController(Game game, GameView view, Runnable onReturnToMenu) {
        this.game = game;
        this.view = view;
        this.onReturnToMenu = onReturnToMenu;
    }

    public void start() {
        view.setCommandHandler(this::handleCommand);

        game.start();
        refreshView();

        view.show();
    }

    private void handleCommand(GameCommand command) {
        if (game.isFinished()) {
            return;
        }

        game.handleCommand(command);
        refreshView();

        if (command == GameCommand.QUIT_GAME && game.isFinished()) {
            onReturnToMenu.run();
        }
    }

    private void refreshView() {
        GameSnapshot snapshot = game.createSnapshot();
        view.render(snapshot);
    }
}