package com.github.spector517.xtbot.lib.executors.ssh;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class JSchSshConnector implements SshConnector {

    public static final int SAMPLING = 100;

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final int timeout;

    private Session session;
    private ChannelExec commandChannel;
    private ChannelSftp sftpChannel;

    public JSchSshConnector(
            String host, int port, String username, String password, int timeout
        ) throws SshConnectorException {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        bindSession();
    }

    @Override
    public SshCommandResult runCommand(String command) throws SshConnectorException {
        try {
            bindCommandChannel();
            var stdOutStream = new ByteArrayOutputStream();
            var stdErrStream = new ByteArrayOutputStream();

            commandChannel.setCommand(command);
            commandChannel.setOutputStream(stdOutStream);
            commandChannel.setErrStream(stdErrStream);
            commandChannel.connect(timeout);

            var maxAttemptsCount = Math.ceilDiv(timeout, SAMPLING);
            while (commandChannel.isConnected() && maxAttemptsCount > 0) {
                Thread.sleep(SAMPLING);
                maxAttemptsCount--;
            }
            if (commandChannel.isConnected()) {
                throw new SshConnectorException("Exec command timeout. Command '%s'".formatted(command));
            }

            var exitCode = commandChannel.getExitStatus();
            var stdOut = new String(stdOutStream.toByteArray(), StandardCharsets.UTF_8);
            var stdErr = new String(stdErrStream.toByteArray(), StandardCharsets.UTF_8);
            return new SshCommandResult(exitCode, stdOut, stdErr);
        } catch (JSchException ex) {
            throw new SshConnectorException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public void copyContent(InputStream content, String destinationPath) throws SshConnectorException {
        try {
            bindSftpChannel();
            sftpChannel.put(content, destinationPath);
        } catch(SftpException ex) {
            throw new SshConnectorException(ex);
        }
    }

    @Override
    public InputStream fetchContent(String sourcePath) throws SshConnectorException {
        try {
            bindSftpChannel();
            return sftpChannel.get(sourcePath);
        } catch(SftpException ex) {
            throw new SshConnectorException(ex);
        }
    }

    @Override
    public void close() {
        if (commandChannel != null && commandChannel.isConnected()) {
            commandChannel.disconnect();
        }
        if (sftpChannel != null && sftpChannel.isConnected()) {
            sftpChannel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    private void bindSession() throws SshConnectorException {
        try {
            session = new JSch().getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(timeout);
        } catch (JSchException ex) {
            throw new SshConnectorException(ex);
        }
    }

    private void bindCommandChannel() throws SshConnectorException {
        if (commandChannel != null && commandChannel.isConnected()) {
            return;
        }
        try {
            commandChannel = (ChannelExec) session.openChannel("exec");
        } catch (JSchException ex) {
            throw new SshConnectorException(ex);
        }
    }

    private void bindSftpChannel() throws SshConnectorException {
        if (sftpChannel != null && sftpChannel.isConnected()) {
            return;
        }
        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(timeout);
        } catch (JSchException ex) {
            throw new SshConnectorException(ex);
        }
    }
}
