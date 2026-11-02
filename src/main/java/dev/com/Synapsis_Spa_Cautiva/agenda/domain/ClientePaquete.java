package dev.com.Synapsis_Spa_Cautiva.agenda.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "CLIENTE_PAQUETE")
@Getter
public class ClientePaquete {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_cliente_paquete")
	private Long idClientePaquete;

	@Column(name = "id_cliente", nullable = false)
	private Long idCliente;

	@Column(name = "id_paquete", nullable = false)
	private Long idPaquete;

	@Column(name = "id_detalle", nullable = false)
	private Long idDetalle;

	@Column(name = "sesiones_totales", nullable = false)
	private Integer sesionesTotales;

	@Column(name = "fecha_compra", nullable = false)
	private LocalDate fechaCompra;

	@Column(name = "fecha_vencimiento")
	private LocalDate fechaVencimiento;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoClientePaquete estado;

	protected ClientePaquete() {
	}

	/** RN36 y RN37: las sesiones usadas no superan las compradas, con el paquete activo y vigente. */
	public boolean verificarSesionDisponible(Integer sesionesUsadas) {
		boolean vigente = fechaVencimiento == null || !fechaVencimiento.isBefore(LocalDate.now());
		return estado == EstadoClientePaquete.ACTIVO && vigente && sesionesUsadas < sesionesTotales;
	}

}
