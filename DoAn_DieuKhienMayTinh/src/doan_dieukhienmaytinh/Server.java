/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;

public class Server extends JFrame {
    private JTextArea logArea;
    private JLabel ipLabel;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private static final int PORT = 5000;
    private static final String PASSWORD = "123456"; // Mật khẩu xác thực

    public Server() {
        setTitle("Remote Desktop Server");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        ipLabel = new JLabel("IP Public: Đang khởi động...", SwingConstants.CENTER);

        add(ipLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try {
                // Lấy IP Public
                String publicIp = getPublicIp();
                ipLabel.setText("IP Public (dùng Client): " + publicIp);
                logArea.append("Server đang chạy trên IP Public: " + publicIp + ", cổng: " + PORT + "\n");

                serverSocket = new ServerSocket(PORT);
                clientSocket = serverSocket.accept();
                logArea.append("Máy khách kết nối từ IP: " + clientSocket.getInetAddress() + "\n");

                outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                inputStream = new ObjectInputStream(clientSocket.getInputStream());

                authenticateClient();
                new Thread(this::sendScreenToClient).start(); // Gửi màn hình liên tục
            } catch (Exception e) {
                logArea.append("Lỗi khi chạy server: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private String getPublicIp() {
        try {
            URL url = new URL("http://checkip.amazonaws.com");
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            return br.readLine().trim();
        } catch (IOException e) {
            logArea.append("Không thể lấy IP Public: " + e.getMessage() + "\n");
            return "Không thể lấy IP Public";
        }
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
                ImageIO.write(screenshot, "jpg", byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                outputStream.writeObject(imageBytes);
                outputStream.flush();
                Thread.sleep(100); // Giới hạn tốc độ truyền
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