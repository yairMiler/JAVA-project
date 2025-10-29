package frontEnd;

import network.Server;

public class Launcher {
    public static void main(String[] args) {
        // Start server in a background thread
        new Thread(() -> {
            try {
                Server.main(new String[]{}); // run your existing server
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "ServerThread").start();

        // Start client (MainApp)
        MainApp.main(args);
    }
}

