package com.pikume.back.social.adapter.out.persistence;

import com.pikume.back.social.domain.friend.FriendRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FriendPersistenceAdapterTest {

	@InjectMocks
	private FriendPersistenceAdapter adapter;
	@Mock
	private FriendJpaRepository friendJpaRepository;
	@Mock
	private FriendRequestJpaRepository friendRequestJpaRepository;

	@Test
	void translatesMySqlDuplicateKeyViolationToNotRecorded() {
		given(friendRequestJpaRepository.insert("from", "to"))
				.willThrow(integrityViolation("duplicate", "23000", 1062));

		boolean recorded = adapter.tryRecordPendingRequest(new FriendRequest("from", "to"));

		assertThat(recorded).isFalse();
	}

	@Test
	void preservesUnknownIntegrityViolation() {
		DataIntegrityViolationException failure = integrityViolation("foreign key", "23000", 1452);
		given(friendRequestJpaRepository.insert("from", "to")).willThrow(failure);

		assertThatThrownBy(() -> adapter.tryRecordPendingRequest(new FriendRequest("from", "to")))
				.isSameAs(failure);
	}

	private DataIntegrityViolationException integrityViolation(String message, String sqlState, int errorCode) {
		return new DataIntegrityViolationException(
				message,
				new SQLIntegrityConstraintViolationException(message, sqlState, errorCode));
	}
}
