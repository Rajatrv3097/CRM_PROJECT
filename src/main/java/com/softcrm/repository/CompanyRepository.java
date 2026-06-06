package com.softcrm.repository;

import com.softcrm.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    // ============ Basic CRUD Queries ============
    Optional<Company> findByCode(String code);
    Optional<Company> findByName(String name);

    boolean existsByCode(String code);
    boolean existsByName(String name);

    List<Company> findByIsActive(Boolean isActive);

    // Find first active company (for auto-assignment during registration)
    Optional<Company> findFirstByIsActiveTrue();

    // ============ Subscription Management ============
    @Query("SELECT c FROM Company c WHERE c.subscriptionExpiry < CURRENT_TIMESTAMP AND c.isActive = true")
    List<Company> findExpiredSubscriptions();

    @Query("SELECT COUNT(c) FROM Company c WHERE c.subscriptionExpiry < CURRENT_TIMESTAMP AND c.isActive = true")
    long countExpiredSubscriptions();

    @Query("SELECT COUNT(c) FROM Company c WHERE c.subscriptionExpiry BETWEEN CURRENT_TIMESTAMP AND :date AND c.isActive = true")
    long countExpiringInDays(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(c) FROM Company c WHERE c.subscriptionExpiry > CURRENT_TIMESTAMP AND c.isActive = true")
    long countActiveSubscriptions();

    // ============ Company Growth Analytics ============
    @Query("SELECT COUNT(c) FROM Company c WHERE c.createdAt >= :date")
    long countCompaniesCreatedAfter(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(c) FROM Company c WHERE c.createdAt BETWEEN :start AND :end")
    long countCompaniesCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(c) FROM Company c WHERE c.isActive = true")
    long countActiveCompanies();

    // ============ Storage Analytics ============
    @Query("SELECT COALESCE(SUM(c.usedStorageGb), 0) FROM Company c")
    Double getTotalStorageUsed();

    // ============ User Count Analytics ============
    @Query("SELECT COUNT(u) FROM User u WHERE u.companyId = :companyId")
    long countUsersByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.companyId = :companyId AND u.status = 'ACTIVE'")
    long countActiveUsersByCompanyId(@Param("companyId") Long companyId);

    // ============ Recent Companies ============
    @Query("SELECT c FROM Company c ORDER BY c.createdAt DESC")
    List<Company> findRecentCompanies(org.springframework.data.domain.Pageable pageable);

    default List<Company> findTop10RecentCompanies() {
        return findRecentCompanies(org.springframework.data.domain.PageRequest.of(0, 10));
    }

    // ============ Company Search ============
    @Query("SELECT c FROM Company c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Company> searchCompanies(@Param("keyword") String keyword);
}