package com.softcrm.repository;

import com.softcrm.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findTop20ByOrderByActivityTimeDesc();

    @Query("SELECT a FROM ActivityLog a WHERE a.user.id IN (SELECT u.id FROM User u WHERE u.companyId = :companyId) ORDER BY a.activityTime DESC")
    List<ActivityLog> findTop20ByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT a FROM ActivityLog a WHERE a.user.manager.id = :managerId ORDER BY a.activityTime DESC")
    List<ActivityLog> findTop20ByTeamId(@Param("managerId") Long managerId);

    @Query("SELECT a FROM ActivityLog a WHERE a.user.id = :userId ORDER BY a.activityTime DESC")
    List<ActivityLog> findTop20ByUserId(@Param("userId") Long userId);
}