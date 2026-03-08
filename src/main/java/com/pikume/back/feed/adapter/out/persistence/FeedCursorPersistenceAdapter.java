package com.pikume.back.feed.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import com.pikume.back.feed.application.dto.FeedBucket;
import com.pikume.back.feed.application.dto.FeedCursor;
import com.pikume.back.feed.application.dto.FeedCursorCandidate;
import com.pikume.back.feed.application.port.out.LoadFeedCursorCandidatesPort;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class FeedCursorPersistenceAdapter implements LoadFeedCursorCandidatesPort {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<FeedCursorCandidate> loadCandidates(String currentUserId, FeedBucket bucket, FeedCursor cursor, int limit) {
		if (limit <= 0) {
			return List.of();
		}
		if (!FeedBucket.hasUser(currentUserId) && bucket != FeedBucket.NOT_CONSUMED_PUBLIC) {
			return List.of();
		}

		Query query = entityManager.createNativeQuery(buildSql(currentUserId, bucket, cursor));
		applyParameters(query, currentUserId, cursor);
		query.setMaxResults(limit);

		@SuppressWarnings("unchecked")
		List<Object[]> rows = query.getResultList();

		return rows.stream()
				.map(row -> new FeedCursorCandidate(
						bucket,
						((Number) row[0]).longValue(),
						((Number) row[1]).longValue(),
						((Number) row[2]).longValue(),
						toLocalDateTime(row[3])))
				.toList();
	}

	private String buildSql(String currentUserId, FeedBucket bucket, FeedCursor cursor) {
		StringBuilder sql = new StringBuilder("""
				SELECT d.id,
				       COALESCE(lc.like_count, 0) AS like_count,
				       COALESCE(cc.comment_count, 0) AS comment_count,
				       d.created_at
				FROM diary d
				LEFT JOIN (
				    SELECT l.diary_id, COUNT(l.id) AS like_count
				    FROM likes l
				    WHERE l.deleted_at IS NULL
				    GROUP BY l.diary_id
				) lc ON lc.diary_id = d.id
				LEFT JOIN (
				    SELECT c.diary_id, COUNT(c.id) AS comment_count
				    FROM comments c
				    WHERE c.deleted_at IS NULL
				    GROUP BY c.diary_id
				) cc ON cc.diary_id = d.id
				WHERE d.deleted_at IS NULL
				""");

		appendVisibilityCondition(sql, currentUserId, bucket);
		appendConsumedCondition(sql, currentUserId, bucket);
		appendCursorCondition(sql, cursor);

		sql.append("""
				
				ORDER BY d.created_at DESC,
				         COALESCE(lc.like_count, 0) DESC,
				         COALESCE(cc.comment_count, 0) DESC,
				         d.id DESC
				""");

		return sql.toString();
	}

	private void appendVisibilityCondition(StringBuilder sql, String currentUserId, FeedBucket bucket) {
		if (!FeedBucket.hasUser(currentUserId)) {
			sql.append(" AND d.status = 'PUBLIC'");
			return;
		}

		sql.append(" AND d.user_id <> :currentUserId");

		if (bucket.isFriendBucket()) {
			sql.append("""
					 AND d.status IN ('FRIENDS', 'PUBLIC')
					 AND EXISTS (
					     SELECT 1
					     FROM friend f
					     WHERE (f.user_id_1 = :currentUserId AND f.user_id_2 = d.user_id)
					        OR (f.user_id_2 = :currentUserId AND f.user_id_1 = d.user_id)
					 )
					""");
			return;
		}

		sql.append("""
				 AND d.status = 'PUBLIC'
				 AND NOT EXISTS (
				     SELECT 1
				     FROM friend f
				     WHERE (f.user_id_1 = :currentUserId AND f.user_id_2 = d.user_id)
				        OR (f.user_id_2 = :currentUserId AND f.user_id_1 = d.user_id)
				 )
				""");
	}

	private void appendConsumedCondition(StringBuilder sql, String currentUserId, FeedBucket bucket) {
		if (!FeedBucket.hasUser(currentUserId)) {
			return;
		}

		String consumedClause = """
				(
				    EXISTS (
				        SELECT 1
				        FROM feed_click fc
				        WHERE fc.user_id = :currentUserId
				          AND fc.diary_id = d.id
				    )
				    OR EXISTS (
				        SELECT 1
				        FROM likes ul
				        WHERE ul.user_id = :currentUserId
				          AND ul.diary_id = d.id
				          AND ul.deleted_at IS NULL
				    )
				    OR EXISTS (
				        SELECT 1
				        FROM comments uc
				        WHERE uc.user_id = :currentUserId
				          AND uc.diary_id = d.id
				          AND uc.deleted_at IS NULL
				    )
				)
				""";

		sql.append(bucket.isConsumedBucket() ? " AND " : " AND NOT ")
				.append(consumedClause);
	}

	private void appendCursorCondition(StringBuilder sql, FeedCursor cursor) {
		if (cursor == null) {
			return;
		}

		sql.append("""
				 AND (
				     d.created_at < :cursorCreatedAt
				     OR (
				         d.created_at = :cursorCreatedAt
				         AND COALESCE(lc.like_count, 0) < :cursorLikeCount
				     )
				     OR (
				         d.created_at = :cursorCreatedAt
				         AND COALESCE(lc.like_count, 0) = :cursorLikeCount
				         AND COALESCE(cc.comment_count, 0) < :cursorCommentCount
				     )
				     OR (
				         d.created_at = :cursorCreatedAt
				         AND COALESCE(lc.like_count, 0) = :cursorLikeCount
				         AND COALESCE(cc.comment_count, 0) = :cursorCommentCount
				         AND d.id < :cursorDiaryId
				     )
				 )
				""");
	}

	private void applyParameters(Query query, String currentUserId, FeedCursor cursor) {
		if (FeedBucket.hasUser(currentUserId)) {
			query.setParameter("currentUserId", currentUserId);
		}
		if (cursor != null) {
			query.setParameter("cursorLikeCount", cursor.likeCount());
			query.setParameter("cursorCommentCount", cursor.commentCount());
			query.setParameter("cursorCreatedAt", cursor.createdAt());
			query.setParameter("cursorDiaryId", cursor.diaryId());
		}
	}

	private LocalDateTime toLocalDateTime(Object value) {
		if (value instanceof Timestamp timestamp) {
			return timestamp.toLocalDateTime();
		}
		if (value instanceof LocalDateTime localDateTime) {
			return localDateTime;
		}
		throw new IllegalStateException("지원하지 않는 createdAt 타입입니다: " + value);
	}
}
