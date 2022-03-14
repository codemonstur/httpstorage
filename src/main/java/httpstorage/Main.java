package httpstorage;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.BlockingHandler;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import static io.undertow.UndertowOptions.ALWAYS_SET_KEEP_ALIVE;
import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static io.undertow.util.StatusCodes.METHOD_NOT_ALLOWED;

public enum Main {;

    public static void main(final String... args) {
        System.setProperty("org.jboss.logging.provider", "slf4j");

        final HttpHandler download = newDownloadHandler();
        final HttpHandler upload = newUploadHandler();
        final HttpHandler forbidden = exchange -> exchange.setStatusCode(METHOD_NOT_ALLOWED);

        newHttpServer("0.0.0.0", 8080, exchange -> {
            final var method = exchange.getRequestMethod();
            final var handler = switch (method.toString()) {
                case "PUT" -> upload;
                case "GET" -> download;
                default -> forbidden;
            };
            handler.handleRequest(exchange);
        }).start();
    }

    private static Undertow newHttpServer(final String address, final int port, final HttpHandler handler) {
        return Undertow.builder()
            .setServerOption(ALWAYS_SET_KEEP_ALIVE, true)
            .setServerOption(ENABLE_HTTP2, true)
            .addHttpListener(port, address)
            .setHandler(handler)
            .build();
    }

    private static HttpHandler newUploadHandler() {
        return new BlockingHandler(exchange -> {
            try (final var out = new FileOutputStream(exchange.getRequestPath())) {
                exchange.getInputStream().transferTo(out);
            }
        });
    }

    private static HttpHandler newDownloadHandler() {
        return new BlockingHandler(exchange -> {
            try (final var in = new FileInputStream(exchange.getRequestPath())) {
                in.transferTo(exchange.getOutputStream());
            }
        });
    }

}
