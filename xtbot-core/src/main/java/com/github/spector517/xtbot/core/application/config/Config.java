package com.github.spector517.xtbot.core.application.config;

import com.github.spector517.xtbot.core.application.component.ComponentsContainer;
import com.github.spector517.xtbot.core.properties.LoadPropertiesException;
import com.github.spector517.xtbot.core.properties.Properties;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
public class Config {

    private final int version;
    private final String botToken;
    private final List<Stage> stages;
    
    private final ComponentsContainer container;
    private final Stage initialStage;
    private final Stage failStage;

    public Config(ComponentsContainer container) {
        this.container = container;
        final Properties props;
        try {
            props = container.propertiesLoader().load();
        } catch (LoadPropertiesException e) {
            throw new LoadConfigException(e);
        }
        this.version = props.version();
        this.botToken = props.botToken();
        this.stages = props.stages().stream()
                .map(stageProps -> new Stage(stageProps, container))
                .toList();
        checkDuplicateStages();
        initialStage = getInitialStage();
        failStage = getFailStage();
    }

    public Stage getStage(String name) throws StageNotFoundException {
        return stages.stream().filter(stage -> stage.name().equals(name)).findFirst()
                .orElseThrow(() -> new StageNotFoundException("No stage found with name: %s".formatted(name)));
    }

    private void checkDuplicateStages() {
        var allNames = stages.stream().map(Stage::name).toList();
        var distinctNames = allNames.stream().distinct().toList();
        if (allNames.size() != distinctNames.size()) {
            allNames = new ArrayList<>(allNames);
            distinctNames.forEach(allNames::remove);
            throw new LoadConfigException("Duplicate stage names found: %s"
                    .formatted(String.join(", ", allNames)));
        }
    }

    private Stage getInitialStage() {
        var initialStages = stages.stream().filter(Stage::initial).toList();
        if (initialStages.isEmpty()) {
            throw new LoadConfigException("No required initial stage found");
        }
        if (initialStages.size() > 1) {
            throw new LoadConfigException(
                "Only one initial stage allowed, but found: %s".formatted(
                    String.join(", ", initialStages.stream().map(Stage::name).toList())
                )
            );
        }
        return initialStages.getFirst();
    }

    private Stage getFailStage() {
        var failedStages = stages.stream().filter(Stage::fail).toList();
        if (failedStages.isEmpty()) {
            throw new LoadConfigException("No required fail stage found");
        }
        if (failedStages.size() > 1) {
            throw new LoadConfigException(
                "Only one fail stage allowed, but found: %s".formatted(
                    String.join(", ", failedStages.stream().map(Stage::name).toList())
                )
            );
        }
        return failedStages.getFirst();
    }
}
