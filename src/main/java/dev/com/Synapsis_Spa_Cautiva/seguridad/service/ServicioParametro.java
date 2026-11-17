package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.math.BigDecimal;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.seguridad.controller.ParametroDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.controller.TipoDocumentoDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Parametro;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.TipoDocumento;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioParametro;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioTipoDocumento;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementa ParametroApi (SPI de comun): los demás módulos leen los parámetros sin depender de seguridad. [It. 2 · CUS65, RF87, RN45] Además,
 * la Administradora cambia sus valores y configura los tipos de documento; los códigos y los tipos los carga el script y no se crean ni se
 * borran. El cambio lo audita TRG_AUD_PARAMETRO (RN29).
 */
@Service
@RequiredArgsConstructor
public class ServicioParametro implements ParametroApi {

	private final RepositorioParametro repositorioParametro;

	private final UsuarioActual usuarioActual;

	private final RepositorioTipoDocumento repositorioTipoDocumento;

	@Override
	@Transactional(readOnly = true)
	public BigDecimal valorNumero(String codigo) {
		return obtenerValorNumero(codigo);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal obtenerValorNumero(String codigo) {
		return repositorioParametro.findByCodigo(codigo)
			.map(parametro -> parametro.getValorNumero())
			.orElseThrow(() -> new IllegalStateException("Falta el parámetro numérico " + codigo));
	}

	@Transactional(readOnly = true)
	public List<ParametroDTO> listarParametros() {
		return repositorioParametro.findAll(Sort.by("codigo")).stream().map(ServicioParametro::aDTO).toList();
	}

	/** RF87: solo cambia el valor (CK_PARAMETRO_VALOR); AGENDA_PASO_MIN tiene su rango (RN51). Código inexistente (también LIMPIEZA_MAX_MIN, retirado en v5.2): 404. */
	@Transactional
	public ParametroDTO cambiarValor(String codigo, ParametroDTO oParametroDTO) {
		Long idUsuario = usuarioActual.idUsuario();
		Parametro oParametro = repositorioParametro.findByCodigo(codigo)
			.orElseThrow(() -> new ReglaNegocioException("RF87", "El parámetro " + codigo + " no existe: los códigos no se crean.", HttpStatus.NOT_FOUND));
		oParametro.cambiarValor(oParametroDTO.valorNumero(), oParametroDTO.valorTexto(), oParametroDTO.fechaVigencia(), idUsuario);
		return aDTO(repositorioParametro.saveAndFlush(oParametro));
	}

	/** RN45: lo usan las pantallas de CUS02, CUS04, CUS19, CUS28 y CUS36 en lugar de los id fijos. */
	@Transactional(readOnly = true)
	public List<TipoDocumentoDTO> listarTiposDocumento() {
		return repositorioTipoDocumento.findAll(Sort.by("idTipoDocumento")).stream().map(ServicioParametro::aDTO).toList();
	}

	/** RN45: se activa o desactiva el tipo y se marca si se consulta en APISPERU (solo DNI y RUC). */
	@Transactional
	public TipoDocumentoDTO configurarTipoDocumento(Long idTipoDocumento, TipoDocumentoDTO oTipoDocumentoDTO) {
		TipoDocumento oTipoDocumento = repositorioTipoDocumento.findById(idTipoDocumento)
			.orElseThrow(() -> new ReglaNegocioException("RN45", "El tipo de documento " + idTipoDocumento + " no existe.", HttpStatus.NOT_FOUND));
		oTipoDocumento.configurar(oTipoDocumentoDTO.consultaApi(), oTipoDocumentoDTO.estado());
		return aDTO(repositorioTipoDocumento.saveAndFlush(oTipoDocumento));
	}

	private static ParametroDTO aDTO(Parametro oParametro) {
		return new ParametroDTO(oParametro.getIdParametro(), oParametro.getCodigo(), oParametro.getDescripcion(), oParametro.getValorNumero(),
				oParametro.getValorTexto(), oParametro.getFechaVigencia(), oParametro.getIdUsuario());
	}

	private static TipoDocumentoDTO aDTO(TipoDocumento oTipoDocumento) {
		return new TipoDocumentoDTO(oTipoDocumento.getIdTipoDocumento(), oTipoDocumento.getCodigoSunat(), oTipoDocumento.getNombre(),
				oTipoDocumento.getAbreviatura(), oTipoDocumento.getLongitudMin(), oTipoDocumento.getLongitudMax(), oTipoDocumento.getSoloNumeros(),
				oTipoDocumento.getConsultaApi(), oTipoDocumento.getEstado());
	}

}
