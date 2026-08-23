package me.dagxam.texturemanager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Встроенная HTTP-раздача готового ресурс-пака. */
public final class ResourcePackHttpServer {
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
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();
        plugin.getLogger().info("Встроенный HTTP-сервер ресурс-пака запущен на порту " + port + ".");
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
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) || !Files.exists(packFile)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            long length = Files.size(packFile);
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, length);
            try (OutputStream output = exchange.getResponseBody()) {
                Files.copy(packFile, output);
            }
        }
    }
}
