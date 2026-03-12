package com.github.spector517.xtbot.core.repository.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Entity
@Table(name = "xtbot_messages")
@Data
@Accessors(fluent = true, chain = true)
@EqualsAndHashCode(exclude = "client")
@ToString(exclude = "client")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Column(name = "telegram_message_id")
    private Integer telegramMessageId;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MessageType type;
}

