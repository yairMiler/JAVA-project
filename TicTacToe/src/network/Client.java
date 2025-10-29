package network;


import java.io.*;
import java.net.*;
import java.util.function.BiConsumer;

public class Client {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    // callback: (board, winner) -> {}
    private final BiConsumer<char[], Character> onUpdate;

    public Client(String serverIp, int port, BiConsumer<char[], Character> onUpdate) throws IOException {
        this.socket = new Socket(serverIp, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.onUpdate = onUpdate;

        listen();
    }

    private void listen() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    String[] parts = msg.split(",");
                    char[] board = parts[0].toCharArray();
                    char winner = parts[1].charAt(0);
                    onUpdate.accept(board, winner);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendMove(int pos) {
        out.println("MOVE:" + pos);
    }
}
