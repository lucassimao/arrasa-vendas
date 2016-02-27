package br.com.arrasavendas.util;

import java.net.HttpURLConnection;

/**
 * Created by lsimaocosta on 27/12/15.
 */
public class Response {
    private String message;
    private int status;
    private long lastModified;

    public Response() {
        this("",-1,0);
    }

    public Response(String message, int status) {
        this(message,status,0);
    }

    public Response(String message, int status, long lastModified) {
        this.message = message;
        this.status = status;
        this.lastModified = lastModified;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "Response{" +
                "message='" + message + '\'' +
                ", status=" + status +
                ", lastModified=" + lastModified +
                '}';
    }
}
