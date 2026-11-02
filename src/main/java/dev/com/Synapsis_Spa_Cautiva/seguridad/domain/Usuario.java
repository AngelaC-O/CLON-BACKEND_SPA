package dev.com.Synapsis_Spa_Cautiva.seguridad.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.SiNoConverter;

import lombok.Getter;

import org.hibernate.annotations.DynamicUpdate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Cuenta del personal. El bloqueo por intentos lo aplica TRG_USUARIO_REGLAS: Java solo cuenta los intentos.
 */
@Entity
@Table(name = "USUARIO")
@DynamicUpdate
@Getter
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Long idUsuario;

	@Column(name = "id_personal", nullable = false)
	private Long idPersonal;

	@Column(name = "id_rol", nullable = false)
	private Long idRol;

	@Column(name = "nombre_usuario", nullable = false, length = 50)
	private String nombreUsuario;

	@Column(name = "clave_hash", nullable = false, length = 200)
	private String claveHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoUsuario estado;

	@Column(name = "intentos_fallidos", nullable = false)
	private Integer intentosFallidos;

	@Convert(converter = SiNoConverter.class)
	@Column(name = "debe_cambiar_clave", nullable = false, columnDefinition = "CHAR(1)")
	private Boolean debeCambiarClave;

	@Column(name = "bloqueado_hasta")
	private LocalDateTime bloqueadoHasta;

	@Column(name = "ultimo_acceso")
	private LocalDateTime ultimoAcceso;

	@Column(name = "fecha_creacion", updatable = false, nullable = false)
	private LocalDateTime fechaCreacion;

	private static final BCryptPasswordEncoder CODIFICADOR = new BCryptPasswordEncoder();

	protected Usuario() {
	}

	public Usuario(Long idPersonal, Long idRol, String nombreUsuario, String claveHash) {
		this.idPersonal = idPersonal;
		this.idRol = idRol;
		this.nombreUsuario = nombreUsuario;
		this.claveHash = claveHash;
		this.estado = EstadoUsuario.ACTIVO;
		this.intentosFallidos = 0;
		this.debeCambiarClave = Boolean.TRUE;
		this.fechaCreacion = LocalDateTime.now();
	}

	/** Un bloqueo vencido se levanta antes de comparar, para que un nuevo error vuelva a contar (RF77). */
	public boolean autenticar(String clave) {
		if (estado == EstadoUsuario.INACTIVO) {
			return false;
		}
		if (estado == EstadoUsuario.BLOQUEADO) {
			if (bloqueadoHasta != null && LocalDateTime.now().isBefore(bloqueadoHasta)) {
				return false;
			}
			estado = EstadoUsuario.ACTIVO;
			intentosFallidos = 0;
			bloqueadoHasta = null;
		}
		return CODIFICADOR.matches(clave, claveHash);
	}

	public void registrarIntentoFallido() {
		intentosFallidos = intentosFallidos + 1;
	}

	public void registrarAcceso(LocalDateTime ultimoAcceso) {
		this.ultimoAcceso = ultimoAcceso;
		this.intentosFallidos = 0;
	}

	public void asignarRol(Long idRol) {
		this.idRol = idRol;
	}

}
