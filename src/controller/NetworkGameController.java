package controller;

import model.EndReason;
import model.GameMode;
import model.GameModel;
import net.Connection;
import net.Protocol.Bye;
import net.Protocol.Kind;
import net.Protocol.Message;
import net.Protocol.Move;
import net.Protocol.Simple;

import javax.swing.Timer;
import java.util.function.Consumer;

/**
 * A LAN game. Both machines keep their own GameModel and validate every move
 * they receive; anything illegal ends the link as a desync. The host plays X
 * in odd rounds and O in even rounds, so pieces alternate without being sent.
 * New rounds need both players' agreement; undo and hints are not available.
 */
public class NetworkGameController implements GameSession, Connection.Listener {
    private final Connection connection;
    private final boolean host;
    private final String opponentName;
    private final int turnSeconds;
    private final GameModel model;

    private GameSession.Listener listener;
    private int round = 1;
    private boolean connected = true;
    private boolean restartPending;
    private boolean desync;

    private long deadline;
    private Timer clockTimer;

    public NetworkGameController(Connection connection, boolean host, int size, int turnSeconds, String opponentName) {
        this.connection = connection;
        this.host = host;
        this.turnSeconds = turnSeconds;
        this.opponentName = opponentName;
        this.model = new GameModel(size);
    }

    public String getOpponentName() { return opponentName; }
    public boolean isHost() { return host; }
    public boolean isConnected() { return connected; }
    public boolean isRestartPending() { return restartPending; }
    public int getRound() { return round; }

    @Override
    public void setListener(GameSession.Listener listener) { this.listener = listener; }

    @Override
    public GameModel getModel() { return model; }

    @Override
    public GameMode getMode() { return GameMode.LAN; }

    @Override
    public int getHumanSide() {
        return (round % 2 == 1) == host ? GameModel.X : GameModel.O;
    }

    private int opponentSide() {
        return GameModel.opponent(getHumanSide());
    }

    @Override
    public void start() {
        connection.setListener(this);
        restartClock();
    }

    @Override
    public boolean acceptsInput() {
        return connected && !restartPending && !model.isOver() && model.getCurrentPlayer() == getHumanSide();
    }

    @Override
    public void play(int r, int c) {
        if (!acceptsInput() || !model.play(r, c)) return;
        connection.send(new Move(r, c));
        afterChange();
    }

    @Override
    public boolean canUndo() { return false; }

    @Override
    public void undo() {
        // Not offered in LAN games
    }

    /** Asks the opponent for a new round; it starts when they accept. */
    @Override
    public void restart() {
        requestNewRound();
    }

    @Override
    public void newRound(boolean swapSides) {
        // Pieces always alternate between rounds in LAN games
        requestNewRound();
    }

    private void requestNewRound() {
        if (!connected || restartPending) return;
        restartPending = true;
        connection.send(new Simple(Kind.RESTART_REQ));
        fireStateChanged();
    }

    /** Answer to the opponent's request for a new round. */
    public void answerRestart(boolean accept) {
        if (!connected) return;
        connection.send(new Simple(accept ? Kind.RESTART_OK : Kind.RESTART_NO));
        if (accept) beginNextRound();
    }

    @Override
    public void resign() {
        if (!connected || model.isOver()) return;
        model.forfeit(getHumanSide(), EndReason.RESIGN);
        connection.send(new Simple(Kind.RESIGN));
        afterChange();
    }

    @Override
    public boolean isAiThinking() { return false; }

    @Override
    public void requestHint(Consumer<int[]> onHint) {
        // Hints would be unfair against a person on another machine
    }

    @Override
    public int getTurnSeconds() { return turnSeconds; }

    @Override
    public long getRemainingMillis() {
        return deadline == 0 ? -1 : Math.max(0, deadline - System.currentTimeMillis());
    }

    @Override
    public void pauseClock() {
        // Both machines run the clock, so it cannot pause for one of them
    }

    @Override
    public void resumeClock() {
    }

    @Override
    public void dispose() {
        stopClock();
        listener = null;
        if (connected) connection.close("left");
    }

    // ---- Connection.Listener ----

    @Override
    public void onMessage(Message message) {
        if (message instanceof Move m) {
            if (model.isOver() || restartPending || model.getCurrentPlayer() != opponentSide() || !model.play(m.row(), m.col())) {
                desync = true;
                connection.close("desync");
                return;
            }
            afterChange();
        } else if (message instanceof Simple s) {
            switch (s.kind()) {
                case RESIGN -> forfeitOpponent(EndReason.RESIGN);
                case TIMEOUT -> forfeitOpponent(EndReason.TIMEOUT);
                case RESTART_REQ -> {
                    if (restartPending) {
                        // Both asked at once: treat theirs as the answer to ours
                        connection.send(new Simple(Kind.RESTART_OK));
                        beginNextRound();
                    } else if (listener != null) {
                        listener.onRestartRequested();
                    }
                }
                case RESTART_OK -> {
                    if (restartPending) beginNextRound();
                }
                case RESTART_NO -> {
                    restartPending = false;
                    if (listener != null) listener.onRestartDeclined();
                    fireStateChanged();
                }
                default -> { }
            }
        } else if (message instanceof Bye) {
            // onClosed follows with reason "bye"
        }
    }

    @Override
    public void onClosed(String reason) {
        if (!connected) return;
        connected = false;
        restartPending = false;
        stopClock();
        String why = desync ? "desync" : reason;
        if (!model.isOver() && !"local".equals(reason)) {
            model.forfeit(opponentSide(), EndReason.DISCONNECT);
        }
        fireStateChanged();
        if (listener != null) listener.onConnectionLost(why);
    }

    private void forfeitOpponent(EndReason reason) {
        if (model.isOver()) return;
        model.forfeit(opponentSide(), reason);
        afterChange();
    }

    private void beginNextRound() {
        restartPending = false;
        round++;
        model.reset();
        restartClock();
        fireStateChanged();
    }

    private void afterChange() {
        restartClock();
        fireStateChanged();
        if (model.isOver() && listener != null) listener.onGameOver();
    }

    private void fireStateChanged() {
        if (listener != null) listener.onStateChanged();
    }

    // ---- turn clock: both turns are shown, but each machine only times out its own player ----

    private void restartClock() {
        if (turnSeconds <= 0 || model.isOver() || !connected) {
            stopClock();
            return;
        }
        deadline = System.currentTimeMillis() + turnSeconds * 1000L;
        if (clockTimer == null) clockTimer = new Timer(100, e -> tickClock());
        clockTimer.start();
    }

    private void stopClock() {
        deadline = 0;
        if (clockTimer != null) clockTimer.stop();
    }

    private void tickClock() {
        if (deadline == 0) return;
        long remaining = getRemainingMillis();
        if (listener != null) listener.onClockTick(remaining);
        if (remaining > 0 || model.getCurrentPlayer() != getHumanSide()) return;
        model.forfeit(getHumanSide(), EndReason.TIMEOUT);
        connection.send(new Simple(Kind.TIMEOUT));
        afterChange();
    }
}
