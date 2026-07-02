package com.example.demo.repository;

import com.example.demo.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByUserIdAndPollId(Long userId, Long pollId);
    List<Vote> findByUserId(Long userId);
    
    @jakarta.transaction.Transactional
    void deleteByPollId(Long pollId);
}
