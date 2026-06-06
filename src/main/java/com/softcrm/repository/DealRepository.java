package com.softcrm.repository;

import com.softcrm.entity.Deal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {

    // ================= BASIC COUNTS =================

    @Query("""
            SELECT COUNT(d)
            FROM Deal d
            WHERE d.status = :status
            """)
    long countByStatus(@Param("status") Deal.DealStatus status);

    @Query("""
            SELECT COUNT(d)
            FROM Deal d
            WHERE d.owner.id = :ownerId
            AND d.status = :status
            """)
    long countByOwnerIdAndStatus(
            @Param("ownerId") Long ownerId,
            @Param("status") Deal.DealStatus status
    );

    @Query("""
            SELECT COUNT(d)
            FROM Deal d
            WHERE d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            """)
    long countClosedWonDeals();

    // ✅ ADDED - Count closed won deals by company
    @Query("""
            SELECT COUNT(d)
            FROM Deal d
            WHERE d.owner.companyId = :companyId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            """)
    long countClosedWonDealsByCompany(@Param("companyId") Long companyId);

    // ✅ ADDED - Count closed won deals by team
    @Query("""
            SELECT COUNT(d)
            FROM Deal d
            WHERE d.owner.manager.id = :teamId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            """)
    long countClosedWonDealsByTeam(@Param("teamId") Long teamId);

    // ✅ ADDED - Count closed won deals by user
    @Query("""
            SELECT COUNT(d)
            FROM Deal d
            WHERE d.owner.id = :userId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            """)
    long countClosedWonDealsByUser(@Param("userId") Long userId);

    // ================= TEAM PERFORMANCE =================

    @Query("""
            SELECT u.id as userId,
                   COALESCE(u.username, CONCAT(u.firstName, ' ', u.lastName)) as userName,
                   COUNT(d) as dealCount,
                   COALESCE(SUM(d.amount), 0) as totalRevenue
            FROM User u
            LEFT JOIN Deal d ON d.owner.id = u.id
            WHERE u.id IN :userIds
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            GROUP BY u.id, u.username, u.firstName, u.lastName
            ORDER BY SUM(d.amount) DESC
            """)
    List<Map<String, Object>> getTeamPerformanceByUsers(@Param("userIds") List<Long> userIds);

    @Query("""
            SELECT u.id as userId,
                   COALESCE(u.username, CONCAT(u.firstName, ' ', u.lastName)) as userName,
                   COUNT(d) as dealCount,
                   COALESCE(SUM(d.amount), 0) as totalRevenue
            FROM User u
            LEFT JOIN Deal d ON d.owner.id = u.id
            WHERE d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            GROUP BY u.id, u.username, u.firstName, u.lastName
            ORDER BY SUM(d.amount) DESC
            """)
    List<Map<String, Object>> getTeamPerformance();

    // ================= TEAM COUNTS =================

    @Query("""
            SELECT COUNT(d)
            FROM Deal d
            WHERE d.owner.manager.id = :teamId
            AND d.status = :status
            """)
    long countByTeamIdAndStatus(
            @Param("teamId") Long teamId,
            @Param("status") Deal.DealStatus status
    );

    // ================= MONTHLY REVENUE BY COMPANY WITH DATE RANGE =================

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.owner.companyId = :companyId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND d.closedDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal getMonthlyRevenueByCompanyAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ================= MONTHLY SALES DATA BY TEAM =================

    @Query("""
            SELECT DATE_FORMAT(d.closedDate, '%Y-%m') as month,
                   COUNT(d) as dealCount,
                   COALESCE(SUM(d.amount), 0) as revenue
            FROM Deal d
            WHERE d.owner.manager.id = :teamId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND d.closedDate >= :startDate
            GROUP BY month
            ORDER BY month
            """)
    List<Map<String, Object>> getMonthlySalesDataByTeam(
            @Param("teamId") Long teamId,
            @Param("startDate") LocalDateTime startDate
    );

    // ================= MONTHLY SALES DATA BY COMPANY =================

    @Query("""
            SELECT DATE_FORMAT(d.closedDate, '%Y-%m') as month,
                   COUNT(d) as dealCount,
                   COALESCE(SUM(d.amount), 0) as revenue
            FROM Deal d
            WHERE d.owner.companyId = :companyId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND d.closedDate >= :startDate
            GROUP BY month
            ORDER BY month
            """)
    List<Map<String, Object>> getMonthlySalesDataByCompany(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate
    );

    // ================= MONTHLY DEALS CLOSED DATA =================

    @Query("""
            SELECT DATE_FORMAT(d.closedDate, '%Y-%m') as month,
                   COUNT(d) as dealsClosed,
                   COALESCE(SUM(d.amount), 0) as totalValue
            FROM Deal d
            WHERE d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND d.closedDate >= :startDate
            GROUP BY month
            ORDER BY month
            """)
    List<Map<String, Object>> getMonthlyDealsClosedData(
            @Param("startDate") LocalDateTime startDate
    );

    // ================= REVENUE =================

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND MONTH(d.closedDate) = MONTH(CURRENT_DATE)
            """)
    BigDecimal getMonthlyRevenue();

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            """)
    BigDecimal getTotalRevenue();

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.owner.id = :userId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            """)
    BigDecimal getUserRevenue(@Param("userId") Long userId);

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.owner.id = :userId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND MONTH(d.closedDate) = MONTH(CURRENT_DATE)
            """)
    BigDecimal getUserMonthlyRevenue(@Param("userId") Long userId);

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.owner.id = :userId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND d.closedDate BETWEEN :start AND :end
            """)
    BigDecimal getUserRevenueBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ================= DATE RANGE =================

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND d.closedDate BETWEEN :start AND :end
            """)
    BigDecimal getRevenueBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ================= MONTHLY CHART DATA =================

    @Query("""
            SELECT DATE_FORMAT(d.closedDate, '%Y-%m') as month,
                   COALESCE(SUM(d.amount), 0) as revenue
            FROM Deal d
            WHERE d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND d.closedDate >= :startDate
            GROUP BY month
            ORDER BY month
            """)
    List<Map<String, Object>> getMonthlySalesData(
            @Param("startDate") LocalDateTime startDate
    );

    // ================= COMPANY =================

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.owner.companyId = :companyId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            """)
    BigDecimal getTotalRevenueByCompany(
            @Param("companyId") Long companyId
    );

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.owner.companyId = :companyId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND MONTH(d.closedDate) = MONTH(CURRENT_DATE)
            """)
    BigDecimal getMonthlyRevenueByCompany(
            @Param("companyId") Long companyId
    );

    @Query("""
            SELECT COUNT(d)
            FROM Deal d
            WHERE d.owner.companyId = :companyId
            AND d.status = :status
            """)
    long countByCompanyIdAndStatus(
            @Param("companyId") Long companyId,
            @Param("status") Deal.DealStatus status
    );

    // ================= TEAM =================

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.owner.manager.id = :managerId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            """)
    BigDecimal getTotalRevenueByTeam(
            @Param("managerId") Long managerId
    );

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.owner.manager.id = :managerId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND MONTH(d.closedDate) = MONTH(CURRENT_DATE)
            """)
    BigDecimal getMonthlyRevenueByTeam(
            @Param("managerId") Long managerId
    );

    // ================= USER =================

    @Query("""
            SELECT DATE_FORMAT(d.closedDate, '%Y-%m') as month,
                   COALESCE(SUM(d.amount), 0) as revenue
            FROM Deal d
            WHERE d.owner.id = :userId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND d.closedDate >= :startDate
            GROUP BY month
            ORDER BY month
            """)
    List<Map<String, Object>> getUserMonthlySalesData(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate
    );

    // ================= DEAL STAGE =================

    @Query("""
            SELECT d.stage as stage,
                   COUNT(d) as count
            FROM Deal d
            GROUP BY d.stage
            """)
    List<Map<String, Object>> getDealCountByStage();

    @Query("""
            SELECT d.stage as stage,
                   COUNT(d) as count
            FROM Deal d
            WHERE d.owner.companyId = :companyId
            GROUP BY d.stage
            """)
    List<Map<String, Object>> getDealCountByStageByCompany(
            @Param("companyId") Long companyId
    );

    // ================= REGION =================

    @Query("""
            SELECT COALESCE(d.customer.city, 'Unknown') as city,
                   COALESCE(SUM(d.amount), 0) as revenue
            FROM Deal d
            WHERE d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            GROUP BY d.customer.city
            ORDER BY SUM(d.amount) DESC
            """)
    List<Map<String, Object>> getRevenueByRegion();

    @Query("""
            SELECT COALESCE(d.customer.city, 'Unknown') as city,
                   COALESCE(SUM(d.amount), 0) as revenue
            FROM Deal d
            WHERE d.owner.companyId = :companyId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            GROUP BY d.customer.city
            ORDER BY SUM(d.amount) DESC
            """)
    List<Map<String, Object>> getRevenueByRegionForCompany(
            @Param("companyId") Long companyId
    );

    // ================= PRODUCT =================

    @Query("""
            SELECT COALESCE(d.name, 'Unknown') as product,
                   COALESCE(SUM(d.amount), 0) as revenue
            FROM Deal d
            WHERE d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            GROUP BY d.name
            ORDER BY SUM(d.amount) DESC
            """)
    List<Map<String, Object>> getRevenueByProduct();

    @Query("""
            SELECT COALESCE(d.name, 'Unknown') as product,
                   COALESCE(SUM(d.amount), 0) as revenue
            FROM Deal d
            WHERE d.owner.companyId = :companyId
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            GROUP BY d.name
            ORDER BY SUM(d.amount) DESC
            """)
    List<Map<String, Object>> getRevenueByProductForCompany(
            @Param("companyId") Long companyId
    );

    // ================= TOP PERFORMERS =================

    @Query("""
            SELECT u,
                   COUNT(d),
                   COALESCE(SUM(d.amount), 0)
            FROM User u
            LEFT JOIN Deal d
            ON d.owner.id = u.id
            AND d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            GROUP BY u.id
            ORDER BY SUM(d.amount) DESC
            """)
    List<Object[]> getTopPerformingEmployees(Pageable pageable);

    default List<Object[]> getTopPerformingEmployees(int limit) {
        return getTopPerformingEmployees(PageRequest.of(0, limit));
    }

    // ================= DATE REPORTS =================

    @Query("""
            SELECT COUNT(d)
            FROM Deal d
            WHERE d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND d.closedDate BETWEEN :start AND :end
            """)
    long countByClosedDateBetweenAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Deal d
            WHERE d.stage = com.softcrm.entity.Deal.DealStage.CLOSED_WON
            AND d.closedDate BETWEEN :start AND :end
            """)
    BigDecimal sumAmountByClosedDateBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}