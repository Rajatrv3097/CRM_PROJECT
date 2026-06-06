package com.softcrm.repository;



import com.softcrm.entity.LoginLog;
import com.softcrm.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
    List<LoginLog> findByUserOrderByLoginTimeDesc(User user);
    List<LoginLog> findByEmailOrderByLoginTimeDesc(String email);
}
