package top.devgo.graphitej;

class GraphiteJClientException extends RuntimeException {

    GraphiteJClientException(String message) {
        super(message);
    }

    GraphiteJClientException(String message, Exception cause) {
        super(message, cause);
    }
}
