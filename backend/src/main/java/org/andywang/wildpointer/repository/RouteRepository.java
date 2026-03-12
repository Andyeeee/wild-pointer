package org.andywang.wildpointer.repository;

import org.andywang.wildpointer.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Integer> {
    List<Route> findByUserIdOrderByCreatedAtDesc(Integer userId);
    void deleteByIdAndUserId(Integer id, Integer userId);
}
