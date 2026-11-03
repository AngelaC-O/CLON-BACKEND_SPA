package dev.com.Synapsis_Spa_Cautiva.comun.service;

import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Identifica al usuario en Oracle en cada transacción: sin esto, los triggers de auditoría responden -20001.
 * Corre en la misma conexión y antes de cualquier sentencia de la transacción.
 */
@Slf4j
class TransaccionConSesionOracle extends JpaTransactionManager {

	static final String INICIAR = "BEGIN PKG_SESION.iniciar(?); END;";

	private static final String TERMINAR = "BEGIN PKG_SESION.terminar; END;";

	private final transient UsuarioActual usuarioActual;

	TransaccionConSesionOracle(EntityManagerFactory entityManagerFactory, UsuarioActual usuarioActual) {
		super(entityManagerFactory);
		this.usuarioActual = usuarioActual;
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		super.doBegin(transaction, definition);
		try {
			usuarioActual.buscarIdUsuario().ifPresent(idUsuario -> jdbc().update(INICIAR, idUsuario));
		}
		catch (RuntimeException ex) {
			liberarTrasFallo(transaction);
			throw ex;
		}
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		try {
			jdbc().execute(TERMINAR);
		}
		catch (DataAccessException ex) {
			log.error("No se pudo llamar a PKG_SESION.terminar", ex);
		}
		super.doCleanupAfterCompletion(transaction);
	}

	/** Spring no limpia una transacción que falla al empezar: sin esto, la conexión no vuelve al pool. */
	private void liberarTrasFallo(Object transaction) {
		EntityManagerHolder holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(obtainEntityManagerFactory());
		if (holder != null && holder.getEntityManager().getTransaction().isActive()) {
			holder.getEntityManager().getTransaction().rollback();
		}
		super.doCleanupAfterCompletion(transaction);
	}

	private JdbcTemplate jdbc() {
		return new JdbcTemplate(getDataSource());
	}

}
