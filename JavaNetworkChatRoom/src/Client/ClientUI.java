package client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    private JTextField ipField;
    private JTextField portField;
    private JTextField usernameField;
    private JButton loginBtn;
    private JButton exitBtn;

    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendBtn;
    private JButton leaveChatBtn;
    private DefaultListModel<String> usersModel;
    private JList<String> usersList;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    
    private final Color pinkBackground = new Color(255, 220, 230);

    public ClientUI() {
        setTitle("Chat Client");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(pinkBackground);

        initLoginPanel();
        initChatPanel();

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
        setVisible(true);
    }

    private void initLoginPanel() {
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(pinkBackground);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        ipField = new JTextField("localhost", 15);
        portField = new JTextField("5000", 5);
        usernameField = new JTextField(15);
        
        loginBtn = new JButton("Login");
        loginBtn.setForeground(Color.RED);
        
        exitBtn = new JButton("Exit");
        exitBtn.setForeground(Color.RED);

        gbc.gridx = 0; gbc.gridy = 0; loginPanel.add(new JLabel("Server IP:"), gbc);
        gbc.gridx = 1; loginPanel.add(ipField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; loginPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1; loginPanel.add(portField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; loginPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; loginPanel.add(usernameField, gbc);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        btnPanel.setBackground(pinkBackground);
        btnPanel.add(loginBtn);
        btnPanel.add(exitBtn);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        loginPanel.add(btnPanel, gbc);

        loginBtn.addActionListener(e -> connectToServer());
        exitBtn.addActionListener(e -> System.exit(0));
        
        mainPanel.add(loginPanel, "LOGIN");
    }

    private void initChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(pinkBackground);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(pinkBackground);
        chatArea.setForeground(Color.BLACK);
        
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("Chat Room"));
        chatScroll.setBackground(pinkBackground);
        chatScroll.getViewport().setBackground(pinkBackground);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(pinkBackground);
        messageField = new JTextField();
        
        JPanel actionsPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        actionsPanel.setBackground(pinkBackground);
        
        sendBtn = new JButton("Send");
        sendBtn.setForeground(Color.RED);
        
        leaveChatBtn = new JButton("Leave Chat");
        leaveChatBtn.setForeground(Color.RED);
        
        actionsPanel.add(sendBtn);
        actionsPanel.add(leaveChatBtn);
        
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(actionsPanel, BorderLayout.EAST);
        chatPanel.add(bottomPanel, BorderLayout.SOUTH);

        usersModel = new DefaultListModel<>();
        usersList = new JList<>(usersModel);
        usersList.setBackground(pinkBackground);
        usersList.setForeground(Color.RED); 
        
        JScrollPane userScroll = new JScrollPane(usersList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        userScroll.setBorder(BorderFactory.createTitledBorder("Online Users"));
        userScroll.setBackground(pinkBackground);
        userScroll.getViewport().setBackground(pinkBackground);
        chatPanel.add(userScroll, BorderLayout.EAST);

        sendBtn.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        
        leaveChatBtn.addActionListener(e -> {
            if (out != null) {
                out.println("LOGOUT|" + username);
            }
            handleDisconnect();
        });

        mainPanel.add(chatPanel, "CHAT");
    }

    private void connectToServer() {
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();
        username = usernameField.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty.");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("LOGIN|" + username);

            cardLayout.show(mainPanel, "CHAT");
            setTitle("Chat Client - " + username);

            new Thread(new IncomingReader()).start();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not connect to server: " + ex.getMessage());
        }
    }

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (!msg.isEmpty()) {
            out.println("MESSAGE|" + username + "|" + msg);
            messageField.setText("");
        }
    }

    private void handleDisconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(() -> {
            usersModel.clear();
            chatArea.setText("");
            setTitle("Chat Client");
            cardLayout.show(mainPanel, "LOGIN");
        });
    }

    private class IncomingReader implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    String command = parts[0];

                    switch (command) {
                        case "CHAT":
                            chatArea.append(parts[1] + "\n");
                            break;
                        case "SYSTEM":
                            chatArea.append("[SYSTEM] " + parts[1] + "\n");
                            break;
                        case "USERS":
                            SwingUtilities.invokeLater(() -> {
                                usersModel.clear();
                                if (parts.length > 1) {
                                    String[] names = parts[1].split(",");
                                    for (String name : names) {
                                        usersModel.addElement(name);
                                    }
                                }
                            });
                            break;
                        case "KICK":
                            String kickedUser = parts[1];
                            if (kickedUser.equals(username)) {
                                JOptionPane.showMessageDialog(ClientUI.this, "You have been kicked by the Admin.");
                                handleDisconnect();
                                return;
                            } else {
                                chatArea.append("[SYSTEM] " + kickedUser + " was kicked from the server.\n");
                            }
                            break;
                    }
                }
            } catch (Exception e) {
            } finally {
                JOptionPane.showMessageDialog(ClientUI.this, "Connection to server lost. The application will now close.", "Server Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientUI::new);
    }
}