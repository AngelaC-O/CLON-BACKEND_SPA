package dev.com.Synapsis_Spa_Cautiva.seguridad.repository;

import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.TipoDocumento;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioTipoDocumento extends JpaRepository<TipoDocumento, Long> {

	Optional<TipoDocumento> findByCodigoSunat(String codigoSunat);

}
