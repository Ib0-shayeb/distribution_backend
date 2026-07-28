package com.example.distribution_backernd.repository;

import com.example.distribution_backernd.model.LocationLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LocationLogRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LocationLogRepository locationLogRepository;

    @Test
    void shouldFindHistoryRangeInChronologicalOrder() {
        Integer userId = 100;
        ZonedDateTime now = ZonedDateTime.now();

        LocationLog log1 = createLog(userId, now.minusMinutes(30));
        LocationLog log2 = createLog(userId, now.minusMinutes(10));
        LocationLog logOld = createLog(userId, now.minusHours(5));
        LocationLog logOtherUser = createLog(999, now.minusMinutes(20));

        entityManager.persist(log1);
        entityManager.persist(log2);
        entityManager.persist(logOld);
        entityManager.persist(logOtherUser);
        entityManager.flush();

        List<LocationLog> results = locationLogRepository.findHistoryRange(
                userId,
                now.minusHours(1),
                now
        );

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getRecordedAt()).isEqualTo(log1.getRecordedAt());
        assertThat(results.get(1).getRecordedAt()).isEqualTo(log2.getRecordedAt());
    }

    @Test
    void shouldFindActiveUserIdsInLastHourAndDeduplicate() {
        ZonedDateTime now = ZonedDateTime.now();

        entityManager.persist(createLog(1, now.minusMinutes(10)));
        entityManager.persist(createLog(1, now.minusMinutes(40)));

        entityManager.persist(createLog(2, now.minusMinutes(5)));

        entityManager.persist(createLog(3, now.minusHours(2)));

        entityManager.flush();

        List<Integer> activeUserIds = locationLogRepository.findActiveUserIds();

        assertThat(activeUserIds).containsExactly(1, 2);
    }

    private LocationLog createLog(Integer userId, ZonedDateTime timestamp) {
        LocationLog log = new LocationLog();
        log.setUserId(userId);
        log.setRecordedAt(timestamp);

        log.setLatitude(52.2297);
        log.setLongitude(21.0122);

        return log;
    }
}