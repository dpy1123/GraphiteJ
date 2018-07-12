package top.devgo.graphitej;


import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class GraphiteJClient {

    private static final Charset GRAPHITE_J_ENCODING = Charset.forName("UTF-8");

    private static final GraphiteJErrorHandler NO_OP_HANDLER = e -> { /* No-op */ };

    private static String prefix;
    private static NonBlockingTcpSender sender;

    private static volatile boolean start = false;

    private GraphiteJClient() {}

    public static void start(String prefix, String hostname, int port) {
        start(prefix, hostname, port, NO_OP_HANDLER);
    }

    public static void start(String prefix, String hostname, int port, GraphiteJErrorHandler errorHandler) {
        if (!start) {
            GraphiteJClient.prefix = (prefix == null || prefix.trim().isEmpty()) ? "" : (prefix.trim() + ".");

            try {
                sender = new NonBlockingTcpSender(hostname, port, GRAPHITE_J_ENCODING, errorHandler);
            } catch (Exception e) {
                throw new GraphiteJClientException("Failed to start GraphiteJ client", e);
            }

            start = true;
        }
    }


    public static void stop() {
        if (start) {
            start = false;
            sender.stop();
        }
    }

    private static void check() {
        if (!start) {
            throw new GraphiteJClientException("GraphiteJ is not start!");
        }
    }

    /**
     *
     * @param aspects
     * @param value
     * @param timestamp 10位ts
     */
    public static void sendRawMetric(String[] aspects, String value, long timestamp){
        check();
        String key = prefix + String.join(".",
                Arrays.stream(aspects)
                        .map(s -> String.valueOf(s).trim().replace('.', '_').replace(':', '-'))
                        .collect(Collectors.toList()));

        sender.send(String.format("%s %s %d%n", key, value, timestamp));
    }

    public static void sendRawMetric(String[] aspects, String value){
        sendRawMetric(aspects, value, System.currentTimeMillis()/1000);
    }
}
