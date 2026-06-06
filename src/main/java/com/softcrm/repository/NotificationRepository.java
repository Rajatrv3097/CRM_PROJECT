package com.softcrm.repository;

import com.softcrm.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ============ Basic Queries ============
    @Query("SELECT n FROM Notification n ORDER BY n.createdAt DESC")
    List<Notification> findTopNByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    default List<Notification> findTopNByOrderByCreatedAtDesc(int limit) {
        return findTopNByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, limit));
    }

    // ============ User Notifications ============
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findTopNByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    default List<Notification> findTopNByUserIdOrderByCreatedAtDesc(Long userId, int limit) {
        return findTopNByUserIdOrderByCreatedAtDesc(userId, org.springframework.data.domain.PageRequest.of(0, limit));
    }

    // ============ Unread Counts ============
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isRead = false")
    long countByIsReadFalse();

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    long countByUserIdAndIsReadFalse(@Param("userId") Long userId);

    // ============ Mark as Read ============
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    int markAllAsReadByUserId(@Param("userId") Long userId);

    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :notificationId AND n.user.id = :userId")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    int markAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    // ============ Delete Notifications ============
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND n.isRead = true")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    int deleteAllReadByUserId(@Param("userId") Long userId);

    // ============ Type-based Queries ============
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndType(@Param("userId") Long userId, @Param("type") String type, org.springframework.data.domain.Pageable pageable);

    // ============ Date Range Queries ============
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") java.time.LocalDateTime since);
}