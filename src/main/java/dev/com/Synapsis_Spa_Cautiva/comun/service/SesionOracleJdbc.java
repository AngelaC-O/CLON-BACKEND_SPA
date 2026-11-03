package dev.com.Synapsis_Spa_Cautiva.comun.service;

import dev.com.Synapsis_Spa_Cautiva.comun.api.SesionOracle;
import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SesionOracleJdbc implements SesionOracle {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public void iniciar(Long idUsuario) {
		jdbcTemplate.update(TransaccionConSesionOracle.INICIAR, idUsuario);
	}

}
