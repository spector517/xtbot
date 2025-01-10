package com.github.spector517.xtbot.core.application.repository;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Entity
@Table(name = "clients")
@Data
@Accessors(fluent = true, chain = true)
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "external_id", unique = true, nullable = false)
    private long externalId;

    @Column(name = "name")
    private String name;

    @Column(name = "current_stage")
    private String currentStage;

    @Column(name = "previous_sended_message_id")
    private int previousSendedMessageId;

    @Column(name = "previous_stages")
    private String previousStages;

    @Column(name = "stage_initiated")
    private boolean currentStageInitiated;

    @Column(name = "stage_completed")
    private boolean currentStageCompleted;

    @Column(name = "additional_vars")
    private String additionalVars;

    @Column(name = "stage_vars")
    private String stageVars;
}
