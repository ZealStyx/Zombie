package io.github.zom.net;

import com.badlogic.gdx.Gdx;

import java.io.*;
import java.net.Socket;

/**
 * Client-side network handler.
 *
 * Connects to a GameServer, sends player input packets, and receives
 * world state updates. Runs a background reader thread.
 *
 * Usage:
 * GameClient client = new GameClient();
 * client.connect("127.0.0.1", 7777, "MyName");
 * // each frame:
 * client.sendInput(inputPacket);
 * Protocol.WorldStateUpdate state = client.getLatestState();
 */
public class GameClient {

    private Socket socket;
    private volatile boolean connected;
    private int assignedPlayerId;
    private String rejectReason;

    // Latest world state from server (written by reader thread, read by game
    // thread)
    private volatile Protocol.WorldStateUpdate latestState;

    public GameClient() {
    }

    /**
     * Connect to the server. Blocks until join response is received.
     *
     * @return true if accepted, false if rejected (check getRejectReason()).
     */
    public boolean connect(String host, int port, String playerName) {
        try {
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);

            // Send join request
            Protocol.JoinRequest req = new Protocol.JoinRequest();
            req.playerName = playerName;
            String reqJson = Protocol.toJson(req);
            Gdx.app.log("GameClient", "DEBUG: Sending JoinRequest: " + reqJson);
            Protocol.sendMessage(socket.getOutputStream(), reqJson);

            // Wait for join response
            String respJson = Protocol.readMessage(socket.getInputStream());
            Gdx.app.log("GameClient", "DEBUG: Received JoinResponse: " + respJson);
            Protocol.JoinResponse resp = Protocol.fromJson(Protocol.JoinResponse.class, respJson);

            if (!resp.accepted) {
                rejectReason = resp.rejectReason;
                socket.close();
                return false;
            }

            assignedPlayerId = resp.assignedId;
            connected = true;
            Gdx.app.log("GameClient", "Connected as player " + assignedPlayerId);

            // Start reader thread
            Thread reader = new Thread(this::readerLoop, "ClientReader");
            reader.setDaemon(true);
            reader.start();

            return true;

        } catch (IOException e) {
            rejectReason = "Connection failed: " + e.getMessage();
            Gdx.app.error("GameClient", rejectReason);
            return false;
        }
    }

    /** Disconnect from server. */
    public void disconnect() {
        connected = false;
        try {
            if (socket != null)
                socket.close();
        } catch (IOException ignored) {
        }
        Gdx.app.log("GameClient", "Disconnected.");
    }

    /** Send player input to server. Non-blocking. */
    public void sendInput(Protocol.PlayerInput input) {
        if (!connected || socket == null)
            return;
        try {
            Protocol.sendMessage(socket.getOutputStream(), Protocol.toJson(input));
        } catch (IOException e) {
            Gdx.app.error("GameClient", "Send failed: " + e.getMessage());
            disconnect();
        }
    }

    /** @return the latest world state snapshot, or null if none received yet. */
    public Protocol.WorldStateUpdate getLatestState() {
        return latestState;
    }

    /** @return assigned player entity id from server. */
    public int getAssignedPlayerId() {
        return assignedPlayerId;
    }

    /** @return reason for rejection or connection failure. */
    public String getRejectReason() {
        return rejectReason;
    }

    public boolean isConnected() {
        return connected;
    }

    // ── Reader thread ────────────────────────────────────────────────────────

    private void readerLoop() {
        try {
            while (connected && !socket.isClosed()) {
                String msg = Protocol.readMessage(socket.getInputStream());
                String type = Protocol.peekType(msg);

                if (Protocol.TYPE_WORLD_STATE.equals(type)) {
                    latestState = Protocol.fromJson(Protocol.WorldStateUpdate.class, msg);
                } else if (Protocol.TYPE_PLAYER_DISCONNECT.equals(type)) {
                    Protocol.PlayerDisconnect pkt = Protocol.fromJson(Protocol.PlayerDisconnect.class, msg);
                    Gdx.app.log("GameClient", "Player " + pkt.playerId + " disconnected.");
                }
            }
        } catch (IOException e) {
            if (connected) {
                Gdx.app.error("GameClient", "Reader error: " + e.getMessage());
            }
        } finally {
            connected = false;
        }
    }
}
