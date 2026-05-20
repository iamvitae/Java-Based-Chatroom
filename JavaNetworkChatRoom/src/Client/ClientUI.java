package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ClientUI extends JFrame {
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    private JTextField ipField;
    private JTextField portField;
    private JTextField userField;
    private JButton loginButton;
    private JButton exitAppButton;
    
    private JTextArea chatArea;
    private DefaultListModel<String> usersListModel;
    private JList<String> activeUsersList;
    private JTextField messageField;
    private JButton sendButton;
    private JButton leaveButton;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    
    private volatile boolean intentionalLogout = false;
    
    private Color pinkHue = new Color(255, 192, 203);
    private Color redText = Color.RED;

    public ClientUI() {
        setTitle("Network Chat Client");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        
        initLoginPanel();
        initChatPanel();
        
        add(cardPanel);
        cardLayout.show(cardPanel, "Login");
    }

    private void initLoginPanel() {
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(pinkHue);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        JLabel titleLabel = new JLabel("Join Chat Room");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(redText);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        JLabel ipLabel = new JLabel("Server IP:");
        ipLabel.setForeground(redText);
        loginPanel.add(ipLabel, gbc);
        
        gbc.gridx = 1;
        ipField = new JTextField("localhost", 15);
        loginPanel.add(ipField, gbc);
        
        gbc.gridy = 2; gbc.gridx = 0;
        JLabel portLabel = new JLabel("Port:");
        portLabel.setForeground(redText);
        loginPanel.add(portLabel, gbc);
        
        gbc.gridx = 1;
        portField = new JTextField("5000", 15);
        loginPanel.add(portField, gbc);
        
        gbc.gridy = 3; gbc.gridx = 0;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(redText);
        loginPanel.add(userLabel, gbc);
        
        gbc.gridx = 1;
        userField = new JTextField(15);
        loginPanel.add(userField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(pinkHue);
        loginButton = new JButton("Login");
        exitAppButton = new JButton("Exit");
        buttonPanel.add(loginButton);
        buttonPanel.add(exitAppButton);
        
        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2;
        loginPanel.add(buttonPanel, gbc);
        
        loginButton.addActionListener(e -> connectToServer());
        exitAppButton.addActionListener(e -> System.exit(0));
        
        cardPanel.add(loginPanel, "Login");
    }

    private void initChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(10, 10));
        chatPanel.setBackground(pinkHue);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        
        usersListModel = new DefaultListModel<>();
        activeUsersList = new JList<>(usersListModel);
        JScrollPane userScroll = new JScrollPane(activeUsersList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        userScroll.setBorder(BorderFactory.createTitledBorder("Active Users"));
        
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
        bottomPanel.setBackground(pinkHue);
        messageField = new JTextField();
        sendButton = new JButton("Send");
        leaveButton = new JButton("Leave Chat");
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionPanel.setBackground(pinkHue);
        actionPanel.add(sendButton);
        actionPanel.add(Box.createHorizontalStrut(5));
        actionPanel.add(leaveButton);
        
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(actionPanel, BorderLayout.EAST);
        
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(userScroll, BorderLayout.EAST);
        chatPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        
        leaveButton.addActionListener(e -> {
            intentionalLogout = true; 
            
            if (out != null) {
                out.println("LOGOUT|" + username);
            }
            
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            
            chatArea.setText("");
            cardLayout.show(cardPanel, "Login");
        });
        
        cardPanel.add(chatPanel, "Chat");
    }

    private void connectToServer() {
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();
        username = userField.getText().trim();
        
        if (ip.isEmpty() || portStr.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            int port = Integer.parseInt(portStr);
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            intentionalLogout = false;
            
            out.println("LOGIN|" + username);
            
            new Thread(new IncomingReader()).start();
            cardLayout.show(cardPanel, "Chat");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not connect to server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println("MESSAGE|" + username + "|" + message);
            messageField.setText("");
        }
    }

    private void handleDisconnect() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Disconnected from server.", "Connection Lost", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        });
    }

    private class IncomingReader implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String response = line;
                    SwingUtilities.invokeLater(() -> processResponse(response));
                }
                
                if (!intentionalLogout) {
                    handleDisconnect();
                }
                
            } catch (IOException e) {
                if (!intentionalLogout) {
                    handleDisconnect();
                }
            }
        }
        
        private void processResponse(String response) {
            String[] parts = response.split("\\|", 2);
            String command = parts[0];
            
            switch (command) {
                case "SYSTEM":
                case "CHAT":
                    chatArea.append(parts[1] + "\n");
                    break;
                case "USERS":
                    usersListModel.clear();
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        String[] users = parts[1].split(",");
                        for (String u : users) {
                            usersListModel.addElement(u);
                        }
                    }
                    break;
                case "KICK":
                    if (parts.length > 1) {
                        String kickedUser = parts[1];
                        if (kickedUser.equals(username)) {
                            JOptionPane.showMessageDialog(ClientUI.this, "You have been kicked by the administrator.", "Kicked", JOptionPane.WARNING_MESSAGE);
                            System.exit(0);
                        } else {
                            chatArea.append("[SYSTEM] User " + kickedUser + " was removed by the server.\n");
                        }
                    }
                    break;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClientUI().setVisible(true);
        });
    }
}