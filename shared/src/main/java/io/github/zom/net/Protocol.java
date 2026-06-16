package io.github.zom.net;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Network protocol definitions for client-server communication.
 *
 * All packets are JSON-encoded, preceded by a 4-byte big-endian length prefix.
 * Wire format:  [4 bytes: payload length] [UTF-8 JSON payload]
 *
 * Every JSON payload has a "type" field that identifies the packet class.
 */
public final class Protocol {

    public static final int DEFAULT_PORT = 7777;

    /** Maximum players a server will accept. Configurable via ServerLauncher. */
    public static final int DEFAULT_MAX_PLAYERS = 8;

    private Protocol() {}

    // ── Packet type constants ────────────────────────────────────────────────

    public static final String TYPE_JOIN_REQUEST      = "join_request";
    public static final String TYPE_JOIN_RESPONSE     = "join_response";
    public static final String TYPE_PLAYER_INPUT      = "player_input";
    public static final String TYPE_WORLD_STATE       = "world_state";
    public static final String TYPE_PLAYER_DISCONNECT = "player_disconnect";

    // ── Join ─────────────────────────────────────────────────────────────────

    /** Sent by client to request joining the server. */
    public static class JoinRequest {
        public String type = TYPE_JOIN_REQUEST;
        public String playerName = "Player";
    }

    /** Sent by server in response to a JoinRequest. */
    public static class JoinResponse {
        public String type = TYPE_JOIN_RESPONSE;
        public boolean accepted;
        public int assignedId;          // entity id assigned to this player (0 if rejected)
        public String rejectReason;     // null if accepted
    }

    // ── Player input (client → server) ───────────────────────────────────────

    /** Sent every frame (or on change) by the client to relay inputs. */
    public static class PlayerInput {
        public String type = TYPE_PLAYER_INPUT;
        public float moveX;             // -1 .. 1
        public float moveY;             // -1 .. 1
        public boolean meleeRequested;
        public boolean rangedRequested;
        public boolean interactRequested;
        public String direction;        // "up","down","left","right"
    }

    // ── World state (server → client) ────────────────────────────────────────

    /** Broadcasted by the server each tick with the state of all entities. */
    public static class WorldStateUpdate {
        public String type = TYPE_WORLD_STATE;
        public EntitySnapshot[] entities;
    }

    /** Minimal snapshot of one entity for network sync. */
    public static class EntitySnapshot {
        public int entityId;
        public float x, y, w, h;
        public String direction;
        public String pose;
        public float stateTime;
        public boolean isPlayer;
        public boolean isZed;
        public boolean isItem;
        // Zed-specific
        public String zedType;
        public String skinName;
        public boolean alive;
        // Player-specific
        public String playerSkin;
        public float hp, maxHp;
        // Item-specific
        public int itemId;
        public int quantity;
    }

    /** Sent by server when a player disconnects. */
    public static class PlayerDisconnect {
        public String type = TYPE_PLAYER_DISCONNECT;
        public int playerId;
    }

    // ── Serialization helpers ────────────────────────────────────────────────

    private static final Json json = new Json();
    static {
        json.setOutputType(JsonWriter.OutputType.json);
        json.setIgnoreUnknownFields(true);
    }

    /** Serialize any packet object to a JSON string. */
    public static String toJson(Object packet) {
        return json.toJson(packet);
    }

    /** Deserialize a JSON string to a packet object of the given type. */
    public static <T> T fromJson(Class<T> type, String jsonStr) {
        return json.fromJson(type, jsonStr);
    }

    /**
     * Peek at the "type" field of a JSON packet string without full deserialization.
     * Returns null if not found.
     */
    public static String peekType(String jsonStr) {
        // Simple substring search — avoids full parse overhead
        int idx = jsonStr.indexOf("\"type\"");
        if (idx < 0) return null;
        int colon = jsonStr.indexOf(':', idx);
        if (colon < 0) return null;
        int q1 = jsonStr.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = jsonStr.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return jsonStr.substring(q1 + 1, q2);
    }

    /** Write a length-prefixed UTF-8 JSON message to an OutputStream. */
    public static void sendMessage(java.io.OutputStream out, String jsonStr) throws java.io.IOException {
        byte[] payload = jsonStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] header = new byte[4];
        header[0] = (byte) ((payload.length >> 24) & 0xFF);
        header[1] = (byte) ((payload.length >> 16) & 0xFF);
        header[2] = (byte) ((payload.length >>  8) & 0xFF);
        header[3] = (byte) ((payload.length      ) & 0xFF);
        synchronized (out) {
            out.write(header);
            out.write(payload);
            out.flush();
        }
    }

    /** Read a length-prefixed UTF-8 JSON message from an InputStream. Blocks until complete. */
    public static String readMessage(java.io.InputStream in) throws java.io.IOException {
        byte[] header = new byte[4];
        readFully(in, header);
        int length = ((header[0] & 0xFF) << 24)
                   | ((header[1] & 0xFF) << 16)
                   | ((header[2] & 0xFF) <<  8)
                   | ((header[3] & 0xFF));
        if (length <= 0 || length > 1_000_000) {
            throw new java.io.IOException("Invalid message length: " + length);
        }
        byte[] payload = new byte[length];
        readFully(in, payload);
        return new String(payload, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void readFully(java.io.InputStream in, byte[] buf) throws java.io.IOException {
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) throw new java.io.EOFException("Connection closed");
            off += n;
        }
    }
}
