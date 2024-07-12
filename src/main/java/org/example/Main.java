package org.example;


import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    private static final int PORT = 9999;

    public static void main(String[] args) {
        Server server = new Server();
        server.addHandler("GET", "/classic.html", ((Request request, BufferedOutputStream out) -> {
            final Path filePath = Path.of(".", "public", request.getPath());//путь к файлу
            final String mimeType = Files.probeContentType(filePath);//определяем тип файла

            final String template = Files.readString(filePath);//прочитали файл
            final byte[] content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
        }));
        server.addHandler("GET", "/index.html", ((Request request, BufferedOutputStream out) -> {
            final Path filePath = Path.of(".", "public", request.getPath());
            final String mimeType = Files.probeContentType(filePath);
            final long length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        }));
        server.addHandler("GET", "/spring.png", (Request request, BufferedOutputStream out) -> {
            final Path filePath = Path.of(".", "public", request.getPath());
            final String mimeType = Files.probeContentType(filePath);
            final long length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        });

        server.addHandler("POST", "/message", (Request request, BufferedOutputStream out) -> {
            out.write(("HTTP/1.1 200 OK\r\n" +
                    "Content-Length: 43\r\n" +
                    "Connection: close\r\n" +
                    "Content-Type: text/html\r\n" +
                    "\r\n" +
                    "<html><head></head><body>POST</body></html>\n").getBytes());
            out.flush();
        });
        server.start(PORT);
    }
}