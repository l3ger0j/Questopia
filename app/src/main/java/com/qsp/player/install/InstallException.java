package com.qsp.player.install;

public class InstallException extends RuntimeException {

    public InstallException(String message) {
        super(message);
    }

    public InstallException(String message, Throwable cause) {
        super(message, cause);
    }
}
