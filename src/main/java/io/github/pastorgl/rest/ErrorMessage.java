package io.github.pastorgl.rest;

public class ErrorMessage {
    private String error;

    private ErrorMessage() {
    }

    public ErrorMessage(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
