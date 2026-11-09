package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

/** [It. 2] CUS52 (D-06): personal ACTIVO sin cuenta. PersonalApi da el id y el cargo; los nombres los pone PersonaApi (DF-23). */
public record PersonalSinUsuarioDTO(Long idPersonal, String nombres, String apellidos, String cargo) {
}
