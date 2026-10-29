package com.example.demo.repository;

import com.example.demo.model.Post;
import com.example.demo.model.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    List<PostComment> findByPostOrderByCreatedAtAsc(Post post);

    List<PostComment> findByPostAndParentCommentIsNullOrderByCreatedAtAsc(Post post);

    List<PostComment> findByParentCommentOrderByCreatedAtAsc(PostComment parentComment);

    long countByPost(Post post);
}
