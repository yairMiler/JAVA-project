package network;

import backEnd.TicTacToeBackend;
import java.io.*;
import java.net.*;

public class Server {
    private static final int PORT = 5000;

    public static void main(String[] args) throws IOException {
        System.out.println("TicTacToe Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket);
                new Thread(new ClientHandler(socket)).start();
            }
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    private final TicTacToeBackend backend = new TicTacToeBackend();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            sendBoard(out);

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("MOVE:")) {
                    int pos = Integer.parseInt(msg.substring(5));

                    boolean ok = backend.playerMove(pos);
                    if (ok && !backend.isGameOver()) {
                        backend.computerMove(); // CPU plays
                    }
                    sendBoard(out);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBoard(PrintWriter out) {
        char[] b = backend.getBoard();
        out.println(new String(b) + "," + backend.getWinner());
    }
}
