package top.devgo.graphitej;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.*;


final class NonBlockingTcpSender {
    private final Charset encoding;
    private volatile SocketChannel clientSocket;
    private final ExecutorService executor;
    private final ScheduledExecutorService connectionChecker;
    private final GraphiteJErrorHandler handler;
    private static final int MAX_CAPACITY = 4096;
    private final LinkedBlockingQueue<String> pendingList = new LinkedBlockingQueue<>(MAX_CAPACITY);

    NonBlockingTcpSender(String hostname, int port, Charset encoding, GraphiteJErrorHandler handler) throws IOException {
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

        connectionChecker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread result = Executors.defaultThreadFactory().newThread(r);
            result.setName("GraphiteJ-connectionChecker-"+ result.getName());
            return result;
        });

        connectionChecker.scheduleAtFixedRate(() -> {
            try {
                clientSocket.socket().sendUrgentData(0xFF);
            } catch (IOException e) {
                try {
                    clientSocket.close();
                    clientSocket = SocketChannel.open();
                    clientSocket.connect(new InetSocketAddress(hostname, port));
                } catch (IOException e1) {
                    //NO-OP
                }
                while (!pendingList.isEmpty()) {
                    send(pendingList.poll());
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    void stop() {
        try {
            executor.shutdown();
            connectionChecker.shutdown();
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
        if (clientSocket.isConnected()) {
            executor.execute(() -> blockingSend(message));
        } else {
            while (pendingList.size() >= MAX_CAPACITY)
                pendingList.poll();
            pendingList.add(message);
        }
    }

    private void blockingSend(String message) {
        try {
            final byte[] sendData = message.getBytes(encoding);
            clientSocket.write(ByteBuffer.wrap(sendData));
        } catch (Exception e) {
            handler.handle(e);
        }
    }
}