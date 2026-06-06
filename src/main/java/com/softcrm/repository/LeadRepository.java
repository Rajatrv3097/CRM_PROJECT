package com.softcrm.repository;

import com.softcrm.entity.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    // ============ Company-wise Queries ============

    @Query("SELECT l FROM Lead l WHERE l.companyId = :companyId AND l.isDeleted = false")
    List<Lead> findAllByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT l FROM Lead l WHERE l.companyId = :companyId AND l.isDeleted = false")
    Page<Lead> findAllByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.companyId = :companyId AND l.id = :id AND l.isDeleted = false")
    Optional<Lead> findByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    // ============ Count by Company ============

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.companyId = :companyId AND l.isDeleted = false")
    long countByCompanyId(@Param("companyId") Long companyId);

    // ============ By Status ============

    @Query("SELECT l FROM Lead l WHERE l.companyId = :companyId AND l.status = :status AND l.isDeleted = false")
    List<Lead> findByStatus(@Param("companyId") Long companyId, @Param("status") String status);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.companyId = :companyId AND l.status = :status")
    long countByStatus(@Param("companyId") Long companyId, @Param("status") String status);

    // ============ By Assigned To ============

    @Query("SELECT l FROM Lead l WHERE l.assignedTo.id = :userId AND l.isDeleted = false")
    List<Lead> findByAssignedTo(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.assignedTo.id = :userId")
    long countByAssignedTo(@Param("userId") Long userId);

    // ============ UNASSIGNED LEADS QUERIES ============

    @Query("SELECT l FROM Lead l WHERE l.assignedTo IS NULL AND l.isDeleted = false")
    List<Lead> findByAssignedToIsNull();

    @Query("SELECT l FROM Lead l WHERE l.companyId = :companyId AND l.assignedTo IS NULL AND l.isDeleted = false")
    List<Lead> findByCompanyIdAndAssignedToIsNull(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.companyId = :companyId AND l.assignedTo IS NULL")
    long countUnassignedLeadsByCompany(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.assignedTo IS NULL")
    long countTotalUnassignedLeads();

    // ============ ASSIGNED LEADS QUERIES (Not null) ============

    @Query("SELECT l FROM Lead l WHERE l.assignedTo IS NOT NULL AND l.isDeleted = false")
    List<Lead> findByAssignedToIsNotNull();

    @Query("SELECT l FROM Lead l WHERE l.companyId = :companyId AND l.assignedTo IS NOT NULL AND l.isDeleted = false")
    List<Lead> findByCompanyIdAndAssignedToIsNotNull(@Param("companyId") Long companyId);

    // ============ Team Queries ============

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.assignedTo.manager.id = :managerId")
    long countByTeamId(@Param("managerId") Long managerId);

    // ============ Status & Source Counts ============

    @Query("SELECT l.status as status, COUNT(l) as count FROM Lead l WHERE l.companyId = :companyId GROUP BY l.status")
    List<Map<String, Object>> getLeadCountByStatus(@Param("companyId") Long companyId);

    @Query("SELECT l.source as source, COUNT(l) as count FROM Lead l WHERE l.companyId = :companyId GROUP BY l.source")
    List<Map<String, Object>> getLeadCountBySource(@Param("companyId") Long companyId);

    // ============ Monthly Data ============

    @Query("SELECT DATE_FORMAT(l.createdAt, '%Y-%m') as month, COUNT(l) as count FROM Lead l " +
            "WHERE l.companyId = :companyId AND l.createdAt >= :startDate GROUP BY month ORDER BY month")
    List<Map<String, Object>> getMonthlyLeadsDataByCompany(@Param("companyId") Long companyId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT DATE_FORMAT(l.createdAt, '%Y-%m') as month, COUNT(l) as count FROM Lead l " +
            "WHERE l.createdAt >= :startDate GROUP BY month ORDER BY month")
    List<Map<String, Object>> getMonthlyLeadsData(@Param("startDate") LocalDateTime startDate);

    // ============ Team Monthly Data ============

    @Query("SELECT DATE_FORMAT(l.createdAt, '%Y-%m') as month, COUNT(l) as count FROM Lead l " +
            "WHERE l.assignedTo.manager.id = :managerId AND l.createdAt >= :startDate GROUP BY month ORDER BY month")
    List<Map<String, Object>> getMonthlyLeadsDataByTeam(@Param("managerId") Long managerId, @Param("startDate") LocalDateTime startDate);

    // ============ User Monthly Data ============

    @Query("SELECT DATE_FORMAT(l.createdAt, '%Y-%m') as month, COUNT(l) as count FROM Lead l " +
            "WHERE l.assignedTo.id = :userId AND l.createdAt >= :startDate GROUP BY month ORDER BY month")
    List<Map<String, Object>> getUserMonthlyLeadsData(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    // ============ Search ============

    @Query("SELECT l FROM Lead l WHERE l.companyId = :companyId AND l.isDeleted = false AND " +
            "(LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(l.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(l.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(l.company) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Lead> searchLeads(@Param("companyId") Long companyId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.companyId = :companyId AND l.isDeleted = false AND " +
            "(LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(l.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(l.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Lead> searchLeadsSimple(@Param("companyId") Long companyId, @Param("keyword") String keyword);

    // ============ Duplicate Detection ============

    @Query("SELECT COUNT(l) > 0 FROM Lead l WHERE l.companyId = :companyId AND l.email = :email AND l.isDeleted = false")
    boolean existsByEmailAndCompanyId(@Param("email") String email, @Param("companyId") Long companyId);

    @Query("SELECT COUNT(l) > 0 FROM Lead l WHERE l.companyId = :companyId AND l.phone = :phone AND l.isDeleted = false")
    boolean existsByPhoneAndCompanyId(@Param("phone") String phone, @Param("companyId") Long companyId);

    // ============ Converted Leads ============

    @Query("SELECT l FROM Lead l WHERE l.convertedCustomer IS NOT NULL AND l.companyId = :companyId")
    List<Lead> findConvertedLeads(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.companyId = :companyId AND l.status = 'CONVERTED'")
    long countByCompanyIdAndStatusConverted(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.companyId = :companyId AND l.status = 'LOST'")
    long countByCompanyIdAndStatusLost(@Param("companyId") Long companyId);

    // ============ Recent Leads ============

    @Query("SELECT l FROM Lead l WHERE l.companyId = :companyId ORDER BY l.createdAt DESC")
    List<Lead> findRecentLeads(@Param("companyId") Long companyId, Pageable pageable);

    // ============ Team Status Counts ============

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.assignedTo.manager.id = :managerId AND l.status = :status")
    long countByTeamIdAndStatus(@Param("managerId") Long managerId, @Param("status") String status);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.assignedTo.id = :userId AND l.status = 'CONVERTED'")
    long countByAssignedToAndStatusConverted(@Param("userId") Long userId);

    // ============ Assignment Statistics ============

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.assignedTo.id = :userId")
    long countLeadsByAssignedUser(@Param("userId") Long userId);

    @Query("SELECT l.assignedTo.id as userId, COUNT(l) as leadCount FROM Lead l " +
            "WHERE l.companyId = :companyId AND l.assignedTo IS NOT NULL " +
            "GROUP BY l.assignedTo.id ORDER BY leadCount DESC")
    List<Map<String, Object>> getLeadAssignmentDistribution(@Param("companyId") Long companyId);

    // ============ Leads by Multiple Assignees ============

    @Query("SELECT l FROM Lead l WHERE l.assignedTo.id IN :userIds AND l.isDeleted = false")
    List<Lead> findByAssignedToIn(@Param("userIds") List<Long> userIds);

    @Query("SELECT l FROM Lead l WHERE l.companyId = :companyId AND l.assignedTo.id IN :userIds AND l.isDeleted = false")
    List<Lead> findByCompanyIdAndAssignedToIn(@Param("companyId") Long companyId, @Param("userIds") List<Long> userIds);
}