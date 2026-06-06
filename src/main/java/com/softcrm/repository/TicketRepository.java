package com.softcrm.repository;

import com.softcrm.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // ============ Basic Counts ============
    // ✅ Keep ONLY the enum versions, remove String versions
    long countByStatus(Ticket.TicketStatus status);
    long countByPriority(Ticket.TicketPriority priority);
    long countByCreatedAtAfter(LocalDateTime dateTime);

    // ============ Distribution Queries ============
    @Query("SELECT t.status as status, COUNT(t) as count FROM Ticket t GROUP BY t.status")
    List<Map<String, Object>> getTicketCountByStatus();

    @Query("SELECT t.priority as priority, COUNT(t) as count FROM Ticket t GROUP BY t.priority")
    List<Map<String, Object>> getTicketCountByPriority();

    @Query("SELECT t.status as status, COUNT(t) as count FROM Ticket t WHERE t.customer.companyId = :companyId GROUP BY t.status")
    List<Map<String, Object>> getTicketCountByStatusByCompany(@Param("companyId") Long companyId);

    @Query("SELECT t.priority as priority, COUNT(t) as count FROM Ticket t WHERE t.customer.companyId = :companyId GROUP BY t.priority")
    List<Map<String, Object>> getTicketCountByPriorityByCompany(@Param("companyId") Long companyId);

    // ============ Company Level (Admin) ============
    // ✅ Keep ONLY the enum version
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.customer.companyId = :companyId AND t.status = :status")
    long countByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") Ticket.TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.customer.companyId = :companyId AND t.createdAt >= :dateTime")
    long countByCompanyIdAndCreatedAtAfter(@Param("companyId") Long companyId, @Param("dateTime") LocalDateTime dateTime);

    // ============ Team Level (Manager) ============
    // ✅ Fix: Change parameter to Ticket.TicketStatus enum
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.manager.id = :managerId AND t.status = :status")
    long countByTeamIdAndStatus(@Param("managerId") Long managerId, @Param("status") Ticket.TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.manager.id = :managerId AND t.createdAt >= :dateTime")
    long countByTeamIdAndCreatedAtAfter(@Param("managerId") Long managerId, @Param("dateTime") LocalDateTime dateTime);

    // ============ Personal Level (Employee) ============
    // ✅ Keep ONLY the enum version
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :userId AND t.status = :status")
    long countByAssignedToAndStatus(@Param("userId") Long userId, @Param("status") Ticket.TicketStatus status);

    // ============ Recent Tickets ============
    @Query("SELECT t FROM Ticket t ORDER BY t.createdAt DESC")
    List<Ticket> findTopNByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    default List<Ticket> findTopNByOrderByCreatedAtDesc(int limit) {
        return findTopNByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Query("SELECT t FROM Ticket t WHERE t.customer.companyId = :companyId ORDER BY t.createdAt DESC")
    List<Ticket> findTopNByCompanyIdOrderByCreatedAtDesc(@Param("companyId") Long companyId, org.springframework.data.domain.Pageable pageable);

    default List<Ticket> findTopNByCompanyIdOrderByCreatedAtDesc(Long companyId, int limit) {
        return findTopNByCompanyIdOrderByCreatedAtDesc(companyId, org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Query("SELECT t FROM Ticket t WHERE t.assignedTo.id IN :userIds ORDER BY t.createdAt DESC")
    List<Ticket> findTopNByAssignedToInOrderByCreatedAtDesc(@Param("userIds") List<Long> userIds, org.springframework.data.domain.Pageable pageable);

    default List<Ticket> findTopNByAssignedToInOrderByCreatedAtDesc(List<Long> userIds, int limit) {
        return findTopNByAssignedToInOrderByCreatedAtDesc(userIds, org.springframework.data.domain.PageRequest.of(0, limit));
    }

    // ============ Resolution Time ============
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, t.createdAt, t.resolvedAt)) FROM Ticket t WHERE t.resolvedAt IS NOT NULL")
    Double getAverageResolutionTime();

    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, t.createdAt, t.resolvedAt)) FROM Ticket t WHERE t.customer.companyId = :companyId AND t.resolvedAt IS NOT NULL")
    Double getAverageResolutionTimeByCompany(@Param("companyId") Long companyId);
}