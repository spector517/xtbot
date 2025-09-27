package com.github.spector517.xtbot.core.properties;

import com.github.spector517.xtbot.core.properties.exception.IncludePreprocessorException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
class YamlFileIncludePreprocessor {

    private static final Pattern ARG_REPLACEMENT_PATTERN = Pattern.compile("!\\$(\\d+)");
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
            "^(\\p{Blank}*)!include\\p{Blank}+(?:'([^']+)'|\"([^\"]+)\"|([^\\s'\"]+))(\\p{Blank}+.+|\\p{Blank}+$|$)",
            Pattern.MULTILINE
    );
    private static final Pattern INCLUDE_ARGS_PATTERN = Pattern.compile(
            "^(?:(?:'[^']+'|\"[^\"]+\"|[^\\s'\"]+)(?:\\p{Blank}+|$))+$"
    );

    public String preprocess(String content, Path rootPath) throws IncludePreprocessorException {
        var matcher = INCLUDE_PATTERN.matcher(content);
        var buffer = new StringBuilder();
        while(matcher.find()) {
            var includePath = rootPath.getParent().resolve(extractPath(matcher)).normalize();
            var arguments = extractArguments(matcher.group(5).strip());
            var shift = matcher.group(1);
            var includedContent = includeContent(includePath, arguments, shift);
            matcher.appendReplacement(buffer, matcherSafeString(includedContent));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String extractPath(Matcher matcher) throws IncludePreprocessorException {
        return Stream.of(2, 3, 4)
                .map(matcher::group)
                .filter(Objects::nonNull)
                .findAny()
                .orElseThrow(() -> new IncludePreprocessorException("Fail to extract path"));
    }

    private List<String> extractArguments(String argsLine) throws IncludePreprocessorException {
        argsLine = argsLine.trim();
        if (argsLine.isBlank()) {
            return List.of();
        }
        var matcher = INCLUDE_ARGS_PATTERN.matcher(argsLine);
        if (!matcher.matches()) {
            throw new IncludePreprocessorException("Invalid include arguments format");
        }

        var args = new LinkedList<String>();
        var currentQuote = '\u0000';
        var argChars = new ArrayList<Character>();
        for (var symbol : argsLine.toCharArray()) {
            if ((symbol == '\'' || symbol == '"') && currentQuote == '\u0000') {
                currentQuote = symbol;
            } else if (symbol == currentQuote || symbol == ' ' && currentQuote == '\u0000') {
                currentQuote = '\u0000';
                args.add(buildArgument(argChars));
                argChars.clear();
            } else {
                argChars.add(symbol);
            }
        }
        args.add(buildArgument(argChars));
        return args.stream().filter(a -> !a.isEmpty()).toList();
    }

    private String buildArgument(List<Character> chars) {
        return chars.stream().map(Object::toString).collect(Collectors.joining());
    }

    private String includeContent(Path path, List<String> args, String shift) throws IncludePreprocessorException {
        try {
            var filledContent = fillContent(Files.readString(path, StandardCharsets.UTF_8), args);
            var includedContent = preprocess(filledContent, path);
            return includedContent.lines()
                    .map(shift::concat)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new IncludePreprocessorException(e);
        }
    }

    private String fillContent(String content, List<String> args) {
        var matcher = ARG_REPLACEMENT_PATTERN.matcher(content);
        var buffer = new StringBuilder();
        while (matcher.find()) {
            var argNumber = Integer.parseInt(matcher.group(1));
            var arg = argNumber < args.size() ? args.get(argNumber) : "";
            matcher.appendReplacement(buffer, matcherSafeString(arg));
        }
        matcher.appendTail(buffer);
        return buffer.toString().strip();
    }

    private String matcherSafeString(String s) {
        return s.replace("$", "\\$");
    }
}
