package dev.com.Synapsis_Spa_Cautiva.pagos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.SiNoConverter;

import lombok.Getter;

@Entity
@Table(name = "MEDIO_PAGO")
@Getter
public class MedioPago {

	private static final String EFECTIVO = "EFECTIVO";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_medio_pago")
	private Long idMedioPago;

	@Column(name = "nombre", nullable = false, length = 15)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoMedioPago estado;

	@Convert(converter = SiNoConverter.class)
	@Column(name = "usa_pasarela", nullable = false, columnDefinition = "CHAR(1)")
	private Boolean usaPasarela;

	protected MedioPago() {
	}

	/** RF88, RN42: el efectivo se cobra y se cuenta en caja, nunca por la pasarela (CK_MEDIO_PAGO_EFECTIVO). */
	public void configurar(EstadoMedioPago estado, Boolean usaPasarela) {
		if (EFECTIVO.equals(nombre) && Boolean.TRUE.equals(usaPasarela)) {
			throw new ReglaNegocioException("RN42", "El efectivo no se cobra por la pasarela.");
		}
		this.estado = estado;
		this.usaPasarela = usaPasarela;
	}

}
