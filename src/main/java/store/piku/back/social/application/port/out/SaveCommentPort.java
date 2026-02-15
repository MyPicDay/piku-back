package store.piku.back.social.application.port.out;

import store.piku.back.social.domain.comment.Comment;

public interface SaveCommentPort {

	Comment save(Comment comment);
}
