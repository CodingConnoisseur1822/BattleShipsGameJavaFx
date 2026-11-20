package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import javafx.application.Platform;
import network.ServerConfig;

public class ServerRequest {

    private static final int TIMEOUT_MILLISECONDS = 5000;
    private static final String MESSAGE_NO_RESPONSE = "No response from server.";
    private static final String MESSAGE_UNREACHABLE = "Could not reach the server.";

    public interface ResponseListener {
        void onResponse(String response);

        void onFailure(String errorMessage);
    }

    public static void send(final String request, final ResponseListener listener) {
        Thread requestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String response = null;
                String error = null;

                try {
                    response = exchange(request);
                    if (response == null) {
                        error = MESSAGE_NO_RESPONSE;
                    }
                } catch (IOException e) {
                    error = MESSAGE_UNREACHABLE;
                }

                deliver(listener, response, error);
            }
        });
        requestThread.setDaemon(true);
        requestThread.start();
    }

    private static String exchange(String request) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ServerConfig.HOST, ServerConfig.PORT), TIMEOUT_MILLISECONDS);
            socket.setSoTimeout(TIMEOUT_MILLISECONDS);

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.println(request);
            return reader.readLine();
        }
    }

    private static void deliver(final ResponseListener listener, final String response, final String error) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (error != null) {
                    listener.onFailure(error);
                } else {
                    listener.onResponse(response);
                }
            }
        });
    }
}
