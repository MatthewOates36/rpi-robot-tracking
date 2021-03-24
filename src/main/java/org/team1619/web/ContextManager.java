package org.team1619.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ContextManager implements HttpHandler {

    private final List<Context> contexts;
    public boolean sorted;

    public ContextManager() {
        contexts = new ArrayList<>();
        sorted = false;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            var path = exchange.getRequestURI().getPath();
            var method = Method.valueOf(exchange.getRequestMethod());

            if ("/".equals(path)) {
                path = "";
            }

            var contextPath = new ContextPath(method, path);

            var context = getContext(contextPath);

            var req = new Request(exchange, context.getUriParams(contextPath));
            var res = new Response(exchange);

            context.getHandler().handle(req, res);

            res.end();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addContext(Context context) {
        sorted = false;
        contexts.add(context);
    }

    private Context getContext(ContextPath path) {

        if (!sorted) {
            contexts.sort(Comparator.comparingInt((Context context) -> context.getPath().size()).thenComparingInt(context -> -context.getPath().dynamicSize()).thenComparing(context -> context.getPath().isClosed()).thenComparing(context -> Method.ANY != context.getMethod()).reversed());
            sorted = true;
        }

        for (Context context : contexts) {
            if (context.pathEquals(path)) {
                return context;
            }
        }

        for (Context context : contexts) {
            if (context.pathAssignable(path)) {
                return context;
            }
        }

        return null;
    }

    public void all(String route, ContextHandler handler) {
        addContext(new Context(Method.ANY, route, handler));
    }

    public void get(String route, ContextHandler handler) {
        addContext(new Context(Method.GET, route, handler));
    }

    public void post(String route, ContextHandler handler) {
        addContext(new Context(Method.POST, route, handler));
    }

    public void resource(String route, String path) {
        addContext(new Context(Method.GET, route, (req, res) -> {
            serveFile(res, Paths.get(path), true);
        }));
    }

    public void resourceFolder(String route, String path) {
        addContext(new Context(Method.GET, route, (req, res) -> {
            serveFile(res, Paths.get(path, req.path().replaceFirst(route, "")), true);
        }));
    }

    public void file(String route, String path) {
        addContext(new Context(Method.GET, route, (req, res) -> serveFile(res, Paths.get(path), false)));
    }

    public void folder(String route, String path) {
        addContext(new Context(Method.GET, route, (req, res) -> serveFile(res, Paths.get(path, req.path().replaceFirst(route, "")), false)));
    }

    public void tempRedirect(String route, String redirect) {
        addContext(new Context(Method.ANY, route, (req, res) -> {
            try {
                res.header("location", new URL(redirect).toString()).status(307);
            } catch (Exception e) {
                try {
                    res.header("location", new URL("http://" + req.header("host") + redirect).toString()).status(307);
                } catch (Exception e1) {
                    throw new RuntimeException("Unable to redirect to: " + "http://" + req.header("host") + redirect);
                }
            }
        }));
    }

    public void permRedirect(String route, String redirect) {
        addContext(new Context(Method.ANY, route, (req, res) -> {
            try {
                res.header("location", new URL(redirect).toString()).status(308);
            } catch (Exception e) {
                try {
                    res.header("location", new URL("http://" + req.header("host") + redirect).toString()).status(308);
                } catch (Exception e1) {
                    throw new RuntimeException("Unable to redirect to: " + "http://" + req.header("host") + redirect);
                }
            }
        }));
    }

    private void serveFile(Response res, Path file, boolean resource) throws IOException {

        if (null != file) {
            String mime = "";
            String filePathString = file.toString().replace("\\", "/");
            if (filePathString.endsWith(".html")) {
                mime = "text/html";
            } else if (filePathString.endsWith(".js")) {
                mime = "application/javascript";
            } else if (filePathString.endsWith(".css")) {
                mime = "text/css";
            }

            res.header("Content-Type", mime);

            try {
                if (resource) {
                    res.body(getClass().getResourceAsStream(file.toString().replace("\\", "/")).readAllBytes());
                } else {
                    res.body(Files.readAllBytes(file));
                }
                res.status(200);
                return;
            } catch (Exception e) {
            }
        }

        res.body("Error 404 (Not Found)").status(404);
    }

    public enum Method {
        ANY,
        GET,
        POST
    }

    public interface ContextHandler {

        void handle(Request req, Response res) throws IOException;
    }

    public static class Request {

        private final HttpExchange exchange;
        private final Map<String, String> uriParams;
        private final Map<String, String> queryParams;

        private Request(HttpExchange exchange, Map<String, String> uriParams) {
            this.exchange = exchange;

            this.uriParams = uriParams;

            var rawQuery = rawQuery();
            var queryParams = new HashMap<String, String>();

            if(null != rawQuery) {
                for (var param : rawQuery.split("&")){
                    var parts = param.split("=", 2);
                    queryParams.put(uriDecode(parts[0]), uriDecode(parts[1]));
                }
            }
            this.queryParams = Map.copyOf(queryParams);
        }

        private static String uriDecode(String uriEncodedString) {
            try {
                return URLDecoder.decode(uriEncodedString.replace("+", "%2B"), "UTF-8")
                        .replace("%2B", "+");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }

        public URI uri() {
            return exchange.getRequestURI();
        }

        public Method method() {
            return Method.valueOf(exchange.getRequestMethod());
        }

        public String path() {
            return uri().getPath();
        }

        public Map<String, String> uriParams() {
            return uriParams;
        }

        public String uriParam(String key) {
            return uriParams().get(key);
        }

        public String query() {
            return uri().getQuery();
        }

        public String rawQuery() {
            return uri().getRawQuery();
        }

        public Map<String, String> queryParams() {
            return queryParams;
        }

        public String queryParam(String key) {
            return queryParams().get(key);
        }

        public InetSocketAddress address() {
            return exchange.getRemoteAddress();
        }

        public Headers headers() {
            return exchange.getRequestHeaders();
        }

        public String header(String key) {
            return headers().getFirst(key);
        }

        public List<String> fullHeader(String key) {
            return headers().getOrDefault(key, List.of());
        }

        public String body() {
            try {
                return new String(exchange.getRequestBody().readAllBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static class Response {

        private final HttpExchange exchange;
        private boolean headersSent;
        private byte[] body;

        private Response(HttpExchange exchange) {
            this.exchange = exchange;
            headersSent = false;
            body = null;
        }

        public Response status(int status) throws IOException {
            if (!headersSent) {
                this.exchange.sendResponseHeaders(status, 0);

                headersSent = true;

                if (null != body) {
                    body(body);
                }
            }

            return this;
        }

        public Headers headers() {
            return exchange.getResponseHeaders();
        }

        public String header(String key) {
            return headers().getFirst(key);
        }

        public List<String> fullHeader(String key) {
            return headers().getOrDefault(key, List.of());
        }

        public Response header(String key, String value) {
            if (headersSent) {
                throw new RuntimeException("Can't add headers after status(int) has been called");
            }

            headers().set(key, value);

            return this;
        }

        public Response body(byte[] body) throws IOException {
            if (!headersSent) {
                this.body = body;
            } else {
                exchange.getResponseBody().write(body);
            }

            return this;
        }

        public Response body(String body) throws IOException {
            return body(body.getBytes());
        }

        public void end() {
            exchange.close();
        }
    }

    private static class Context {

        private ContextPath path;
        private ContextHandler handler;

        public Context(Method method, String path, ContextHandler handler) {
            this.path = new ContextPath(method, path);
            this.handler = handler;
        }

        public Method getMethod() {
            return getPath().getMethod();
        }

        public ContextPath getPath() {
            return path;
        }

        public ContextHandler getHandler() {
            return handler;
        }

        public Map<String, String> getUriParams(ContextPath path) {
            var uriParams = new HashMap<String, String>();

            for (int s = 0; s < getPath().size(); s++) {
                var segment = getPath().getSegment(s).getSegment();
                if (getPath().getSegment(s).isDynamic()) {
                    uriParams.put(segment.substring(1, segment.length() - 1), path.getSegment(s).getSegment());
                }
            }

            return uriParams;
        }

        public boolean pathEquals(ContextPath path) {
            if (!methodEquals(path)) {
                return false;
            }

            if (getPath().size() < 1 && path.size() == getPath().size()) {
                return true;
            }

            if (path.size() != getPath().size()) {
                return false;
            }

            for (int s = 0; s < getPath().size(); s++) {
                if (!getPath().getSegment(s).segmentEquals(path.getSegment(s))) {
                    return false;
                }
            }

            return true;
        }

        public boolean pathAssignable(ContextPath path) {
            if (!methodEquals(path)) {
                return false;
            }

            if (getPath().size() < 1) {
                return true;
            }

            if (path.size() < getPath().size()) {
                return false;
            }

            for (int s = 0; s < getPath().size() - 1; s++) {
                if (!getPath().getSegment(s).segmentEquals(path.getSegment(s))) {
                    return false;
                }
            }

            if (getPath().getSegment(getPath().size() - 1).segmentEquals(path.getSegment(getPath().size() - 1))) {
                return true;
            }

            if (!getPath().isClosed() && getPath().getSegment(getPath().size() - 1).segmentAssignable(path.getSegment(getPath().size() - 1))) {
                return true;
            }

            return false;
        }

        public boolean methodEquals(ContextPath path) {
            return Method.ANY == getMethod() || path.getMethod() == getMethod();
        }

        @Override
        public String toString() {
            return path.toString();
        }
    }

    private static class ContextPath {

        private final String path;
        private final List<ContextPathSegment> segments;
        private final boolean closed;
        private final Method method;
        private final int dynamicSize;

        public ContextPath(Method method, String path) {
            this.method = method;
            path = path.trim();

            if (!path.isEmpty() && !path.startsWith("/")) {
                throw new ContextPathException("Non empty paths must start with /");
            }

            if (path.contains("//")) {
                throw new ContextPathException("Paths can't contain //");
            }

            this.path = path;

            closed = path.endsWith("/");

            segments = Arrays.stream(path.split("/")).filter(Predicate.not(String::isEmpty)).map(ContextPathSegment::new).collect(Collectors.toList());

            dynamicSize = Long.valueOf(segments.stream().filter(ContextPathSegment::isDynamic).count()).intValue();
        }

        public Method getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public ContextPathSegment getSegment(int segmentNumber) {
            return segments.get(segmentNumber);
        }

        public int size() {
            if (path.isEmpty()) {
                return -1;
            }
            return segments.size();
        }

        public int dynamicSize() {
            return dynamicSize;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public String toString() {
            return getMethod() + ": " + getPath();
        }
    }

    private static class ContextPathSegment {

        private final String segment;
        private final boolean isDynamic;

        public ContextPathSegment(String segment) {
            this.segment = segment;

            isDynamic = segment.startsWith("{") && segment.endsWith("}");
        }

        public String getSegment() {
            return segment;
        }

        public boolean isDynamic() {
            return isDynamic;
        }

        public boolean segmentEquals(ContextPathSegment segment) {
            if (getSegment().equals(segment.getSegment())) {
                return true;
            }

            return isDynamic();
        }

        public boolean segmentAssignable(ContextPathSegment segment) {
            if (segment.getSegment().startsWith(getSegment())) {
                return true;
            }

            return isDynamic();
        }
    }

    private static class ContextPathException extends RuntimeException {

        public ContextPathException(String message) {
            super(message);
        }
    }
}
