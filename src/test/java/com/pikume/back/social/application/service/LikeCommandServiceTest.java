package com.pikume.back.social.application.service;

import com.pikume.back.social.application.event.SocialNotificationEvent;
import com.pikume.back.social.application.exception.SocialErrorCode;
import com.pikume.back.social.application.exception.SocialException;
import com.pikume.back.social.application.port.out.LoadDiaryLikesPort;
import com.pikume.back.social.application.port.out.PublishSocialNotificationEventPort;
import com.pikume.back.social.application.port.out.RecordDiaryLikePort;
import com.pikume.back.social.application.port.out.ResolveInteractionDiaryPort;
import com.pikume.back.social.application.readmodel.InteractionDiaryView;
import com.pikume.back.social.domain.like.Like;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LikeCommandServiceTest {

	@InjectMocks private LikeCommandService service;
	@Mock private LoadDiaryLikesPort loadDiaryLikesPort;
	@Mock private RecordDiaryLikePort recordDiaryLikePort;
	@Mock private ResolveInteractionDiaryPort resolveInteractionDiaryPort;
	@Mock private PublishSocialNotificationEventPort eventPort;

	@Test
	void recordsNewLikeAndPublishesForAnotherOwner() {
		given(resolveInteractionDiaryPort.resolveVisibleDiary(1L, "liker"))
				.willReturn(Optional.of(new InteractionDiaryView(1L, "owner", false, false)));
		given(loadDiaryLikesPort.loadLikeState("liker", 1L)).willReturn(Optional.empty());
		given(loadDiaryLikesPort.countActiveLikes(1L)).willReturn(3L);

		var result = service.addLike("liker", 1L);

		assertThat(result.likeCount()).isEqualTo(3L);
		verify(recordDiaryLikePort).recordLike(any());
		verify(eventPort).publish(new SocialNotificationEvent.LikeCreated("owner", "liker", 1L));
	}

	@Test
	void reactivatingLikeDoesNotRepublishCreation() {
		Like like = Like.builder().userId("liker").diaryId(1L).build();
		like.cancel();
		given(resolveInteractionDiaryPort.resolveVisibleDiary(1L, "liker"))
				.willReturn(Optional.of(new InteractionDiaryView(1L, "owner", false, false)));
		given(loadDiaryLikesPort.loadLikeState("liker", 1L)).willReturn(Optional.of(like));

		service.addLike("liker", 1L);

		assertThat(like.isActive()).isTrue();
		verify(eventPort, never()).publish(any());
	}

	@Test
	void rejectsAlreadyActiveLike() {
		Like like = Like.builder().userId("liker").diaryId(1L).build();
		given(resolveInteractionDiaryPort.resolveVisibleDiary(1L, "liker"))
				.willReturn(Optional.of(new InteractionDiaryView(1L, "owner", false, false)));
		given(loadDiaryLikesPort.loadLikeState("liker", 1L)).willReturn(Optional.of(like));

		assertThatThrownBy(() -> service.addLike("liker", 1L))
				.isInstanceOfSatisfying(SocialException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(SocialErrorCode.ALREADY_LIKED));
	}
}
