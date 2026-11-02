package dev.com.Synapsis_Spa_Cautiva;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularidadTest {

	@Test
	void verificaLosModulos() {
		ApplicationModules.of(SynapsisSpaCautivaApplication.class).verify();
	}

}
