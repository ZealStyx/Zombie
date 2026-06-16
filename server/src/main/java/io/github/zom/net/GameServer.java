package io.github.zom.net;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * Lightweight TCP game server.
 *
 * Accepts player connections up to a configurable limit, receives player input,
 * and broadcasts world state snapshots to all connected clients.
 *
 * Wire format per message:
 *   [4 bytes big-endian length] [UTF-8 JSON payload]
 *
 * Thread model:
 *   - 1 accept thread (blocks on ServerSocket.accept)
 *   - 1 reader thread per client (blocks on InputStream.read)
 *   - 1 game-loop thread (ticks ECS, broadcasts state)
 */
public class GameServer {

    private final int port;
    private final int maxPlayers;
    private ServerSocket serverSocket;
    private volatile boolean running;

    private final ConcurrentHashMap<Integer, ClientConnection> clients = new ConcurrentHashMap<>();
    private int nextPlayerId = 1;

    // Latest inputs from each player (written by reader threads, read by game loop)
    private final ConcurrentHashMap<Integer, Protocol.PlayerInput> playerInputs = new ConcurrentHashMap<>();

    public GameServer(int port, int maxPlayers) {
        this.port = port;
        this.maxPlayers = maxPlayers;
    }

    /** Start listening for connections. Blocks the calling thread. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("[Server] Listening on port " + port + " (max " + maxPlayers + " players)");

        // Start the game loop in a separate thread
        Thread gameLoop = new Thread(this::gameLoop, "GameLoop");
        gameLoop.setDaemon(true);
        gameLoop.start();

        // Accept loop
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                handleNewConnection(socket);
            } catch (SocketException e) {
                if (running) {
                    System.err.println("[Server] Accept error: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        for (ClientConnection cc : clients.values()) {
            cc.close();
        }
        clients.clear();
        System.out.println("[Server] Stopped.");
    }

    // ── Connection handling ──────────────────────────────────────────────────

    private synchronized void handleNewConnection(Socket socket) {
        try {
            // Read the join request
            String joinJson = readMessage(socket.getInputStream());
            String type = Protocol.peekType(joinJson);

            if (!Protocol.TYPE_JOIN_REQUEST.equals(type)) {
                socket.close();
                return;
            }

            Protocol.JoinRequest req = Protocol.fromJson(Protocol.JoinRequest.class, joinJson);

            if (clients.size() >= maxPlayers) {
                // Reject — server full
                Protocol.JoinResponse resp = new Protocol.JoinResponse();
                resp.accepted = false;
                resp.rejectReason = "Server is full (" + maxPlayers + "/" + maxPlayers + ")";
                sendMessage(socket.getOutputStream(), Protocol.toJson(resp));
                socket.close();
                System.out.println("[Server] Rejected " + req.playerName + " — server full.");
                return;
            }

            int playerId = nextPlayerId++;
            Protocol.JoinResponse resp = new Protocol.JoinResponse();
            resp.accepted = true;
            resp.assignedId = playerId;
            sendMessage(socket.getOutputStream(), Protocol.toJson(resp));

            ClientConnection cc = new ClientConnection(playerId, req.playerName, socket);
            clients.put(playerId, cc);
            System.out.println("[Server] " + req.playerName + " joined as player " + playerId
                    + " (" + clients.size() + "/" + maxPlayers + ")");

            // Start reader thread for this client
            Thread reader = new Thread(() -> clientReaderLoop(cc), "ClientReader-" + playerId);
            reader.setDaemon(true);
            reader.start();

        } catch (IOException e) {
            System.err.println("[Server] Error handling new connection: " + e.getMessage());
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void clientReaderLoop(ClientConnection cc) {
        try {
            while (running && !cc.socket.isClosed()) {
                String msg = readMessage(cc.socket.getInputStream());
                String type = Protocol.peekType(msg);
                if (Protocol.TYPE_PLAYER_INPUT.equals(type)) {
                    Protocol.PlayerInput input = Protocol.fromJson(Protocol.PlayerInput.class, msg);
                    playerInputs.put(cc.playerId, input);
                }
            }
        } catch (IOException e) {
            // Client disconnected
        } finally {
            disconnectClient(cc.playerId);
        }
    }

    private void disconnectClient(int playerId) {
        ClientConnection cc = clients.remove(playerId);
        if (cc != null) {
            cc.close();
            playerInputs.remove(playerId);
            System.out.println("[Server] Player " + playerId + " (" + cc.playerName + ") disconnected."
                    + " (" + clients.size() + "/" + maxPlayers + ")");

            // Notify remaining clients
            Protocol.PlayerDisconnect pkt = new Protocol.PlayerDisconnect();
            pkt.playerId = playerId;
            broadcast(Protocol.toJson(pkt));
        }
    }

    // ── Game loop ────────────────────────────────────────────────────────────

    private void gameLoop() {
        final long TICK_MS = 50; // 20 ticks/sec
        while (running) {
            long start = System.currentTimeMillis();

            // TODO: process playerInputs → update headless ECS world → build WorldStateUpdate
            // For now, just echo a minimal state so clients know the server is alive.

            // Build and broadcast world state
            Protocol.WorldStateUpdate state = new Protocol.WorldStateUpdate();
            state.entities = new Protocol.EntitySnapshot[0]; // placeholder
            broadcast(Protocol.toJson(state));

            long elapsed = System.currentTimeMillis() - start;
            if (elapsed < TICK_MS) {
                try { Thread.sleep(TICK_MS - elapsed); } catch (InterruptedException ignored) {}
            }
        }
    }

    // ── Wire helpers ─────────────────────────────────────────────────────────

    private void broadcast(String json) {
        for (ClientConnection cc : clients.values()) {
            try {
                sendMessage(cc.socket.getOutputStream(), json);
            } catch (IOException e) {
                disconnectClient(cc.playerId);
            }
        }
    }

    /** Write a length-prefixed UTF-8 JSON message. */
    static void sendMessage(OutputStream out, String json) throws IOException {
        Protocol.sendMessage(out, json);
    }

    /** Read a length-prefixed UTF-8 JSON message. Blocks until complete. */
    static String readMessage(InputStream in) throws IOException {
        return Protocol.readMessage(in);
    }

    // ── Client connection wrapper ────────────────────────────────────────────

    static class ClientConnection {
        final int playerId;
        final String playerName;
        final Socket socket;

        ClientConnection(int playerId, String playerName, Socket socket) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.socket = socket;
        }

        void close() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /** @return number of currently connected players. */
    public int getPlayerCount() {
        return clients.size();
    }

    /** @return the configured maximum player count. */
    public int getMaxPlayers() {
        return maxPlayers;
    }
}
