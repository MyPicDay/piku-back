package com.pikume.back.social.application.service;

import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.social.application.policy.AnonymousCommentAccessPolicy;
import com.pikume.back.social.application.port.out.LoadCommentEngagementPort;
import com.pikume.back.social.application.port.out.LoadCommentThreadsPort;
import com.pikume.back.social.application.port.out.LoadSocialParticipantProfilesPort;
import com.pikume.back.social.application.port.out.ResolveInteractionDiaryPort;
import com.pikume.back.social.application.readmodel.CommentThreadView;
import com.pikume.back.social.application.readmodel.DiaryEngagementCount;
import com.pikume.back.social.application.readmodel.InteractionDiaryView;
import com.pikume.back.social.application.readmodel.SocialParticipantProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceTest {

	@Mock private LoadCommentThreadsPort loadCommentThreadsPort;
	@Mock private LoadCommentEngagementPort loadCommentEngagementPort;
	@Mock private ResolveInteractionDiaryPort resolveInteractionDiaryPort;
	@Mock private LoadSocialParticipantProfilesPort loadSocialParticipantProfilesPort;
	private CommentQueryService service;

	@BeforeEach
	void setUp() {
		service = new CommentQueryService(
				loadCommentThreadsPort,
				loadCommentEngagementPort,
				resolveInteractionDiaryPort,
				loadSocialParticipantProfilesPort,
				new AnonymousCommentAccessPolicy());
	}

	@Test
	void hidesAnonymousIdentityAndSkipsProfileLookup() {
		PageQuery query = PageQuery.of(0, 10);
		given(resolveInteractionDiaryPort.resolveVisibleDiary(1L, "third"))
				.willReturn(Optional.of(new InteractionDiaryView(1L, "owner", true, false)));
		given(loadCommentThreadsPort.loadRootCommentPage(1L, query))
				.willReturn(new PageResult<>(List.of(comment("author")), 0, 10, 1));

		var item = service.queryRootCommentPage(1L, query, "third").content().get(0);

		assertThat(item.userId()).isNull();
		assertThat(item.nickname()).isEqualTo("익명");
		assertThat(item.avatar()).isNull();
		assertThat(item.content()).isEqualTo("비공개 댓글");
		verify(loadSocialParticipantProfilesPort, never()).loadProfiles(anySet());
	}

	@Test
	void enrichesIdentifiedCommentWithTranslatedProfile() {
		PageQuery query = PageQuery.of(0, 10);
		given(resolveInteractionDiaryPort.resolveVisibleDiary(1L, "viewer"))
				.willReturn(Optional.of(new InteractionDiaryView(1L, "owner", false, false)));
		given(loadCommentThreadsPort.loadRootCommentPage(1L, query))
				.willReturn(new PageResult<>(List.of(comment("author")), 0, 10, 1));
		given(loadSocialParticipantProfilesPort.loadProfiles(Set.of("author")))
				.willReturn(Map.of("author", new SocialParticipantProfile("author", "작성자", "https://avatar")));

		var item = service.queryRootCommentPage(1L, query, "viewer").content().get(0);

		assertThat(item.userId()).isEqualTo("author");
		assertThat(item.nickname()).isEqualTo("작성자");
		assertThat(item.avatar()).isEqualTo("https://avatar");
	}

	@Test
	void convertsTypedCommentCountsToMap() {
		given(loadCommentEngagementPort.loadActiveCommentCounts(List.of(1L, 2L)))
				.willReturn(List.of(new DiaryEngagementCount(1L, 2L)));

		assertThat(service.queryCommentCounts(List.of(1L, 2L))).containsEntry(1L, 2L);
	}

	private CommentThreadView comment(String author) {
		LocalDateTime now = LocalDateTime.of(2026, 7, 22, 10, 0);
		return new CommentThreadView(10L, 1L, author, "내용", null, 0, now, now, false);
	}
}
