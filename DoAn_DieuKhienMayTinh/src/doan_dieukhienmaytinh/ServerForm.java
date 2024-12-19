package doan_dieukhienmaytinh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;

public class ServerForm extends JFrame {
    private JTextArea logArea;
    private JLabel ipLabel;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private static final int PORT = 5000;
    private static final String PASSWORD = "123456";

    public ServerForm() {
        setTitle("Remote Desktop Server");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Log Area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // IP Label
        ipLabel = new JLabel("IP: Đang khởi động...", SwingConstants.CENTER);

        add(ipLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Tự động khởi động Server
        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try {
                String ipAddress = InetAddress.getLocalHost().getHostAddress();
                ipLabel.setText("IP: " + ipAddress);
                logArea.append("Server đang chạy trên IP: " + ipAddress + ", cổng: " + PORT + "\n");

                serverSocket = new ServerSocket(PORT);
                clientSocket = serverSocket.accept();
                logArea.append("Máy khách kết nối: " + clientSocket.getInetAddress() + "\n");

                outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                inputStream = new ObjectInputStream(clientSocket.getInputStream());

                authenticateClient();
                new Thread(this::sendScreenToClient).start(); // Gửi màn hình liên tục
                handleClientCommands();
            } catch (Exception e) {
                logArea.append("Lỗi khi chạy server: " + e.getMessage() + "\n");
            }
        }).start();
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
                Thread.sleep(100); // Điều chỉnh tốc độ truyền
            }
        } catch (Exception e) {
            logArea.append("Lỗi khi gửi màn hình: " + e.getMessage() + "\n");
        }
    }

    private void handleClientCommands() {
        try {
            Robot robot = new Robot();

            while (true) {
                String command = (String) inputStream.readObject();
                if (command.startsWith("mouse")) {
                    String[] parts = command.split(",");
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    robot.mouseMove(x, y);
                } else if (command.equals("click")) {
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                } else if (command.startsWith("key")) {
                    int keyCode = Integer.parseInt(command.split(",")[1]);
                    robot.keyPress(keyCode);
                    robot.keyRelease(keyCode);
                }else if (command.equals("sendfile")) {
                receiveFileFromClient();
            }

            }
        } catch (Exception e) {
            logArea.append("Lỗi khi xử lý lệnh từ client: " + e.getMessage() + "\n");
        }
    }
    private void receiveFileFromClient() {
    try {
        // Nhận tên file
        String fileName = (String) inputStream.readObject();

        // Nhận kích thước file
        long fileSize = inputStream.readLong();

        // Nhận dữ liệu file
        byte[] buffer = new byte[1024];
        try (FileOutputStream fos = new FileOutputStream("received_" + fileName)) {
            long bytesReceived = 0;
            while (bytesReceived < fileSize) {
                int bytesRead = inputStream.read(buffer);
                fos.write(buffer, 0, bytesRead);
                bytesReceived += bytesRead;
            }
        }

        logArea.append("Đã nhận file: " + fileName + "\n");
    } catch (Exception e) {
        logArea.append("Lỗi khi nhận file: " + e.getMessage() + "\n");
    }
}

    private void sendFileToClient(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                outputStream.writeObject("ERROR: File không tồn tại");
                return;
            }

            // Gửi file
            byte[] fileBytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(fileBytes);
            }

            // Gửi tên file và nội dung file
            outputStream.writeObject("FILE_START");
            outputStream.writeObject(file.getName()); // Gửi tên file
            outputStream.writeObject(fileBytes);     // Gửi dữ liệu file
            outputStream.writeObject("FILE_END");

            logArea.append("Đã gửi file: " + fileName + " đến máy khách.\n");
        } catch (Exception e) {
            logArea.append("Lỗi khi gửi file: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerForm serverForm = new ServerForm();
            serverForm.setVisible(true);
        });
    }
}
