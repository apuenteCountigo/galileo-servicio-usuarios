package com.galileo.cu.usuarios.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import com.galileo.cu.commons.models.UnidadesUsuarios;

@RestResource(exported = false)
public interface UnidadesUsuariosRepository extends CrudRepository<UnidadesUsuarios, Long> {
    @Query("SELECT uu FROM UnidadesUsuarios uu WHERE uu.usuario.id=:idUsuario")
    List<UnidadesUsuarios> buscarUsuario(long idUsuario);
}
