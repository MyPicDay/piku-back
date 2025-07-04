package store.piku.back.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.piku.back.comment.entity.Comment;


public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "user")
    Page<Comment> findByDiaryIdAndParentIsNull(Long diaryId, Pageable pageable);

    int countByParentId(Long parentId);

    @EntityGraph(attributePaths = "user")
    Page<Comment> findByParentId(Long parentId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.diary.id = :diaryId")
    long countAllByDiaryId(@Param("diaryId") Long diaryId);

}