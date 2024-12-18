package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;

public class ServerForm extends JFrame {
    private JTextArea logArea;
    private JLabel ipLabel, passLabel;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private static final int PORT = 5000;
    private String PASSWORD;

    public ServerForm() {
        setTitle("Remote Desktop Server");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Log Area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // IP and Password Labels
        ipLabel = new JLabel("Mã IP: Đang khởi động...", SwingConstants.CENTER);
        passLabel = new JLabel("Mật khẩu: Đang tạo...", SwingConstants.CENTER);

        add(ipLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(passLabel, BorderLayout.SOUTH);

        // Tự động khởi động Server
        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try {
                String ipAddress = InetAddress.getLocalHost().getHostAddress();
                ipLabel.setText("IP: " + ipAddress);
                PASSWORD = generatePassword();
                passLabel.setText("Mật khẩu: " + PASSWORD);
                logArea.append("Server đang chạy trên IP: " + ipAddress + ", cổng: " + PORT + "\n");

                serverSocket = new ServerSocket(PORT);
                clientSocket = serverSocket.accept();
                logArea.append("Máy khách kết nối: " + clientSocket.getInetAddress() + "\n");

                outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                inputStream = new ObjectInputStream(clientSocket.getInputStream());

                authenticateClient();
                new Thread(this::sendScreenToClient).start(); // Gửi màn hình liên tục
            } catch (Exception e) {
                logArea.append("Lỗi khi chạy server: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private String generatePassword() {
        return String.valueOf((int) (Math.random() * 90000 + 10000)); // Mật khẩu ngẫu nhiên 5 chữ số
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

    private void sendScreenToClient() {
        try {
            Robot robot = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            while (true) {
                BufferedImage screenshot = robot.createScreenCapture(new Rectangle(screenSize));
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(screenshot, "jpeg", byteArrayOutputStream); // Nén ảnh
                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                outputStream.writeObject(imageBytes);
                outputStream.flush();
                Thread.sleep(100); // Giới hạn tốc độ gửi
            }
        } catch (Exception e) {
            logArea.append("Lỗi khi gửi màn hình: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerForm serverForm = new ServerForm();
            serverForm.setVisible(true);
        });
    }
}
