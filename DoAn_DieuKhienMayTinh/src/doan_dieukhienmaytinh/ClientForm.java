package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ClientForm extends JFrame {
    private JTextField serverIpField, chatInputField;
    private JPasswordField passwordField;
    private JButton connectButton, sendFileButton, sendChatButton;
    private JLabel screenLabel;
    private JTextArea chatArea;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private int screenWidthServer;
    private int screenHeightServer;

    public ClientForm() {
        setTitle("Remote Desktop Client");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(1, 3));
        serverIpField = new JTextField("Nhập IP Server");
        passwordField = new JPasswordField("123456");
        connectButton = new JButton("Connect");
        sendFileButton = new JButton("Gửi File");
        sendFileButton.setEnabled(false);
        topPanel.add(serverIpField);
        topPanel.add(passwordField);
        topPanel.add(connectButton);
        topPanel.add(sendFileButton);

        screenLabel = new JLabel();
        screenLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        chatInputField = new JTextField();
        sendChatButton = new JButton("Send");
        sendChatButton.setEnabled(false);

        bottomPanel.add(chatScrollPane, BorderLayout.CENTER);
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInputPanel.add(chatInputField, BorderLayout.CENTER);
        chatInputPanel.add(sendChatButton, BorderLayout.EAST);
        bottomPanel.add(chatInputPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(screenLabel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        connectButton.addActionListener(e -> {
            if ("Connect".equals(connectButton.getText())) {
                connectToServer();
            } else {
                disconnectFromServer();
            }
        });

        sendFileButton.addActionListener(e -> sendFile());
        sendChatButton.addActionListener(e -> sendChat());
    }

    private void connectToServer() {
        String serverIp = serverIpField.getText();
        String password = new String(passwordField.getPassword());

        try {
            socket = new Socket(serverIp, 5000);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            String[] screenSize = ((String) inputStream.readObject()).split(",");
            screenWidthServer = Integer.parseInt(screenSize[0]);
            screenHeightServer = Integer.parseInt(screenSize[1]);

            outputStream.writeObject(password);

            String response = (String) inputStream.readObject();
            if (!response.equals("Máy khách xác thực thành công")) {
                JOptionPane.showMessageDialog(this, "Xác thực thất bại!");
                socket.close();
                return;
            }

            JOptionPane.showMessageDialog(this, "Kết nối thành công!");
            connectButton.setText("Disconnect");
            sendFileButton.setEnabled(true);
            sendChatButton.setEnabled(true);

            new Thread(this::receiveScreen).start();
            new Thread(this::receiveChat).start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + e.getMessage());
        }
    }

    private void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                outputStream.writeObject("disconnect");
                socket.close();
            }
            JOptionPane.showMessageDialog(this, "Đã ngắt kết nối.");
            connectButton.setText("Connect");
            sendFileButton.setEnabled(false);
            sendChatButton.setEnabled(false);
            screenLabel.setIcon(null);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                outputStream.writeObject("file");
                outputStream.writeObject(file.getName());

                byte[] fileBytes = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.read(fileBytes);
                }
                outputStream.writeObject(fileBytes);
                JOptionPane.showMessageDialog(this, "Đã gửi file: " + file.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi gửi file: " + e.getMessage());
            }
        }
    }

    private void sendChat() {
        String message = chatInputField.getText().trim();
        if (!message.isEmpty()) {
            try {
                outputStream.writeObject("chat," + message);
                chatArea.append("Client: " + message + "\n");
                chatInputField.setText("");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi gửi tin nhắn: " + e.getMessage());
            }
        }
    }

    private void receiveChat() {
        try {
            while (true) {
                Object receivedObject = inputStream.readObject();
                if (receivedObject instanceof String) {
                    String message = (String) receivedObject;
                    if (message.startsWith("chat,")) {
                        chatArea.append("Server: " + message.substring(5) + "\n");
                    }
                }
            }
        } catch (Exception e) {
            chatArea.append("Mất kết nối với Server!\n");
            disconnectFromServer();
        }
    }

    private void receiveScreen() {
        // Existing implementation for receiving screen updates
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientForm clientForm = new ClientForm();
            clientForm.setVisible(true);
        });
    }
}
