package com.github.spector517.xtbot.lib.executors;

import com.github.spector517.xtbot.api.annotation.BotComponent;
import com.github.spector517.xtbot.api.annotation.Executor;
import com.github.spector517.xtbot.api.annotation.Name;
import lombok.experimental.UtilityClass;

import java.util.List;

@BotComponent
@UtilityClass
public class TextExecutors {

    @Executor("xtbot.internal.match")
    public boolean match(
            @Name("patterns") List<String> patterns,
            @Name("val") String val
    ) {
        if (val == null) {
            return patterns == null || patterns.isEmpty();
        }
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().anyMatch(val::matches);
    }

    @Executor("xtbot.internal.split")
    public List<String> split(
            @Name("val") String val,
            @Name("delimiter") String delimiter
    ) {
        if (val == null) {
            return List.of();
        }
        if (delimiter == null) {
            return List.of(val);
        }
        return List.of(val.split(delimiter));
    }
}
