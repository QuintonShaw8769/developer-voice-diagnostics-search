package dev.infrai.voicesearch.infrai;

public final class InfraiApiException extends RuntimeException {
    private final String code;
    private final int status;

    public InfraiApiException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public int status() { return status; }
}
