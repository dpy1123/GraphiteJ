package top.devgo.graphitej;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.*;


final class NonBlockingTcpSender {
    private final String hostname;
    private final int port;
    private final Charset encoding;
    private volatile SocketChannel clientSocket;
    private final ExecutorService executor;
    private final ScheduledThreadPoolExecutor timer;
    private final GraphiteJErrorHandler handler;

    NonBlockingTcpSender(String hostname, int port, Charset encoding, GraphiteJErrorHandler handler) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.encoding = encoding;
        this.handler = handler;

        // blocking sender
        clientSocket = SocketChannel.open();
        clientSocket.connect(new InetSocketAddress(hostname, port));

        executor = Executors.newSingleThreadExecutor(r -> {
            Thread result = Executors.defaultThreadFactory().newThread(r);
            result.setName("GraphiteJ-sender-" + result.getName());
            result.setDaemon(true);
            return result;
        });

        timer = new ScheduledThreadPoolExecutor(1, r -> {
            Thread result = Executors.defaultThreadFactory().newThread(r);
            result.setName("GraphiteJ-reconnect-"+ result.getName());
            return result;
        });
    }

    void stop() {
        try {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            handler.handle(e);
        }
        finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                }
                catch (Exception e) {
                    handler.handle(e);
                }
            }
        }
    }

    void send(final String message) {
        try {
            executor.execute(() -> blockingSend(message));
        }
        catch (Exception e) {
            handler.handle(e);
        }
    }

    private volatile boolean reconnecting = false;
    private void reconnect() {
        if (!reconnecting){
            reconnecting = true;
            doReconnect();
        }
    }
    private void doReconnect() {
        timer.schedule(() -> {
            try {
                clientSocket.close();
                clientSocket = SocketChannel.open();
                clientSocket.connect(new InetSocketAddress(hostname, port));
                reconnecting = false;
            } catch (IOException e) {
                doReconnect();
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void blockingSend(String message) {
        try {
            final byte[] sendData = message.getBytes(encoding);
            clientSocket.write(ByteBuffer.wrap(sendData));
        } catch (IOException e) {
            if ("Broken pipe".equals(e.getMessage())){
                reconnect();
            }
        } catch (Exception e) {
            handler.handle(e);
        }
    }
}