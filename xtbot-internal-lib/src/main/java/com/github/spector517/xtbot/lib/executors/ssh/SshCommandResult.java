package com.github.spector517.xtbot.lib.executors.ssh;

public record SshCommandResult(
    int exitCode,
    String stdout,
    String stderr
) {}