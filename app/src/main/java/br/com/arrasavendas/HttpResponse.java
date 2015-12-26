package br.com.arrasavendas;

/**
 * Created by lsimaocosta on 27/12/15.
 */
public class HttpResponse {
    private String message;
    private int status;

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
}
