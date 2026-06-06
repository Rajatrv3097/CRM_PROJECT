package com.softcrm.repository;

import com.softcrm.entity.user.User;
import com.softcrm.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ============ Basic Find Methods ============
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailVerificationToken(String token);

    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByPhone(String phone);

    List<User> findByStatus(UserStatus status);
    List<User> findByCompanyId(Long companyId);

    // ============ Soft Delete Methods ============
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :userId")
    void deleteUserById(@Param("userId") Long userId);

    @Query("SELECT u FROM User u WHERE u.isDeleted = true")
    List<User> findDeletedUsers();

    @Modifying
    @Query("UPDATE User u SET u.isDeleted = true WHERE u.id = :userId")
    void softDeleteUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.isDeleted = false WHERE u.id = :userId")
    void restoreUser(@Param("userId") Long userId);

    // ============ Company-based Queries ============
    @Query("SELECT u FROM User u WHERE u.companyId = :companyId AND u.isDeleted = false")
    List<User> findAllByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u WHERE u.companyId = :companyId")
    List<User> findAllByCompanyIdIncludeDeleted(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u WHERE u.companyId = :companyId AND u.isDeleted = false")
    Page<User> findAllByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    // ============ User Type Queries ============
    @Query("SELECT u FROM User u WHERE u.userType = :userType")
    List<User> findByUserType(@Param("userType") String userType);

    @Query("SELECT u FROM User u WHERE u.userType IN :userTypes")
    List<User> findByUserTypeIn(@Param("userTypes") List<String> userTypes);

    @Query("SELECT u FROM User u WHERE u.userType = :userType")
    Page<User> findByUserType(@Param("userType") String userType, Pageable pageable);

    // ============ Company + User Type Queries ============
    @Query("SELECT u FROM User u WHERE u.companyId = :companyId AND u.userType = :userType AND u.isDeleted = false")
    List<User> findByCompanyIdAndUserType(@Param("companyId") Long companyId, @Param("userType") String userType);

    @Query("SELECT u FROM User u WHERE u.companyId = :companyId AND u.userType IN :userTypes AND u.isDeleted = false")
    List<User> findByCompanyIdAndUserTypeIn(@Param("companyId") Long companyId, @Param("userTypes") List<String> userTypes);

    @Query("SELECT u FROM User u WHERE u.companyId = :companyId AND u.userType != 'ADMIN' AND u.isDeleted = false")
    List<User> findNonAdminsByCompany(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u WHERE u.companyId = :companyId")
    Page<User> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.companyId = :companyId AND u.isDeleted = false")
    long countActiveUsersByCompany(@Param("companyId") Long companyId);

    // ============ Manager/Employee Relationship Queries ============
    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId AND u.isDeleted = false")
    List<User> findByManagerId(@Param("managerId") Long managerId);

    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId AND u.isDeleted = false")
    Page<User> findByManagerId(@Param("managerId") Long managerId, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.manager.id = :managerId AND u.isDeleted = false")
    long countByManagerId(@Param("managerId") Long managerId);

    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId AND u.userType = 'EMPLOYEE' AND u.isDeleted = false")
    List<User> findTeamMembersByManagerId(@Param("managerId") Long managerId);

    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId AND u.userType IN ('EMPLOYEE', 'MANAGER') AND u.isDeleted = false")
    List<User> findAllSubordinatesByManagerId(@Param("managerId") Long managerId);

    // ============ Role-based Convenience Methods ============
    default List<User> findAllAdmins() {
        return findByUserType("ADMIN");
    }

    default List<User> findAllSuperAdmins() {
        return findByUserType("SUPER_ADMIN");
    }

    default List<User> findAllManagers() {
        return findByUserType("MANAGER");
    }

    default List<User> findAllEmployees() {
        return findByUserType("EMPLOYEE");
    }

    default List<User> findAllCustomers() {
        return findByUserType("CUSTOMER");
    }

    @Query("SELECT u FROM User u WHERE u.userType = 'ADMIN' AND u.companyId = :companyId AND u.isDeleted = false")
    List<User> findAdminsByCompany(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u WHERE u.userType = 'MANAGER' AND u.companyId = :companyId AND u.isDeleted = false")
    List<User> findManagersByCompany(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u WHERE u.userType = 'EMPLOYEE' AND u.companyId = :companyId AND u.isDeleted = false")
    List<User> findEmployeesByCompany(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u WHERE u.userType = 'CUSTOMER' AND u.companyId = :companyId AND u.isDeleted = false")
    List<User> findCustomersByCompany(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u WHERE u.userType IN ('ADMIN', 'MANAGER') AND u.companyId = :companyId AND u.isDeleted = false")
    List<User> findLeadershipByCompany(@Param("companyId") Long companyId);

    // ============ For Lead Assignment ============
    /**
     * Get available users (EMPLOYEE or MANAGER) for lead assignment in a company
     */
    @Query("SELECT u FROM User u WHERE u.companyId = :companyId AND u.userType IN ('EMPLOYEE', 'MANAGER') AND u.isDeleted = false AND u.status = 'ACTIVE'")
    List<User> findAvailableUsersForAssignment(@Param("companyId") Long companyId);

    /**
     * Get available users by specific user types for lead assignment
     */
    @Query("SELECT u FROM User u WHERE u.companyId = :companyId AND u.userType IN :userTypes AND u.isDeleted = false AND u.status = 'ACTIVE'")
    List<User> findAvailableUsersForAssignmentByTypes(@Param("companyId") Long companyId, @Param("userTypes") List<String> userTypes);

    /**
     * Get all active users in a company (excluding deleted and inactive)
     */
    @Query("SELECT u FROM User u WHERE u.companyId = :companyId AND u.isDeleted = false AND u.status = 'ACTIVE'")
    List<User> findAllActiveUsersByCompany(@Param("companyId") Long companyId);

    // ============ Top Performers (Revenue-based) ============
    @Query("SELECT u FROM User u WHERE u.userType != 'CUSTOMER' ORDER BY " +
            "(SELECT COALESCE(SUM(d.amount), 0) FROM Deal d WHERE d.owner.id = u.id AND d.status = 'CLOSED_WON') DESC")
    List<User> findTop10ByOrderByTotalRevenueDesc();

    @Query("SELECT u FROM User u WHERE u.companyId = :companyId AND u.userType != 'CUSTOMER' ORDER BY " +
            "(SELECT COALESCE(SUM(d.amount), 0) FROM Deal d WHERE d.owner.id = u.id AND d.status = 'CLOSED_WON') DESC")
    List<User> findTop10ByCompanyIdOrderByTotalRevenueDesc(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId ORDER BY " +
            "(SELECT COALESCE(SUM(d.amount), 0) FROM Deal d WHERE d.owner.id = u.id AND d.status = 'CLOSED_WON') DESC")
    List<User> findTop10ByManagerIdOrderByTotalRevenueDesc(@Param("managerId") Long managerId);

    // ============ Revenue Queries ============
    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Deal d WHERE d.owner.id = :userId AND d.status = 'CLOSED_WON'")
    BigDecimal getTotalRevenueByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Deal d WHERE d.owner.id = :userId AND d.status = 'CLOSED_WON' AND MONTH(d.closedDate) = MONTH(CURRENT_DATE)")
    BigDecimal getMonthlyRevenueByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Deal d WHERE d.owner.id = :userId AND d.status = 'CLOSED_WON' AND d.closedDate BETWEEN :startDate AND :endDate")
    BigDecimal getRevenueByUserBetweenDates(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Deal d WHERE d.owner.id IN :userIds AND d.status = 'CLOSED_WON'")
    BigDecimal getTotalRevenueByUsers(@Param("userIds") List<Long> userIds);

    // ============ Task Queries ============
    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.status = :status")
    long countTasksByAssignedToAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId")
    long countTotalTasksByAssignedTo(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.status = 'COMPLETED'")
    long countCompletedTasksByAssignedTo(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.createdByUser.id = :userId")
    long countTasksCreatedByUser(@Param("userId") Long userId);

    // ============ Lead Queries ============
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.assignedTo.id = :userId AND l.status = :status")
    long countLeadsByAssignedToAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.assignedTo.id = :userId")
    long countTotalLeadsByAssignedTo(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.createdBy = :username")
    long countLeadsCreatedByUser(@Param("username") String username);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.assignedTo.id = :userId AND l.status = 'CONVERTED'")
    long countConvertedLeadsByAssignedTo(@Param("userId") Long userId);

    // ============ Customer Queries ============
    @Query("SELECT COUNT(u) FROM User u WHERE u.assignedTo.id = :userId AND u.userType = 'CUSTOMER'")
    long countCustomersByAssignedTo(@Param("userId") Long userId);

    @Query("SELECT u FROM User u WHERE u.assignedTo.id = :userId AND u.userType = 'CUSTOMER'")
    List<User> findCustomersByAssignedTo(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(u.totalPurchases), 0) FROM User u WHERE u.assignedTo.id = :userId AND u.userType = 'CUSTOMER'")
    BigDecimal getTotalCustomerPurchasesByAssignedTo(@Param("userId") Long userId);

    // ============ Deal Queries ============
    @Query("SELECT COUNT(d) FROM Deal d WHERE d.owner.id = :userId AND d.status = :status")
    long countDealsByOwnerAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT COUNT(d) FROM Deal d WHERE d.owner.id = :userId")
    long countTotalDealsByOwner(@Param("userId") Long userId);

    @Query("SELECT COUNT(d) FROM Deal d WHERE d.owner.id = :userId AND d.stage = :stage")
    long countDealsByOwnerAndStage(@Param("userId") Long userId, @Param("stage") String stage);

    @Query("SELECT COALESCE(AVG(d.amount), 0) FROM Deal d WHERE d.owner.id = :userId AND d.status = 'CLOSED_WON'")
    BigDecimal getAverageDealSizeByUser(@Param("userId") Long userId);

    // ============ Ticket Queries ============
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :userId AND t.status = :status")
    long countTicketsByAssignedToAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :userId")
    long countTotalTicketsByAssignedTo(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.reportedBy.id = :userId")
    long countTicketsReportedByUser(@Param("userId") Long userId);

    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, t.createdAt, t.resolvedAt)) FROM Ticket t WHERE t.assignedTo.id = :userId AND t.resolvedAt IS NOT NULL")
    Double getAverageResolutionTimeByUser(@Param("userId") Long userId);

    // ============ Meeting Queries ============
    @Query("SELECT COUNT(m) FROM Meeting m WHERE m.organizer.id = :userId AND m.status = 'SCHEDULED'")
    long countScheduledMeetingsByOrganizer(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Meeting m WHERE m.attendee.id = :userId AND m.status = 'SCHEDULED'")
    long countScheduledMeetingsByAttendee(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Meeting m WHERE (m.organizer.id = :userId OR m.attendee.id = :userId) AND m.status = 'COMPLETED'")
    long countCompletedMeetingsByUser(@Param("userId") Long userId);

    // ============ Search Queries ============
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.companyId = :companyId AND " +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsersByCompany(@Param("companyId") Long companyId, @Param("search") String search, Pageable pageable);

    // ============ Admin List Query (with company name) ============
    @Query(value = "SELECT u.id, u.username, u.email, u.phone, u.first_name, u.last_name, " +
            "u.user_type, u.company_id, u.department, u.is_email_verified, " +
            "c.name as company_name, u.status, u.created_at " +
            "FROM users u " +
            "LEFT JOIN companies c ON u.company_id = c.id " +
            "WHERE u.user_type = 'ADMIN'", nativeQuery = true)
    List<Object[]> findAdminList();

    @Query(value = "SELECT u.id, u.username, u.email, u.phone, u.first_name, u.last_name, " +
            "u.user_type, u.company_id, u.department, u.is_email_verified, " +
            "c.name as company_name, u.status, u.created_at " +
            "FROM users u " +
            "LEFT JOIN companies c ON u.company_id = c.id " +
            "WHERE u.user_type = 'ADMIN' AND u.company_id = :companyId", nativeQuery = true)
    List<Object[]> findAdminsByCompanyWithDetails(@Param("companyId") Long companyId);

    // ============ Customer List with Details ============
    @Query(value = "SELECT u.id, u.username, u.email, u.phone, u.first_name, u.last_name, " +
            "u.company_name, u.gst_number, u.industry, u.city, u.state, " +
            "u.total_purchases, u.total_orders, u.loyalty_points, u.customer_tier, " +
            "u.customer_status, CONCAT(assigned.first_name, ' ', assigned.last_name) as assigned_to_name " +
            "FROM users u " +
            "LEFT JOIN users assigned ON u.assigned_to = assigned.id " +
            "WHERE u.user_type = 'CUSTOMER' AND u.company_id = :companyId", nativeQuery = true)
    List<Object[]> findCustomersByCompanyWithDetails(@Param("companyId") Long companyId);

    // ============ Statistics Queries ============
    @Query("SELECT COUNT(DISTINCT u) FROM User u WHERE u.companyId = :companyId AND u.createdAt BETWEEN :startDate AND :endDate")
    long countNewUsersByCompanyBetweenDates(@Param("companyId") Long companyId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT u.userType, COUNT(u) FROM User u WHERE u.companyId = :companyId GROUP BY u.userType")
    List<Object[]> getUserTypeDistributionByCompany(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt >= :since")
    long countActiveUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM User u WHERE u.userType = 'CUSTOMER' AND u.createdAt BETWEEN :startDate AND :endDate")
    long countNewCustomersBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ============ Bulk Operations ============
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.companyId = :companyId")
    void updateStatusByCompanyId(@Param("status") UserStatus status, @Param("companyId") Long companyId);

    @Modifying
    @Query("UPDATE User u SET u.manager.id = :managerId WHERE u.id = :userId")
    void updateManagerForUser(@Param("userId") Long userId, @Param("managerId") Long managerId);

    @Modifying
    @Query("UPDATE User u SET u.isDeleted = true WHERE u.companyId = :companyId")
    void softDeleteByCompanyId(@Param("companyId") Long companyId);
}