package client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientUI extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    
    // Login Panel Components
    private JTextField ipField = new JTextField("localhost", 15);
    private JTextField portField = new JTextField("5000", 5);
    private JTextField usernameField = new JTextField(15);
    
    // Chat Panel Components
    private JTextArea chatArea = new JTextArea();
    private DefaultListModel<String> activeUsersModel = new DefaultListModel<>();
    private JTextField messageInput = new JTextField();
    
    // Networking
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientUI() {
        setTitle("Java Network Chat Room");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createChatPanel(), "CHAT");
        add(mainPanel);
        
        cardLayout.show(mainPanel, "LOGIN");
        setVisible(true);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0;
        
        panel.add(new JLabel("Server IP:"), gbc);
        gbc.gridx = 1; panel.add(ipField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1; panel.add(portField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; panel.add(usernameField, gbc);
        
        JButton loginBtn = new JButton("Login");
        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(loginBtn, gbc);
        
        loginBtn.addActionListener(e -> connectToServer());
        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        chatArea.setEditable(false);
        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        
        JList<String> userList = new JList<>(activeUsersModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        userScroll.setBorder(BorderFactory.createTitledBorder("Active Users"));
        panel.add(userScroll, BorderLayout.EAST);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JButton sendBtn = new JButton("Send");
        JButton exitBtn = new JButton("Exit");
        
        bottomPanel.add(messageInput, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendBtn);
        buttonPanel.add(exitBtn);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        sendBtn.addActionListener(e -> sendMessage());
        messageInput.addActionListener(e -> sendMessage()); // Allow pressing Enter
        exitBtn.addActionListener(e -> disconnect());
        
        return panel;
    }

    private void connectToServer() {
        try {
            String ip = ipField.getText();
            int port = Integer.parseInt(portField.getText());
            username = usernameField.getText().trim();
            
            if(username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username cannot be empty.");
                return;
            }

            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send login request to the server buffer
            out.println("LOGIN|" + username);
            
            // Switch UI
            cardLayout.show(mainPanel, "CHAT");
            setTitle("Chat Room - " + username);
            
            // Start listening for server broadcasts
            new Thread(new ServerListener()).start();
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not connect to server.");
        }
    }

    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (!text.isEmpty()) {
            out.println("MESSAGE|" + username + "|" + text);
            messageInput.setText("");
        }
    }

    private void disconnect() {
        try {
            if (out != null) {
                out.println("LOGOUT|" + username);
            }
            if (socket != null) {
                socket.close();
            }
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Background thread to listen to the server
    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    String[] parts = serverMessage.split("\\|");
                    String type = parts[0];
                    
                    if (type.equals("SYSTEM") || type.equals("CHAT")) {
                        String msg = parts[1];
                        SwingUtilities.invokeLater(() -> chatArea.append(msg + "\n"));
                    } 
                    else if (type.equals("USERS")) {
                        // Update active users list
                        SwingUtilities.invokeLater(() -> {
                            activeUsersModel.clear();
                            if (parts.length > 1) {
                                String[] users = parts[1].split(",");
                                for (String u : users) {
                                    activeUsersModel.addElement(u);
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> chatArea.append("Disconnected from server.\n"));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientUI::new);
    }
}