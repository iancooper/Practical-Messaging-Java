public class SerDerException extends Exception {
    public SerDerException() {
    }

    public SerDerException(String message) {
        super(message);
    }

    public SerDerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerDerException(Throwable cause) {
        super(cause);
    }

    public SerDerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
