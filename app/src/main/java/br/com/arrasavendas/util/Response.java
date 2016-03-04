package br.com.arrasavendas.util;

import java.net.HttpURLConnection;

/**
 * Created by lsimaocosta on 27/12/15.
 */
public class Response {
    private String message;
    private int status;

    public Response() {
    }

    public Response(String message, int status) {
        this.message = message;
        this.status = status;
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


    @Override
    public String toString() {
        return "Response{" +
                "message='" + message + '\'' +
                ", status=" + status +
                '}';
    }
}
