package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.NotaCredito;

import org.springframework.data.jpa.repository.JpaRepository;

/** CUS23: guarda la nota de crédito (COMPROBANTE NC_* más su fila en NOTA_CREDITO) y su respuesta de SUNAT. */
public interface RepositorioNotaCredito extends JpaRepository<NotaCredito, Long> {

}
