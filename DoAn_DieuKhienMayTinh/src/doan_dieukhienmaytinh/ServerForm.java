/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerForm extends JFrame {
    private JTextArea logArea;
    private JButton startServerButton;
    private JLabel ipLabel;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private static final int PORT = 5000;
    private static final String PASSWORD = "123456";

    public ServerForm() {
        setTitle("Remote Desktop Server");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Khu vực log
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // Nút Start Server
        startServerButton = new JButton("Start Server");
        startServerButton.addActionListener(new StartServerAction());

        // Nhãn hiển thị IP
        ipLabel = new JLabel("IP: Chưa khởi động", SwingConstants.CENTER);

        // Sắp xếp giao diện
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(ipLabel, BorderLayout.NORTH);
        topPanel.add(startServerButton, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        executor = Executors.newFixedThreadPool(10); // Xử lý đa luồng
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerForm serverForm = new ServerForm();
            serverForm.setVisible(true);
        });
    }

    private class StartServerAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            startServerButton.setEnabled(false);
            new Thread(() -> startServer()).start();
        }
    }

    private void startServer() {
        try {
            // Lấy địa chỉ IP của máy
            String localIp = getLocalIpAddress();
            ipLabel.setText("IP: " + localIp); // Hiển thị IP lên giao diện
            logArea.append("Server đang chạy trên IP: " + localIp + ", cổng: " + PORT + "\n");

            serverSocket = new ServerSocket(PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logArea.append("Máy khách kết nối từ: " + clientSocket.getInetAddress() + "\n");
                executor.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            logArea.append("Lỗi: " + e.getMessage() + "\n");
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            // Xác thực mật khẩu
            String receivedPassword = (String) inputStream.readObject();
            if (!PASSWORD.equals(receivedPassword)) {
                logArea.append("Sai mật khẩu từ: " + clientSocket.getInetAddress() + "\n");
                outputStream.writeObject("Sai mật khẩu. Kết nối bị từ chối.");
                clientSocket.close();
                return;
            }

            logArea.append("Máy khách xác thực thành công: " + clientSocket.getInetAddress() + "\n");
            outputStream.writeObject("Kết nối thành công!");

            // Xử lý lệnh từ Client
            handleRemoteCommands(inputStream);
        } catch (Exception e) {
            logArea.append("Máy khách ngắt kết nối: " + e.getMessage() + "\n");
        }
    }

    private void handleRemoteCommands(ObjectInputStream inputStream) {
        try {
            while (true) {
                String command = (String) inputStream.readObject();
                logArea.append("Lệnh nhận được: " + command + "\n");
                // Xử lý lệnh tại đây
            }
        } catch (Exception e) {
            logArea.append("Kết nối lệnh bị ngắt: " + e.getMessage() + "\n");
        }
    }

    // Hàm lấy địa chỉ IP của máy
    private String getLocalIpAddress() {
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            return localAddress.getHostAddress();
        } catch (UnknownHostException e) {
            return "Không xác định được IP";
        }
    }
}


