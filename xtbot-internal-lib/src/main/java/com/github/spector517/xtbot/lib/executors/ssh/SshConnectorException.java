package com.github.spector517.xtbot.lib.executors.ssh;

public class SshConnectorException extends Exception {
    
    public SshConnectorException(Throwable cause) {
        super(cause);
    }

    public SshConnectorException(String message) {
        super(message);
    }
}
