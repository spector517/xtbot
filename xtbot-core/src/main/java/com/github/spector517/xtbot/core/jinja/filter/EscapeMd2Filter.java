package com.github.spector517.xtbot.core.jinja.filter;

import com.hubspot.jinjava.lib.filter.Filter;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;

import java.util.Set;

public class EscapeMd2Filter implements Filter {

    public static final Set<Character> CHARACTERS_TO_ESCAPE = Set.of(
        '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'
    );

    @Override
    public String getName() {
        return "escape_md2";
    }

    @Override
    public Object filter(Object inputValue, JinjavaInterpreter interpreter, String... args) {
        if (inputValue == null) {
            return null;
        }

        var input = inputValue.toString();
        var escaped = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (CHARACTERS_TO_ESCAPE.contains(c)) {
                escaped.append('\\');
            }
            escaped.append(c);
        }

        return escaped.toString();
    }
}
