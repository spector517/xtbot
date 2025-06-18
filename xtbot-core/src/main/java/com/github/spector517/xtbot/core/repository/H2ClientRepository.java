package com.github.spector517.xtbot.core.repository;

import com.github.spector517.xtbot.core.repository.entity.ClientEntity;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class H2ClientRepository implements ClientRepository {

    private static final String DB_NAME = "xtbot";

    private final SessionFactory sessionFactory;

    @SneakyThrows
    public H2ClientRepository(Path directory) {
        directory = directory.toAbsolutePath().normalize();
        var dbFile = directory.resolve(DB_NAME);
        if (!Files.exists(directory)) {
            log.warn("H2 database location {} does not exist, creating a new one", directory);
            Files.createDirectories(directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException("H2 database location must be a directory, but was: %s"
                    .formatted(directory));
        }
        var configuration = new Configuration();
        configuration.setProperties(getH2Properties(dbFile.toString()));
        configuration.addAnnotatedClass(ClientEntity.class);

        this.sessionFactory = configuration
                .buildSessionFactory(
                        new StandardServiceRegistryBuilder()
                                .applySettings(configuration.getProperties())
                                .build()
                );
    }

    @Override
    public synchronized ClientEntity findByExternalId(long externalId) throws ClientNotFoundException {
        try(var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            var client = getClientByExternalId(externalId, session);
            if (client.isEmpty()) {
                transaction.commit();
                throw new ClientNotFoundException("Client with externalId %d not found".formatted(externalId));
            }
            transaction.commit();
            return client.get();
        }
    }

    @Override
    public synchronized void save(ClientEntity entity) {
        try(var session = sessionFactory.openSession()) {
            var transaction = session.beginTransaction();
            var client = getClientByExternalId(entity.externalId(), session);
            if (client.isEmpty()) {
                log.debug("Saving new client with externalId {}", entity.externalId());
                session.persist(entity);
            } else {
                log.debug("Updating existing client with externalId {}", entity.externalId());
                entity.id(client.get().id());
                session.merge(entity);
            }
            transaction.commit();
        }
    }

    private Properties getH2Properties(String dbFileLocation) {
        var props = new Properties();
        props.put("hibernate.connection.driver_class", "org.h2.Driver");
        props.put("hibernate.connection.url", "jdbc:h2:file:%s".formatted(dbFileLocation));
        props.put("hibernate.connection.username", "sa");
        props.put("hibernate.connection.password", "");
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.hbm2ddl.auto", "create-only");
        return props;
    }

    private Optional<ClientEntity> getClientByExternalId(long externalId, Session session) {
        return session.createQuery("FROM ClientEntity WHERE externalId = :externalId", ClientEntity.class)
                .setParameter("externalId", externalId)
                .uniqueResultOptional();
    }
}
