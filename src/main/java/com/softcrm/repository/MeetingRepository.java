package com.softcrm.repository;

import com.softcrm.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByStartTimeAfterAndStatus(LocalDateTime after, Meeting.MeetingStatus status);

    @Query("SELECT m FROM Meeting m WHERE m.organizer.id = :userId OR m.attendee.id = :userId ORDER BY m.startTime ASC")
    List<Meeting> findUpcomingByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM Meeting m WHERE (m.organizer.id IN (SELECT u.id FROM User u WHERE u.companyId = :companyId) OR m.attendee.id IN (SELECT u.id FROM User u WHERE u.companyId = :companyId)) AND m.startTime >= :startTime AND m.status = 'SCHEDULED' ORDER BY m.startTime ASC")
    List<Meeting> findUpcomingByCompany(@Param("companyId") Long companyId, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT m FROM Meeting m WHERE (m.organizer.manager.id = :managerId OR m.attendee.manager.id = :managerId) AND m.startTime >= :startTime AND m.status = 'SCHEDULED' ORDER BY m.startTime ASC")
    List<Meeting> findUpcomingByTeam(@Param("managerId") Long managerId, @Param("startTime") LocalDateTime startTime);

    // ============ ADD THESE MISSING METHODS ============

    @Query("SELECT m FROM Meeting m WHERE m.startTime BETWEEN :start AND :end ORDER BY m.startTime ASC")
    List<Meeting> findByStartTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT m FROM Meeting m WHERE (m.organizer.companyId = :companyId OR m.attendee.companyId = :companyId) AND m.startTime BETWEEN :start AND :end ORDER BY m.startTime ASC")
    List<Meeting> findByCompanyIdAndStartTimeBetween(@Param("companyId") Long companyId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT m FROM Meeting m WHERE (m.organizer.id = :userId OR m.attendee.id = :userId) AND m.startTime BETWEEN :start AND :end ORDER BY m.startTime ASC")
    List<Meeting> findByUserIdAndStartTimeBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}