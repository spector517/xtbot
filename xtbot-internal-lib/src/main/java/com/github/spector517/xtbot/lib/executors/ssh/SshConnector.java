package com.github.spector517.xtbot.lib.executors.ssh;

import java.io.InputStream;

public interface SshConnector extends AutoCloseable {

    SshCommandResult runCommand(String command) throws SshConnectorException;

    void copyContent(InputStream content, String destinationPath) throws SshConnectorException;

    InputStream fetchContent(String sourcePath) throws SshConnectorException;
}
