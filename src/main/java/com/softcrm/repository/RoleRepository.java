package com.softcrm.repository;

import com.softcrm.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Boolean existsByName(String name);

    // ✅ Find roles by multiple names (returns Set)
    @Query("SELECT r FROM Role r WHERE r.name IN :names")
    Set<Role> findByNameIn(@Param("names") Set<String> names);

    // ✅ Find roles by multiple names (returns List)
    @Query("SELECT r FROM Role r WHERE r.name IN :names")
    List<Role> findRolesByNameIn(@Param("names") List<String> names);

    // ✅ Find all roles except specific ones
    @Query("SELECT r FROM Role r WHERE r.name NOT IN :excludeNames")
    List<Role> findAllExcept(@Param("excludeNames") List<String> excludeNames);

    // ✅ Count users with a specific role
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countUsersByRoleName(@Param("roleName") String roleName);

    // ✅ Find role by name with permissions eagerly loaded
    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(@Param("name") String name);

    // ✅ Get all role names
    @Query("SELECT r.name FROM Role r")
    List<String> findAllRoleNames();
}