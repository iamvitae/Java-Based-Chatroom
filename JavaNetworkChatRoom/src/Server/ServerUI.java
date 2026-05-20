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
    private static JTextArea chatArea;
    private static DefaultListModel<String> usersModel;
    private JButton startBtn, stopBtn, kickBtn;
    private JTextField portField;
    
    private static final List<Socket> clientSockets = Collections.synchronizedList(new ArrayList<>());
    private static final List<PrintWriter> clientOutputs = Collections.synchronizedList(new ArrayList<>());
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    public ServerUI() {
        setTitle("Chat Server Admin");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        portField = new JTextField("5000", 5); 
        startBtn = new JButton("Start Server");
        stopBtn = new JButton("Stop Server");
        stopBtn.setEnabled(false);
        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);
        topPanel.add(startBtn);
        topPanel.add(stopBtn);
        add(topPanel, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("Chat Messages Area"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Server Log Area"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chatScroll, logScroll);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        usersModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(usersModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(BorderFactory.createTitledBorder("Active Users"));
        
        kickBtn = new JButton("Kick Selected User");
        rightPanel.add(userScroll, BorderLayout.CENTER);
        rightPanel.add(kickBtn, BorderLayout.SOUTH);
        rightPanel.setPreferredSize(new Dimension(170, 0));
        add(rightPanel, BorderLayout.EAST);

        startBtn.addActionListener(e -> {
            try {
                int port = Integer.parseInt(portField.getText().trim());
                startServer(port);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid Port Number!");
            }
        });
        
        stopBtn.addActionListener(e -> stopServer());
        
        kickBtn.addActionListener(e -> {
            String selectedUser = userList.getSelectedValue();
            if (selectedUser != null) {
                logMessage("Admin kicked user: " + selectedUser);
                broadcast("KICK|" + selectedUser);
                removeUser(selectedUser);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a user to kick.");
            }
        });

        setVisible(true);
    }

    private void startServer(int port) {
        isRunning = true;
        startBtn.setEnabled(false);
        portField.setEnabled(false);
        stopBtn.setEnabled(true);
        logMessage("Starting server...");

        new Thread(() -> {
            SharedBoundedBuffer sharedBuffer = new SharedBoundedBuffer();
            new Thread(new Dispatcher(sharedBuffer)).start();
            logMessage("Dispatcher active. Monitoring buffer.");

            try {
                serverSocket = new ServerSocket(port);
                logMessage("Listening on port " + port + "...");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    if (!isRunning) {
                        clientSocket.close();
                        break;
                    }
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    clientSockets.add(clientSocket);
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
        logMessage("Stopping server and disconnecting all users...");
        
        broadcast("SYSTEM|Server is shutting down...");
        
        synchronized (clientSockets) {
            for (Socket s : clientSockets) {
                try {
                    if (s != null && !s.isClosed()) {
                        s.close();
                    }
                } catch (Exception ex) {
                }
            }
            clientSockets.clear();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ex) {
            logMessage("Error closing listener socket.");
        }
        
        clientOutputs.clear();
        SwingUtilities.invokeLater(() -> usersModel.clear());
        logMessage("Server stopped successfully.");
        
        startBtn.setEnabled(true);
        portField.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    public static void logMessage(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }

    public static void chatMessage(String msg) {
        SwingUtilities.invokeLater(() -> chatArea.append(msg + "\n"));
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