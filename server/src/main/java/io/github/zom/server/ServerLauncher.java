package io.github.zom.server;

import io.github.zom.net.GameServer;
import io.github.zom.net.Protocol;

import java.io.IOException;

/**
 * Launches the headless game server.
 *
 * Usage:  java -jar server.jar [port] [maxPlayers]
 *   port       — TCP port to listen on (default 7777)
 *   maxPlayers — maximum concurrent players (default 8)
 */
public class ServerLauncher {
    public static void main(String[] args) {
        int port = Protocol.DEFAULT_PORT;
        int maxPlayers = Protocol.DEFAULT_MAX_PLAYERS;

        if (args.length >= 1) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        if (args.length >= 2) {
            try { maxPlayers = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }

        GameServer server = new GameServer(port, maxPlayers);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "ServerShutdown"));

        try {
            server.start(); // blocks
        } catch (IOException e) {
            System.err.println("[Server] Fatal: " + e.getMessage());
            System.exit(1);
        }
    }
}