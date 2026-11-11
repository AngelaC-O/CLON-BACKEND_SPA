package dev.com.Synapsis_Spa_Cautiva.inventario.repository;

import dev.com.Synapsis_Spa_Cautiva.inventario.domain.CategoriaProducto;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioCategoriaProducto extends JpaRepository<CategoriaProducto, Long> {

	/** RF45: el nombre de la categoría es único (UQ_CATEGORIA_PRODUCTO_NOMBRE). */
	boolean existsByNombre(String nombre);

}
