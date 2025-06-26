package store.piku.back.comment.repository;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.comment.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

}
