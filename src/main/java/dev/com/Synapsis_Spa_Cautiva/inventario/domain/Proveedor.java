package dev.com.Synapsis_Spa_Cautiva.inventario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

/**
 * Subtipo de PERSONA (seguridad): idProveedor = idPersona. Solo estructura en la Iteración 1.
 */
@Entity
@Table(name = "PROVEEDOR")
@Getter
public class Proveedor {

	@Id
	@Column(name = "id_proveedor")
	private Long idProveedor;

	@Column(name = "contacto", length = 80)
	private String contacto;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoProveedor estado;

	protected Proveedor() {
	}

}
