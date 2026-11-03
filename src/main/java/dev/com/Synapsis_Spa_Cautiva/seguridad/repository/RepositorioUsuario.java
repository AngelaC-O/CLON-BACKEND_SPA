package dev.com.Synapsis_Spa_Cautiva.seguridad.repository;

import java.util.List;
import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Permiso;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Rol;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioUsuario extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByNombreUsuario(String nombreUsuario);

	boolean existsByNombreUsuario(String nombreUsuario);

	/** Con el reloj de Oracle: TRG_USUARIO_REGLAS escribe bloqueado_hasta con SYSTIMESTAMP, que puede estar en otra zona que Java. */
	@Query(nativeQuery = true, value = """
			SELECT COUNT(*) FROM USUARIO
			WHERE id_usuario = :idUsuario AND estado = 'BLOQUEADO'
			AND (bloqueado_hasta IS NULL OR bloqueado_hasta > CAST(SYSTIMESTAMP AS TIMESTAMP))
			""")
	long contarBloqueoVigente(@Param("idUsuario") Long idUsuario);

	default boolean tieneBloqueoVigente(Long idUsuario) {
		return contarBloqueoVigente(idUsuario) > 0;
	}

	@Query("select p from Permiso p join RolPermiso rp on rp.id.idPermiso = p.idPermiso where rp.id.idRol = :idRol")
	List<Permiso> findPermisosPorRol(@Param("idRol") Long idRol);

	@Query("select r from Rol r where r.estado = dev.com.Synapsis_Spa_Cautiva.seguridad.domain.EstadoRol.ACTIVO")
	List<Rol> findRolesActivos();

}
