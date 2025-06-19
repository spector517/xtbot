package com.github.spector517.xtbot.core.repository.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Entity
@Table(name = "xtbot_clients")
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "xtbot_client_message_ids",
            joinColumns = @JoinColumn(name = "client_id", referencedColumnName = "id")
    )
    @OrderColumn(name = "message_id_position")
    @Column(name = "message_id", nullable = false)
    private List<Integer> sentMessageIds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "xtbot_client_stages",
        joinColumns = @JoinColumn(name = "client_id", referencedColumnName = "id")
    )
    @OrderColumn(name = "stage_position")
    @Column(name = "stage", nullable = false)
    private List<String> stages;

    @Column(name = "stage_initiated", nullable = false)
    private Boolean stageInitiated;

    @Column(name = "stage_completed", nullable = false)
    private Boolean stageCompleted;

    @Column(name = "additional_vars", columnDefinition = "TEXT", nullable = false)
    private String additionalVars;

    @Column(name = "stage_vars", columnDefinition = "TEXT", nullable = false)
    private String stageVars;
}
