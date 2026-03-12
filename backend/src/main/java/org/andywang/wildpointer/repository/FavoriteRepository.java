package org.andywang.wildpointer.repository;

import org.andywang.wildpointer.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Integer userId);
    void deleteByIdAndUserId(Integer id, Integer userId);
}
