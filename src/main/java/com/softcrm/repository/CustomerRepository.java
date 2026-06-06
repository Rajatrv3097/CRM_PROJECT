package com.softcrm.repository;

import com.softcrm.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // ============ Company-wise ============
    @Query("SELECT c FROM Customer c WHERE c.companyId = :companyId AND c.isDeleted = false")
    List<Customer> findAllByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT c FROM Customer c WHERE c.companyId = :companyId AND c.id = :id AND c.isDeleted = false")
    Optional<Customer> findByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    // ============ Search ============
    @Query("SELECT c FROM Customer c WHERE c.companyId = :companyId AND c.isDeleted = false AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.customerCompanyName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Customer> searchCustomers(@Param("companyId") Long companyId, @Param("keyword") String keyword, Pageable pageable);

    // ============ Filters ============
    @Query("SELECT c FROM Customer c WHERE c.companyId = :companyId AND c.customerTier = :tier AND c.isDeleted = false")
    List<Customer> findByTier(@Param("companyId") Long companyId, @Param("tier") String tier);

    @Query("SELECT c FROM Customer c WHERE c.companyId = :companyId AND c.customerType = :type AND c.isDeleted = false")
    List<Customer> findByType(@Param("companyId") Long companyId, @Param("type") String type);

    @Query("SELECT c FROM Customer c WHERE c.companyId = :companyId AND c.customerStatus = :status AND c.isDeleted = false")
    List<Customer> findByStatus(@Param("companyId") Long companyId, @Param("status") String status);

    @Query("SELECT c FROM Customer c WHERE c.assignedTo.id = :userId AND c.isDeleted = false")
    List<Customer> findByAssignedTo(@Param("userId") Long userId);

    // ============ Check Existence ============
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.companyId = :companyId AND c.email = :email AND c.isDeleted = false")
    boolean existsByEmail(@Param("email") String email, @Param("companyId") Long companyId);

    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.companyId = :companyId AND c.phone = :phone AND c.isDeleted = false")
    boolean existsByPhone(@Param("phone") String phone, @Param("companyId") Long companyId);

    // ============ Statistics ============
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.companyId = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT c.customerTier, COUNT(c) FROM Customer c WHERE c.companyId = :companyId GROUP BY c.customerTier")
    List<Object[]> getCountByTier(@Param("companyId") Long companyId);

    @Query("SELECT COALESCE(SUM(c.totalPurchases), 0) FROM Customer c WHERE c.companyId = :companyId")
    BigDecimal getTotalRevenue(@Param("companyId") Long companyId);

    // ============ Top Customers ============
    @Query("SELECT c FROM Customer c WHERE c.companyId = :companyId ORDER BY c.totalPurchases DESC")
    List<Customer> findTopCustomers(@Param("companyId") Long companyId, Pageable pageable);
}