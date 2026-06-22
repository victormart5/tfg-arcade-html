package com.retro.main.repository;

import com.retro.main.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    
    @Query("SELECT u FROM Usuario u ORDER BY " +
           "((CASE WHEN u.puntos_snake > 0 THEN (5000 - u.puntos_snake) ELSE 0 END) + " +
           "u.puntos_2048 + u.puntos_tetris + u.puntos_break + u.puntos_catch) DESC")
    List<Usuario> findRankingGlobal();
}