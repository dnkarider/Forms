package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.*;



public class Server {
    private final int THREADS = 64;
    final ExecutorService threadPool = Executors.newFixedThreadPool(THREADS);;
    private final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();
    final List<String> allowedMethods = List.of("GET", "POST");
    
    public void start(int port) {
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                final Socket socket = serverSocket.accept();
                threadPool.submit(() -> requestProcess(socket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void requestProcess(Socket socket) {
        try (final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());){
            final Request request = parse(socket);
            if (!handlers.containsKey(request.getMethod())) {
                notFound(out);
            }
            if (!handlers.get(request.getMethod()).containsKey(request.getPath())) {
                notFound(out);
            }
            handlers.get(request.getMethod()).get(request.getPath()).handle(request, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addHandler(String method, String path, Handler handler){
        if (!handlers.containsKey(method)) {
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);
    }

    private Request parse(Socket socket) throws IOException {
        final BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
        final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        final int limit = 4096;
        in.mark(limit);
        final byte[] buffer = new byte[limit];
        final var read = in.read(buffer);

        final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
        final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            badRequest(out);
            socket.close();
        }
        final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest(out);
            socket.close();
        }
        final String method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            badRequest(out);
            socket.close();
        }
        final String path = requestLine[1];
        if (!path.startsWith("/")) {
            badRequest(out);
            socket.close();
        }
        final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final int headersStart = requestLineEnd + requestLineDelimiter.length;
        final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(out);
            socket.close();
        }
        in.reset();
        in.skip(headersStart);

        final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
        final List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));

        String body = "";
        if (!method.equals("GET")) {
            in.skip(headersDelimiter.length);
            final Optional<String> contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final int length = Integer.parseInt(contentLength.get());
                final byte[] bodyBytes = in.readNBytes(length);
                body = new String(bodyBytes);
            }
        }
        return new Request(method, path, headers, body);
    }
    private int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
    private void notFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}