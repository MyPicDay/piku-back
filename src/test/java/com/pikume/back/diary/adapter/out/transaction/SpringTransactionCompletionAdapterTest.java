package com.pikume.back.diary.adapter.out.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@DisplayName("SpringTransactionCompletionAdapter")
class SpringTransactionCompletionAdapterTest {

	@AfterEach
	void clearSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("커밋 후 작업은 독립된 새 트랜잭션에서 실행한다")
	void runsAfterCommitTaskInRequiresNewTransaction() throws Exception {
		PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
		TransactionStatus transactionStatus = mock(TransactionStatus.class);
		given(transactionManager.getTransaction(any(TransactionDefinition.class))).willReturn(transactionStatus);
		SpringTransactionCompletionAdapter adapter = new SpringTransactionCompletionAdapter(transactionManager);
		AtomicBoolean executed = new AtomicBoolean(false);
		TransactionSynchronizationManager.initSynchronization();

		adapter.runAfterCommit(() -> executed.set(true), exception -> {
		});

		assertThat(executed).isFalse();
		TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);
		synchronization.afterCommit();

		assertThat(executed).isTrue();
		then(transactionManager).should().getTransaction(
				org.mockito.ArgumentMatchers.argThat(definition ->
						definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
		then(transactionManager).should().commit(transactionStatus);
	}

	@Test
	@DisplayName("커밋 후 독립 트랜잭션 실패는 실패 처리기에 전달하고 요청 흐름에 전파하지 않는다")
	void suppressesFailureFromAfterCommitTransaction() {
		PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
		TransactionStatus transactionStatus = mock(TransactionStatus.class);
		given(transactionManager.getTransaction(any(TransactionDefinition.class))).willReturn(transactionStatus);
		UnexpectedRollbackException failure = new UnexpectedRollbackException("notification rollback");
		willThrow(failure).given(transactionManager).commit(transactionStatus);
		SpringTransactionCompletionAdapter adapter = new SpringTransactionCompletionAdapter(transactionManager);
		AtomicBoolean executed = new AtomicBoolean(false);
		AtomicReference<RuntimeException> handledFailure = new AtomicReference<>();
		TransactionSynchronizationManager.initSynchronization();

		adapter.runAfterCommit(() -> executed.set(true), handledFailure::set);
		TransactionSynchronization synchronization =
				TransactionSynchronizationManager.getSynchronizations().get(0);

		assertThatCode(synchronization::afterCommit).doesNotThrowAnyException();

		assertThat(executed).isTrue();
		assertThat(handledFailure.get()).isSameAs(failure);
	}
}
