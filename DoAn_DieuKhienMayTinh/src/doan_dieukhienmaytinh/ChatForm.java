package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ChatForm extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

   public ChatForm(String title, Socket chatSocket) {
    setTitle(title);
    setSize(300, 400);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout());

    chatArea = new JTextArea();
    chatArea.setEditable(false);
    JScrollPane scrollPane = new JScrollPane(chatArea);

    JPanel inputPanel = new JPanel(new BorderLayout());
    messageField = new JTextField();
    sendButton = new JButton("Send");
    inputPanel.add(messageField, BorderLayout.CENTER);
    inputPanel.add(sendButton, BorderLayout.EAST);

    add(scrollPane, BorderLayout.CENTER);
    add(inputPanel, BorderLayout.SOUTH);

    try {
        // Khởi tạo các luồng I/O từ socket chat
        outputStream = new ObjectOutputStream(chatSocket.getOutputStream());
        inputStream = new ObjectInputStream(chatSocket.getInputStream());
    } catch (IOException e) {
        showMessage("Error initializing chat: " + e.getMessage());
    }

    sendButton.addActionListener(e -> sendMessage());

    // Chỉ khởi chạy luồng nhận tin nhắn khi inputStream đã được khởi tạo
    if (inputStream != null) {
        new Thread(this::receiveMessages).start();
    }
}



    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            try {
                outputStream.writeObject("CHAT:" + message);
                chatArea.append("You: " + message + "\n");
                messageField.setText("");
            } catch (IOException e) {
                showMessage("Error sending message: " + e.getMessage());
            }
        }
    }

    private void receiveMessages() {
    try {
        if (inputStream == null) {
            throw new IOException("InputStream không được khởi tạo.");
        }

        while (true) {
            Object receivedObject = inputStream.readObject();
            if (receivedObject instanceof String && ((String) receivedObject).startsWith("CHAT:")) {
                String message = ((String) receivedObject).substring(5);
                chatArea.append("Partner: " + message + "\n");
            }
        }
    } catch (IOException | ClassNotFoundException e) {
        showMessage("Kết nối chat bị đóng hoặc lỗi: " + e.getMessage());
    }
}



    private void showMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }
}