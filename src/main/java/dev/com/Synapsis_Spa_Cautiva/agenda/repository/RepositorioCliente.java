package dev.com.Synapsis_Spa_Cautiva.agenda.repository;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cliente;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioCliente extends JpaRepository<Cliente, Long> {

}
