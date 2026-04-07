package com.email.writer.repository;

import com.email.writer.entity.SavedReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SavedReplyRepository extends JpaRepository<SavedReply, Long> {

    List<SavedReply> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<SavedReply> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<SavedReply> findByUserIdAndIsFavoriteOrderByCreatedAtDesc(Long userId, Boolean isFavorite);

    @Query("SELECT sr FROM SavedReply sr WHERE sr.user.id = :userId AND " +
            "(LOWER(sr.emailSubject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sr.replyText) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sr.summary) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<SavedReply> findByUserIdAndSearchTerm(@Param("userId") Long userId,
                                               @Param("search") String search);

    @Query("SELECT sr FROM SavedReply sr WHERE sr.user.id = :userId AND " +
            "LOWER(sr.tone) = LOWER(:tone) AND " +
            "(LOWER(sr.emailSubject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sr.replyText) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(sr.summary) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<SavedReply> findByUserIdAndSearchTermAndTone(@Param("userId") Long userId,
                                                      @Param("search") String search,
                                                      @Param("tone") String tone);

    @Query("SELECT sr FROM SavedReply sr WHERE sr.user.id = :userId " +
            "AND (:tone IS NULL OR LOWER(sr.tone) = LOWER(:tone)) " +
            "AND (:fromDate IS NULL OR sr.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR sr.createdAt <= :toDate) " +
            "ORDER BY sr.createdAt DESC")
    List<SavedReply> findByUserIdWithFilters(@Param("userId") Long userId,
                                             @Param("tone") String tone,
                                             @Param("fromDate") LocalDateTime fromDate,
                                             @Param("toDate") LocalDateTime toDate);

    long countByUserId(Long userId);

    // Added: used by getUserStatistics() to count favorites via DB — previously
    // the service loaded ALL replies into the JVM just to count favorites.
    long countByUserIdAndIsFavorite(Long userId, Boolean isFavorite);

    // Added: used by getUserStatistics() to count recent activity via DB —
    // previously the service loaded ALL replies and filtered in Java.
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime date);

    @Query("SELECT sr.tone, COUNT(sr) FROM SavedReply sr WHERE sr.user.id = :userId " +
            "AND sr.tone IS NOT NULL " +
            "GROUP BY sr.tone " +
            "ORDER BY COUNT(sr) DESC")
    List<Object[]> getToneDistributionForUser(@Param("userId") Long userId);

    @Query("SELECT sr.emailSubject, COUNT(sr) FROM SavedReply sr WHERE sr.user.id = :userId " +
            "AND sr.emailSubject IS NOT NULL " +
            "GROUP BY sr.emailSubject " +
            "ORDER BY COUNT(sr) DESC")
    List<Object[]> getMostCommonSubjectsForUser(@Param("userId") Long userId, Pageable pageable);
}