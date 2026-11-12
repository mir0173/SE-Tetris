package tetris.network;

/** 준비가 확인되었을 때 대전 시작 코드에 전달할 정보 */
public record MatchStart(long matchId, PeerRole role, LobbySettings settings) {
}
