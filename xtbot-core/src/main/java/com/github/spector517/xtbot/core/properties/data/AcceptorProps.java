package com.github.spector517.xtbot.core.properties.data;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class AcceptorProps {

    private String acceptor;
    private String val;

    @JsonAnySetter
    public void init(String key, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value of acceptor '%s' cannot be null".formatted(key));
        }
        if (acceptor != null) {
            throw new IllegalArgumentException(
                "Acceptor '%s' already defined, cannot set one more '%s'".formatted(acceptor, key)
            );
        }
        acceptor = key;
        val = value;
    }
}
