package dev.com.Synapsis_Spa_Cautiva.ventas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "NOTA_CREDITO")
@PrimaryKeyJoinColumn(name = "id_nota_credito")
@Getter
public class NotaCredito extends Comprobante {

	@Column(name = "motivo", nullable = false, length = 200)
	private String motivo;

	@Column(name = "codigo_motivo", nullable = false, length = 2)
	private String codigoMotivo;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_devolucion", nullable = false, length = 15)
	private FormaDevolucion formaDevolucion;

	@Column(name = "id_usuario_solicita", nullable = false)
	private Long idUsuarioSolicita;

	@Column(name = "id_usuario_autoriza", nullable = false)
	private Long idUsuarioAutoriza;

	protected NotaCredito() {
	}

}
