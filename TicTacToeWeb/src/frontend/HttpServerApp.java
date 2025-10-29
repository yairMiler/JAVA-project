package frontend;


import backend.TicTacToeBackend;
import DB.DatabaseHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.awt.Desktop;
import java.net.URI;

/**
 * Minimal HTTP server for TicTacToe.
 * - Serves the web UI at:  http://localhost:8000/TicTacToe
 * - Exposes API endpoints under /TicTacToe/...
 * Uses com.sun.net.httpserver (part of the JDK) and org.json for JSON build.
 */
public class HttpServerApp {

    // single backend instance for this server process
    private static final TicTacToeBackend backend = new TicTacToeBackend();
    // Database handler (may be null if DB init fails)
    private static DatabaseHandler db;

    // port the server will listen on
    public static final int PORT = 8000;

    public static void main(String[] args) throws Exception {
        // initialize database (file path is absolute to your Windows folder)
        try {
            db = new DatabaseHandler("C:/Games/TicTacToeWeb/tictactoe.db");
            System.out.println("DB opened: C:/Games/TicTacToeWeb/tictactoe.db");
        } catch (SQLException e) {
            db = null;
            System.err.println("DB init failed: " + e.getMessage());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        System.out.println("HTTP server started. Open: http://localhost:" + PORT + "/TicTacToe");

        // Serve the main page (index.html)
        server.createContext("/TicTacToe", new RootHandler());

        // API endpoints under /TicTacToe/*
        server.createContext("/TicTacToe/newgame", new NewGameHandler());
        server.createContext("/TicTacToe/move", new MoveHandler());
        server.createContext("/TicTacToe/board", new BoardHandler());
        server.createContext("/TicTacToe/leaderboard", new LeaderboardHandler());
        server.createContext("/TicTacToe/saveWin", new SaveWinHandler());// optional: save a player's win
        server.createContext("/TicTacToe/icon.ico", new IconHandler());

        // use a cached thread pool for handlers (concurrent clients supported)
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
    }

    // -----------------------
    // Handlers
    // -----------------------

    /** Serves the static HTML file (web UI) */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Only GET allowed for the UI
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }

            File f = new File("C:/Games/TicTacToeWeb/index.html");
            if (!f.exists()) {
                sendText(exchange, 404, "UI not found. Put index.html in C:/Games/TicTacToeWeb/");
                return;
            }


            byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

        }
    }

    /** Reset / start a new game: GET /TicTacToe/newgame?mode=offline|online */
    static class NewGameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // accept GET or POST
            String query = exchange.getRequestURI().getQuery();
            boolean onlineMode = query != null && query.contains("mode=online");

            backend.reset();
            // The server doesn't need to keep a separate mode flag for now; client can use online/offline UI.
            sendJson(exchange, buildBoardJson());
        }
    }

    /** Make a move: GET /TicTacToe/move?pos=4  (pos = 0..8) */
    static class MoveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // accept GET or POST queries
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = queryToMap(query);
            String posS = params.get("pos");
            if (posS == null) {
                sendText(exchange, 400, "Missing pos parameter");
                return;
            }

            int pos;
            try {
                pos = Integer.parseInt(posS);
            } catch (NumberFormatException nfe) {
                sendText(exchange, 400, "Invalid pos");
                return;
            }

            // Player is always X in this design
            boolean ok = backend.playerMove(pos);
            if (ok && !backend.isGameOver()) {
                backend.computerMove(); // server runs CPU O move
            }

            sendJson(exchange, buildBoardJson());
        }
    }

    /** Return current board as JSON: GET /TicTacToe/board */
    static class BoardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendJson(exchange, buildBoardJson());
        }
    }

    /** Return leaderboard plain-text (server uses DB.getLeaderboard) */
    static class LeaderboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (db == null) {
                sendText(exchange, 200, "(no DB)");
                return;
            }
            try {
                List<String> rows = db.getLeaderboard();
                String body = String.join("\n", rows.isEmpty() ? Collections.singletonList("(no records)") : rows);
                sendText(exchange, 200, body);
            } catch (SQLException e) {
                e.printStackTrace();
                sendText(exchange, 500, "DB error");
            }
        }
    }

    /** Save a win into DB: POST/GET /TicTacToe/saveWin?name=alice */
    static class SaveWinHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (db == null) { sendText(exchange, 500, "DB unavailable"); return; }
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = queryToMap(query);
            String name = params.getOrDefault("name", "player").trim();
            if (name.isEmpty()) name = "player";
            try {
                db.addWin(name);
                sendText(exchange, 200, "OK");
            } catch (SQLException e) {
                e.printStackTrace();
                sendText(exchange, 500, "DB save failed");
            }
        }
    }

    // -----------------------
    // Helpers: JSON builders and IO
    // -----------------------

    /** Build a JSONObject { board: [...], winner: "X" or "O" or "D" or " " } */
    private static JSONObject buildBoardJson() {
        JSONObject obj = new JSONObject();
        JSONArray arr = new JSONArray();
        char[] b = backend.getBoard();
        for (char c : b) arr.put(String.valueOf(c));
        obj.put("board", arr);
        obj.put("winner", String.valueOf(backend.getWinner()));
        return obj;
    }

    /** Send a plain text response */
    private static void sendText(HttpExchange ex, int code, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    /** Send a JSONObject as application/json */
    private static void sendJson(HttpExchange ex, JSONObject json) throws IOException {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    /** Parse query string like "pos=2&x=1" into a map (URL-decoded) */
    private static Map<String, String> queryToMap(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String p : pairs) {
            int idx = p.indexOf('=');
            try {
                if (idx > 0) {
                    String k = URLDecoder.decode(p.substring(0, idx), StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(p.substring(idx + 1), StandardCharsets.UTF_8);
                    map.put(k, v);
                } else {
                    map.put(URLDecoder.decode(p, StandardCharsets.UTF_8), "");
                }
            } catch (IllegalArgumentException ignored) { /* ignore bad encodings */ }
        }
        return map;
    }

    /** Serves the favicon (icon shown in the browser tab) */
    static class IconHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File iconFile = new File("C:/Games/TicTacToeWeb/icon.ico");
            if (!iconFile.exists()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] iconBytes = java.nio.file.Files.readAllBytes(iconFile.toPath());
            exchange.getResponseHeaders().set("Content-Type", "image/x-icon");
            exchange.sendResponseHeaders(200, iconBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(iconBytes);
            }
        }
    }
}
