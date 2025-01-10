package com.github.spector517.xtbot.lib.executors;

import com.github.spector517.xtbot.api.annotation.BotComponent;
import com.github.spector517.xtbot.api.annotation.Executor;
import com.github.spector517.xtbot.api.annotation.Name;
import com.github.spector517.xtbot.lib.executors.ssh.JSchSshConnector;
import com.github.spector517.xtbot.lib.executors.ssh.SshCommandResult;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@BotComponent
@UtilityClass
public class SshExecutors {

    @Executor("xtbot.internal.check_ssh")
    public boolean isHostReachable(
            @Name("host") String host,
            @Name("port") int port,
            @Name("login") String login,
            @Name("password") String password,
            @Name("timeout") int timeout
    ) {
        try(var sshConnector = new JSchSshConnector(host, port, login, password, timeout)) {
            var message = "ping";
            var checkResult = sshConnector.runCommand("echo -n %s".formatted(message));
            return checkResult.exitCode() == 0 && checkResult.stdout().equals(message);
        } catch (Exception ex) {
            return false;
        }
    }

    @Executor("xtbot.internal.ssh_command")
    @SneakyThrows
    public SshCommandResult executeCommand(
            @Name("host") String host,
            @Name("port") int port,
            @Name("login") String login,
            @Name("password") String password,
            @Name("timeout") int timeout,
            @Name("command") String command
    ) {
        try(var sshConnector = new JSchSshConnector(host, port, login, password, timeout)) {
            return sshConnector.runCommand(command);
        }
    }
}
