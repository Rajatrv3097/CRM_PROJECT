package com.softcrm.repository;

import com.softcrm.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // ============ Basic Counts ============
    // ✅ Use enum instead of String
    long countByStatus(Task.TaskStatus status);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    // ============ User Level ============
    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.status = :status")
    long countByAssignedToIdAndStatus(@Param("userId") Long userId, @Param("status") Task.TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId")
    long countByAssignedToId(@Param("userId") Long userId);

    // ============ Company Level ============
    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.companyId = :companyId AND t.status = :status")
    long countByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") Task.TaskStatus status);

    // ============ Team Level ============
    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.manager.id = :teamId AND t.status = :status")
    long countByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") Task.TaskStatus status);

    // ============ Distribution Queries ============
    @Query("SELECT t.status as status, COUNT(t) as count FROM Task t GROUP BY t.status")
    List<Map<String, Object>> getTaskCountByStatus();

    @Query("SELECT t.priority as priority, COUNT(t) as count FROM Task t GROUP BY t.priority")
    List<Map<String, Object>> getTaskCountByPriority();

    // ============ Recent Tasks ============
    @Query("SELECT t FROM Task t ORDER BY t.createdAt DESC")
    List<Task> findTopNByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    default List<Task> findTopNByOrderByCreatedAtDesc(int limit) {
        return findTopNByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, limit));
    }
}