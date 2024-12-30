package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ClientForm extends JFrame {
    private JTextField serverIpField;
    private JPasswordField passwordField;
    private JButton connectButton, sendFileButton;
    private JLabel screenLabel;
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

        add(topPanel, BorderLayout.NORTH);
        add(screenLabel, BorderLayout.CENTER);

        connectButton.addActionListener(e -> {
            if ("Connect".equals(connectButton.getText())) {
                connectToServer();
            } else {
                disconnectFromServer();
            }
        });

        sendFileButton.addActionListener(e -> sendFile());
    }

    private void connectToServer() {
    String serverIp = serverIpField.getText();
    String password = new String(passwordField.getPassword());

    try {
        // Kết nối chính (chia sẻ màn hình và gửi file)
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

        // Kết nối phụ (chat)
        Socket chatSocket = new Socket(serverIp, 5001);
        if (chatSocket != null) {
            JFrame chatForm = new ChatForm("Client Chat", chatSocket);
            SwingUtilities.invokeLater(() -> chatForm.setVisible(true));
        } else {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến socket chat.");
        }

        // Bắt đầu nhận chia sẻ màn hình
        new Thread(this::receiveScreen).start();
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
                outputStream.writeObject("file"); // Gửi tín hiệu gửi file
                outputStream.writeObject(file.getName()); // Gửi tên file

                byte[] fileBytes = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.read(fileBytes);
                }
                outputStream.writeObject(fileBytes); // Gửi nội dung file
                JOptionPane.showMessageDialog(this, "Đã gửi file: " + file.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi gửi file: " + e.getMessage());
            }
        }
    }

    private void receiveScreen() {
        try {
            while (true) {
                byte[] imageBytes = (byte[]) inputStream.readObject();
                ImageIcon icon = new ImageIcon(imageBytes);

                int width = screenLabel.getWidth();
                int height = screenLabel.getHeight();

                Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                screenLabel.setIcon(new ImageIcon(scaledImage));
                screenLabel.repaint();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Mất kết nối với Server!");
            disconnectFromServer();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientForm clientForm = new ClientForm();
            clientForm.setVisible(true);
        });
    }
}
