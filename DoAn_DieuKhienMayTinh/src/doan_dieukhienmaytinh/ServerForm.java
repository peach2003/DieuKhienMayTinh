package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ServerForm extends JFrame {
    private JTextArea logArea, chatArea;
    private JTextField chatInputField;
    private JButton sendChatButton;
    private JLabel ipLabel;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private static final int PORT = 5000;
    private static final String PASSWORD = "123456";

    public ServerForm() {
        setTitle("Remote Desktop Server");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);

        ipLabel = new JLabel("IP: Đang khởi động...", SwingConstants.CENTER);

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

        add(ipLabel, BorderLayout.NORTH);
        add(logScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        sendChatButton.addActionListener(e -> sendChat());

        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try {
                String ipAddress = InetAddress.getLocalHost().getHostAddress();
                ipLabel.setText("IP: " + ipAddress);
                logArea.append("Server đang chạy trên IP: " + ipAddress + ", cổng: " + PORT + "\n");

                serverSocket = new ServerSocket(PORT);
                while (true) {
                    clientSocket = serverSocket.accept();
                    logArea.append("Máy khách kết nối: " + clientSocket.getInetAddress() + "\n");

                    outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                    inputStream = new ObjectInputStream(clientSocket.getInputStream());

                    sendScreenSize();
                    authenticateClient();
                    sendChatButton.setEnabled(true);

                    new Thread(this::receiveChat).start();
                }
            } catch (Exception e) {
                logArea.append("Lỗi khi chạy server: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private void sendScreenSize() throws IOException {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        outputStream.writeObject(screenSize.width + "," + screenSize.height);
    }

    private void authenticateClient() throws IOException, ClassNotFoundException {
        String receivedPassword = (String) inputStream.readObject();
        logArea.append("Mật khẩu nhận được: " + receivedPassword + "\n");

        if (!PASSWORD.equals(receivedPassword)) {
            logArea.append("Xác thực thất bại\n");
            outputStream.writeObject("Xác thực thất bại");
            clientSocket.close();
            throw new SecurityException("Sai mật khẩu");
        }

        logArea.append("Xác thực thành công\n");
        outputStream.writeObject("Máy khách xác thực thành công");
    }

    private void sendChat() {
        String message = chatInputField.getText().trim();
        if (!message.isEmpty()) {
            try {
                outputStream.writeObject("chat," + message);
                chatArea.append("Server: " + message + "\n");
                chatInputField.setText("");
            } catch (IOException e) {
                logArea.append("Lỗi khi gửi tin nhắn: " + e.getMessage() + "\n");
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
                        chatArea.append("Client: " + message.substring(5) + "\n");
                    }
                }
            }
        } catch (Exception e) {
            chatArea.append("Mất kết nối với Client!\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerForm serverForm = new ServerForm();
            serverForm.setVisible(true);
        });
    }
}
