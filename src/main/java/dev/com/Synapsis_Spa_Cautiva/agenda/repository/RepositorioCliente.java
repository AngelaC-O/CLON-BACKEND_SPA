package dev.com.Synapsis_Spa_Cautiva.agenda.repository;

import java.math.BigDecimal;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioCliente extends JpaRepository<Cliente, Long> {

	/** RN05: el saldo no se guarda, lo suma V_SALDO_FAVOR_CLIENTE desde SALDO_FAVOR_MOV (0 si la clienta no tiene movimientos). */
	@Query(nativeQuery = true, value = "SELECT NVL(MAX(saldo), 0) FROM V_SALDO_FAVOR_CLIENTE WHERE id_cliente = :idCliente")
	BigDecimal saldoFavorDisponible(@Param("idCliente") Long idCliente);

}
