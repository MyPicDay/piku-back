package com.pikume.back.diary.adapter.out.crosscontext;

import com.pikume.back.social.application.port.in.QueryFriendshipUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FriendAdapterForDiaryTest {

	@InjectMocks private FriendAdapterForDiary adapter;
	@Mock private QueryFriendshipUseCase queryFriendshipUseCase;

	@Test
	void translatesSocialFriendshipFactsForDiary() {
		given(queryFriendshipUseCase.areFriends("owner", "viewer")).willReturn(true);
		given(queryFriendshipUseCase.queryFriendIds("owner")).willReturn(List.of("viewer"));

		assertThat(adapter.areFriends("owner", "viewer")).isTrue();
		assertThat(adapter.findFriendIds("owner")).containsExactly("viewer");
	}
}
