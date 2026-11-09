package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.math.BigDecimal;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioParametro;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ServicioParametro implements ParametroApi {

	private final RepositorioParametro repositorioParametro;

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

}
