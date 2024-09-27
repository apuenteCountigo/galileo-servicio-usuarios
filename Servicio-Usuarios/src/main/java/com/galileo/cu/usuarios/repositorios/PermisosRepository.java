package com.galileo.cu.usuarios.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import com.galileo.cu.commons.models.Permisos;

@RestResource(exported = false)
public interface PermisosRepository extends CrudRepository<Permisos, Long> {
    @Query("SELECT p FROM Permisos p WHERE p.usuarios.id=:idUsuario")
    List<Permisos> buscarUsuario(long idUsuario);

    @Query("SELECT p FROM Permisos p WHERE p.usuarios.id=:idUsuario "
    +"AND ("
    + "     (p.tipoEntidad.id=6 AND p.idEntidad IN (SELECT id FROM Operaciones WHERE unidades.id=(SELECT unidad.id FROM UnidadesUsuarios WHERE usuario.id=:idUsuarios AND estado.id=6)))"
    +"      OR (p.tipoEntidad.id=8 AND p.idEntidad IN (SELECT id FROM Objetivos WHERE operaciones.unidades.id=(SELECT unidad.id FROM UnidadesUsuarios WHERE usuario.id=:idUsuarios AND estado.id=6)))"
    + ")")
    List<Permisos> buscarUnidad(long idUsuario);
}
