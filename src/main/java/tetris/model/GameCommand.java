package tetris.model;

public enum GameCommand {

    NONE,
    MOVE_LEFT,
    MOVE_RIGHT,
    ROTATE_CLOCKWISE,
    ROTATE_COUNTERCLOCKWISE,
    SOFT_DROP,
    HARD_DROP,
    HOLD,
    PAUSE,
    QUIT_GAME
}