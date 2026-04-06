package com.pikume.back.social.adapter.in.web;

import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.social.adapter.in.web.problem.SocialProblemType;
import com.pikume.back.social.domain.comment.exception.CommentException;
import com.pikume.back.social.domain.friend.exception.AlreadyFriendsException;
import com.pikume.back.social.domain.friend.exception.FriendException;
import com.pikume.back.social.domain.friend.exception.FriendNotFoundException;
import com.pikume.back.social.domain.friend.exception.FriendRequestNotFoundException;
import com.pikume.back.social.domain.like.exception.DuplicateLikeException;
import com.pikume.back.social.domain.like.exception.LikeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(assignableTypes = { LikeController.class, CommentController.class, FriendController.class })
@RequiredArgsConstructor
public class SocialExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	@ExceptionHandler(LikeException.class)
	public ResponseEntity<ProblemDetail> handleLikeException(LikeException e, HttpServletRequest request) {
		log.warn("LikeException occurred: {}", e.getMessage());
		return buildProblem(SocialProblemType.from(e.getErrorCode()), e.getMessage(), request);
	}

	@ExceptionHandler(CommentException.class)
	public ResponseEntity<ProblemDetail> handleCommentException(CommentException e, HttpServletRequest request) {
		log.warn("CommentException occurred: {}", e.getMessage());
		return buildProblem(SocialProblemType.from(e.getErrorCode()), e.getMessage(), request);
	}

	@ExceptionHandler(DuplicateLikeException.class)
	public ResponseEntity<ProblemDetail> handleDuplicateLikeException(DuplicateLikeException e,
			HttpServletRequest request) {
		log.warn("DuplicateLikeException occurred: {}", e.getMessage());
		return buildProblem(SocialProblemType.DUPLICATE_LIKE, e.getMessage(), request);
	}

	@ExceptionHandler(FriendException.class)
	public ResponseEntity<ProblemDetail> handleFriendException(FriendException e, HttpServletRequest request) {
		log.warn("FriendException occurred: {}", e.getMessage());
		return buildProblem(friendProblemTypeOf(e), e.getMessage(), request);
	}

	private ResponseEntity<ProblemDetail> buildProblem(SocialProblemType problemType, String detail,
			HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(problemType, detail, request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}

	private SocialProblemType friendProblemTypeOf(FriendException exception) {
		if (exception instanceof AlreadyFriendsException) {
			return SocialProblemType.ALREADY_FRIENDS;
		}
		if (exception instanceof FriendRequestNotFoundException) {
			return SocialProblemType.FRIEND_REQUEST_NOT_FOUND;
		}
		if (exception instanceof FriendNotFoundException) {
			return SocialProblemType.FRIEND_NOT_FOUND;
		}
		return SocialProblemType.INVALID_FRIEND_REQUEST;
	}
}
