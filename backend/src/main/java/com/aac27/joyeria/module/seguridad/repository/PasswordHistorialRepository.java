package com.aac27.joyeria.module.seguridad.repository;

import com.aac27.joyeria.module.seguridad.entity.PasswordHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistorialRepository extends JpaRepository<PasswordHistorial, Long> {

    /** Las 5 contraseñas más recientes del usuario (la primera es la vigente). */
    List<PasswordHistorial> findTop5ByUsuarioIdOrderByHistorialIdDesc(Long usuarioId);
}