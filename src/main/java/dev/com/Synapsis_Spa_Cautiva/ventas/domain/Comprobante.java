package dev.com.Synapsis_Spa_Cautiva.ventas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * Supertipo de NotaCredito. Serie, número y fecha de emisión los asigna TRG_COMPROBANTE_REGLAS (RN47).
 */
@Entity
@Table(name = "COMPROBANTE")
@Inheritance(strategy = InheritanceType.JOINED)
@DynamicUpdate
@Getter
public class Comprobante {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_comprobante")
	private Long idComprobante;

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_venta", nullable = false)
	private Venta venta;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 12)
	private TipoComprobante tipo;

	@Generated(event = EventType.INSERT)
	@Column(name = "serie", insertable = false, updatable = false, length = 4)
	private String serie;

	@Generated(event = EventType.INSERT)
	@Column(name = "numero", insertable = false, updatable = false, length = 8)
	private String numero;

	@Generated(event = EventType.INSERT)
	@Column(name = "fecha_emision", insertable = false, updatable = false)
	private LocalDate fechaEmision;

	@Column(name = "id_tipo_doc_comprador")
	private Long idTipoDocComprador;

	@Column(name = "nro_doc_comprador", length = 15)
	private String nroDocComprador;

	@Column(name = "nombre_comprador", length = 150)
	private String nombreComprador;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_sunat", nullable = false, length = 10)
	private EstadoSunat estadoSunat;

	@Column(name = "intentos_envio", nullable = false)
	private Integer intentosEnvio;

	@Column(name = "fecha_envio")
	private LocalDateTime fechaEnvio;

	@Column(name = "fecha_respuesta")
	private LocalDateTime fechaRespuesta;

	@Column(name = "codigo_respuesta", length = 10)
	private String codigoRespuesta;

	@Column(name = "descripcion_respuesta", length = 500)
	private String descripcionRespuesta;

	@Column(name = "hash_cpe", length = 100)
	private String hashCpe;

	@Column(name = "ruta_xml", length = 300)
	private String rutaXml;

	@Column(name = "ruta_cdr", length = 300)
	private String rutaCdr;

	protected Comprobante() {
	}

	Comprobante(Venta venta, TipoComprobante tipo, DocumentoComprador oDocumentoComprador) {
		this.venta = venta;
		this.tipo = tipo;
		if (oDocumentoComprador != null) {
			this.idTipoDocComprador = oDocumentoComprador.idTipoDocComprador();
			this.nroDocComprador = oDocumentoComprador.nroDocComprador();
			this.nombreComprador = oDocumentoComprador.nombreComprador();
		}
		this.estadoSunat = EstadoSunat.PENDIENTE;
		this.intentosEnvio = 0;
	}

	public void registrarRespuestaSunat(EstadoSunat estadoSunat, String codigoRespuesta, String descripcionRespuesta, String hashCpe,
			String rutaXml, String rutaCdr) {
		LocalDateTime ahora = LocalDateTime.now();
		this.estadoSunat = estadoSunat;
		this.intentosEnvio = intentosEnvio + 1;
		this.fechaEnvio = ahora;
		if (estadoSunat == EstadoSunat.ACEPTADO || estadoSunat == EstadoSunat.OBSERVADO || estadoSunat == EstadoSunat.RECHAZADO) {
			this.fechaRespuesta = ahora;
		}
		this.codigoRespuesta = codigoRespuesta;
		this.descripcionRespuesta = descripcionRespuesta;
		this.hashCpe = hashCpe;
		this.rutaXml = rutaXml;
		this.rutaCdr = rutaCdr;
	}

	public Long getIdVenta() {
		return venta.getIdVenta();
	}

	/** Lo que documenta el comprobante: sin restar el adelanto ni el saldo a favor (DF-52). */
	public BigDecimal getImporteVenta() {
		return venta.getSubtotal().add(venta.getIgv());
	}

	public List<VentaDetalle> getDetallesVenta() {
		return venta.getDetalles();
	}

	public Long getIdUsuarioVenta() {
		return venta.getIdUsuario();
	}

}
