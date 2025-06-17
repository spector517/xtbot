package com.github.spector517.xtbot.core.repository.entity;

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
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false)
    private Long externalId;

    @Column(name = "name")
    private String name;

    @Column(name = "current_stage")
    private String currentStage;

    @Column(name = "previous_sended_message_id")
    private Integer previousSendedMessageId;

    @Column(name = "previous_stages", columnDefinition = "TEXT")
    private String previousStages;

    @Column(name = "stage_initiated")
    private Boolean currentStageInitiated;

    @Column(name = "stage_completed")
    private Boolean currentStageCompleted;

    @Column(name = "additional_vars", columnDefinition = "TEXT")
    private String additionalVars;

    @Column(name = "stage_vars", columnDefinition = "TEXT")
    private String stageVars;
}
