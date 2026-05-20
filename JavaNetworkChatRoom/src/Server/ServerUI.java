package server;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerUI extends JFrame {
    private static JTextArea logArea;
    private static DefaultListModel<String> usersModel;
    private JButton startBtn, stopBtn;
    
    private static final List<PrintWriter> clientOutputs = Collections.synchronizedList(new ArrayList<>());
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    public ServerUI() {
        setTitle("Chat Server Admin");
        setSize(550, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        startBtn = new JButton("Start Server");
        stopBtn = new JButton("Stop Server");
        stopBtn.setEnabled(false);
        topPanel.add(new JLabel("Port: 5000"));
        topPanel.add(startBtn);
        topPanel.add(stopBtn);
        add(topPanel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        usersModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(usersModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        userScroll.setBorder(BorderFactory.createTitledBorder("Active Users"));
        add(userScroll, BorderLayout.EAST);

        startBtn.addActionListener(e -> startServer());
        stopBtn.addActionListener(e -> stopServer());

        setVisible(true);
    }

    private void startServer() {
        isRunning = true;
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        logMessage("Starting server...");

        new Thread(() -> {
            SharedBoundedBuffer sharedBuffer = new SharedBoundedBuffer();
            
            new Thread(new Dispatcher(sharedBuffer)).start();
            logMessage("Dispatcher active. Monitoring buffer.");

            try {
                serverSocket = new ServerSocket(5000);
                logMessage("Listening on port 5000...");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    clientOutputs.add(out);
                    
                    new Thread(new ClientHandler(clientSocket, sharedBuffer)).start();
                }
            } catch (Exception ex) {
                if (isRunning) logMessage("Server error: " + ex.getMessage());
            }
        }).start();
    }

    private void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
            clientOutputs.clear();
            usersModel.clear();
            logMessage("Server stopped.");
        } catch (Exception ex) {
            logMessage("Error stopping server.");
        }
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    public static void logMessage(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }

    public static void addUser(String username) {
        SwingUtilities.invokeLater(() -> {
            if (!usersModel.contains(username)) {
                usersModel.addElement(username);
            }
            broadcastActiveUsers(); 
        });
    }

    public static void removeUser(String username) {
        SwingUtilities.invokeLater(() -> {
            usersModel.removeElement(username);
            broadcastActiveUsers(); 
        });
    }

    public static void broadcast(String message) {
        synchronized (clientOutputs) {
            for (PrintWriter out : clientOutputs) {
                out.println(message);
            }
        }
    }
    
    public static void broadcastActiveUsers() {
        synchronized (clientOutputs) {
            java.util.StringJoiner userListStr = new java.util.StringJoiner(",");
            for(int i = 0; i < usersModel.size(); i++) {
                userListStr.add(usersModel.getElementAt(i));
            }
            for (PrintWriter out : clientOutputs) {
                out.println("USERS|" + userListStr.toString());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerUI::new);
    }
}