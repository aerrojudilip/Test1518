import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class SimpleHttpsProxy {

    public static void main(String[] args) throws Exception {
        int localPort = 8888; // Proxy listens on this port
        ServerSocket serverSocket = new ServerSocket(localPort);
        System.out.println("HTTPS Proxy started on port " + localPort);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            InputStream clientIn = clientSocket.getInputStream();
            OutputStream clientOut = clientSocket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientIn));
        ) {
            String requestLine = reader.readLine();
            if (requestLine == null || !requestLine.startsWith("CONNECT")) {
                clientSocket.close();
                return;
            }

            // Example: CONNECT www.google.com:443 HTTP/1.1
            String[] parts = requestLine.split(" ");
            String[] hostPort = parts[1].split(":");
            String host = hostPort[0];
            int port = Integer.parseInt(hostPort[1]);

            System.out.println("Connecting to " + host + ":" + port);

            // Respond OK to initiate TLS handshake with client
            clientOut.write("HTTP/1.1 200 Connection established\r\n\r\n".getBytes());
            clientOut.flush();

            // Upgrade client socket to SSL
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket serverSocket = (SSLSocket) factory.createSocket(host, port);
            serverSocket.startHandshake();

            // Tunnel data between client and server
            InputStream serverIn = serverSocket.getInputStream();
            OutputStream serverOut = serverSocket.getOutputStream();

            // Start data piping in both directions
            new Thread(() -> pipe(clientIn, serverOut)).start();
            pipe(serverIn, clientOut); // Main thread handles server-to-client
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void pipe(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
                out.flush();
            }
        } catch (IOException e) {
            // Connection closed
        }
    }
}
