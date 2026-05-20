package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final SharedBoundedBuffer sharedBuffer;

    public ClientHandler(Socket socket, SharedBoundedBuffer buffer) {
        this.clientSocket = socket;
        this.sharedBuffer = buffer;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String incomingRequest;
            while ((incomingRequest = in.readLine()) != null) {
                sharedBuffer.insert(incomingRequest);
            }
        } catch (Exception e) {
            ServerUI.logMessage("A client connection was dropped.");
        }
    }
}