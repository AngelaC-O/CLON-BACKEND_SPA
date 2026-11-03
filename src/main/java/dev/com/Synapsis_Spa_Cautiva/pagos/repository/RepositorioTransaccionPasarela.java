package dev.com.Synapsis_Spa_Cautiva.pagos.repository;

import java.util.Optional;

import dev.com.Synapsis_Spa_Cautiva.pagos.domain.TransaccionPasarela;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioTransaccionPasarela extends JpaRepository<TransaccionPasarela, Long> {

	Optional<TransaccionPasarela> findByCodigoOperacion(String codigoOperacion);

}
