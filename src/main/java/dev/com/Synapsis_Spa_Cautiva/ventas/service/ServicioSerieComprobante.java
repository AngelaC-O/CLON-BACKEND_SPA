package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.SerieDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.SerieComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioSerieComprobante;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [It. 2 · CUS63, RF107, RN47] Las series B001, F001, BC01 y FC01 se registran una vez por sede; el correlativo lo lleva PKG_VENTA. Una segunda
 * serie ACTIVA del mismo tipo en la sede (UQ_SERIE_COMPR_ACTIVA) o una serie repetida (UQ_SERIE_COMPROBANTE_SERIE) es 409: lo responde Oracle en
 * el saveAndFlush.
 */
@Service
@RequiredArgsConstructor
public class ServicioSerieComprobante {

	private final RepositorioSerieComprobante repositorioSerieComprobante;

	private final UsuarioActual usuarioActual;

	@Transactional(readOnly = true)
	public List<SerieDTO> listarSeries(Long idSede) {
		return repositorioSerieComprobante.findBySede(idSede).stream().map(ServicioSerieComprobante::aDTO).toList();
	}

	@Transactional
	public SerieDTO registrarSerie(SerieDTO oSerieDTO) {
		Long idSede = oSerieDTO.idSede() == null ? usuarioActual.idSede() : oSerieDTO.idSede();
		String serie = oSerieDTO.serie() == null ? null : oSerieDTO.serie().trim().toUpperCase();
		SerieComprobante oSerie = new SerieComprobante(idSede, oSerieDTO.tipo(), serie);
		return aDTO(repositorioSerieComprobante.saveAndFlush(oSerie));
	}

	private static SerieDTO aDTO(SerieComprobante oSerie) {
		return new SerieDTO(oSerie.getIdSerie(), oSerie.getIdSede(), oSerie.getTipo(), oSerie.getSerie(), oSerie.getUltimoNumero(), oSerie.getEstado());
	}

}
