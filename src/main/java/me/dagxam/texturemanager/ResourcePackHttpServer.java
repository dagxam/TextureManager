package me.dagxam.texturemanager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Встроенная HTTP-раздача готового ресурс-пака с ограниченным числом потоков. */
public final class ResourcePackHttpServer {
    private static final int BUFFER_SIZE = 32 * 1024;
    private final TextureManagerPlugin plugin;
    private final Path packFile;
    private final int port;
    private HttpServer server;
    private ExecutorService executor;

    public ResourcePackHttpServer(TextureManagerPlugin plugin, Path packFile, int port) {
        this.plugin = plugin;
        this.packFile = packFile;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/TextureManager.zip", new PackHandler());
        int threads = Math.max(1, plugin.getConfig().getInt("resource-pack.встроенный-сервер.потоки", 2));
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "TextureManager-HTTP-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        executor = Executors.newFixedThreadPool(threads, factory);
        server.setExecutor(executor);
        server.start();
        plugin.getLogger().info("Встроенный HTTP-сервер ресурс-пака запущен на порту " + port + ". Потоков: " + threads + ".");
    }

    public void stop() {
        if (server != null) server.stop(0);
        if (executor != null) executor.shutdownNow();
        server = null;
        executor = null;
    }

    private final class PackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) || !Files.isRegularFile(packFile)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            long length = Files.size(packFile);
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Content-Length", Long.toString(length));
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, length);

            byte[] buffer = new byte[BUFFER_SIZE];
            try (BufferedInputStream input = new BufferedInputStream(Files.newInputStream(packFile), BUFFER_SIZE);
                 OutputStream output = exchange.getResponseBody()) {
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            } finally {
                exchange.close();
            }
        }
    }
}
