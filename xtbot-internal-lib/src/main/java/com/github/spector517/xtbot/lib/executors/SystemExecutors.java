package com.github.spector517.xtbot.lib.executors;

import com.github.spector517.xtbot.api.annotation.BotComponent;
import com.github.spector517.xtbot.api.annotation.Executor;
import com.github.spector517.xtbot.api.annotation.Name;
import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@BotComponent
@UtilityClass
public class SystemExecutors {

    @Executor("x.exec.system.pause")
    public static void pause(
            @Name("time") Integer time,
            @Name("unit") String unit
    ) {
        var chronoUnit = switch (unit.toLowerCase()) {
            case "millis" -> ChronoUnit.MILLIS;
            case "seconds" -> ChronoUnit.SECONDS;
            case "minutes" -> ChronoUnit.MINUTES;
            case "hours" -> ChronoUnit.HOURS;
            default -> throw new IllegalArgumentException("Unsupported time unit: " + unit);
        };
        var duration = Duration.of(time, chronoUnit);
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
