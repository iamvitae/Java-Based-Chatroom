package server;

public class Worker implements Runnable {
    private final String requestData;

    public Worker(String requestData) {
        this.requestData = requestData;
    }

    @Override
    public void run() {
        try {
            String[] parts = requestData.split("\\|");
            String command = parts[0];
            String username = parts.length > 1 ? parts[1] : "Unknown";

            switch (command) {
                case "LOGIN":
                    ServerUI.logMessage("User joined: " + username);
                    ServerUI.broadcast("SYSTEM|" + username + " has joined the chat");
                    ServerUI.addUser(username); 
                    break;
                case "MESSAGE":
                    String chatMsg = parts[2];
                    ServerUI.chatMessage(username + ": " + chatMsg);
                    ServerUI.broadcast("CHAT|" + username + ": " + chatMsg);
                    break;
                case "LOGOUT":
                    ServerUI.logMessage("User left: " + username);
                    ServerUI.broadcast("SYSTEM|" + username + " has left the chat");
                    ServerUI.removeUser(username); 
                    break;
                default:
                    ServerUI.logMessage("Unknown command: " + command);
            }
        } catch (Exception e) {
            ServerUI.logMessage("Error processing request: " + requestData);
        }
    }
}