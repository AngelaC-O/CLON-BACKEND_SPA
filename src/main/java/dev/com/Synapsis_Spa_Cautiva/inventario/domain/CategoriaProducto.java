package dev.com.Synapsis_Spa_Cautiva.inventario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "CATEGORIA_PRODUCTO")
@Getter
public class CategoriaProducto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_categoria")
	private Long idCategoria;

	@Column(name = "nombre", nullable = false, length = 40)
	private String nombre;

	protected CategoriaProducto() {
	}

}
