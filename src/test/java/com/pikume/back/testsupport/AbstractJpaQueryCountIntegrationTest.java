package com.pikume.back.testsupport;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public abstract class AbstractJpaQueryCountIntegrationTest {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	protected void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	protected Statistics hibernateStatistics() {
		return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
	}

	protected long measurePreparedStatements(Runnable action) {
		flushAndClear();
		Statistics statistics = hibernateStatistics();
		statistics.clear();

		action.run();

		return statistics.getPrepareStatementCount();
	}
}
