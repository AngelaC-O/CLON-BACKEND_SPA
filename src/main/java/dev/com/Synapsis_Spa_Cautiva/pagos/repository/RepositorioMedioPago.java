package dev.com.Synapsis_Spa_Cautiva.pagos.repository;

import dev.com.Synapsis_Spa_Cautiva.pagos.domain.MedioPago;

import org.springframework.data.jpa.repository.JpaRepository;

/** CUS62: los cuatro medios de pago (CK_MEDIO_PAGO_NOMBRE) y si se cobran por la pasarela (RF88). */
public interface RepositorioMedioPago extends JpaRepository<MedioPago, Long> {

}
