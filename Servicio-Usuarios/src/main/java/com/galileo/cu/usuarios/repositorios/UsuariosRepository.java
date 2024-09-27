package com.galileo.cu.usuarios.repositorios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.galileo.cu.commons.models.Usuarios;

@CrossOrigin
//@RepositoryRestResource(collectionResourceRel = "usuarios", path = "usuarios",excerptProjection=UsuariosProjection.class)
@RepositoryRestResource(collectionResourceRel = "usuarios", path = "usuarios")
public interface UsuariosRepository extends PagingAndSortingRepository<Usuarios, Long> {
	
	/*@RestResource(path = "listar")
	public Page<Usuarios> findAllByOrderByIdAsc();*/
	
	@RestResource(path = "buscar-nombre")
	public Usuarios findByNombre(@Param("nombre") String nombre);// , Pageable p

	@RestResource(path = "buscarTip")
	public Usuarios findByTip(String tip);
	
	@RestResource(exported = false)
	@Query("SELECT u FROM Usuarios u WHERE u.tip=:tip AND u.id!=:id ")
	public List<Usuarios>  findByTipDist(String tip, long id);
	
	@RestResource(exported = false)
	public List<Usuarios>  findByEmail(String email);
	
	@RestResource(exported = false)
	@Query("SELECT u FROM Usuarios u WHERE u.email=:email AND u.id!=:id ")
	public List<Usuarios>  findByEmailDist(String email, long id);

	@Query("SELECT u FROM Usuarios u WHERE " 
			+ " ( "
			+ "		(:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id=2) "
			+ "			AND u.id IN (SELECT usuario.id FROM UnidadesUsuarios WHERE unidad.id=(SELECT unidad.id FROM UnidadesUsuarios WHERE usuario.id=:idAuth AND estado.id=6))"
			+ "		) "
			+ "	OR (:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id=1)) "
			+ ") "
			+ "AND ((:fechaFin!=null and :fechaInicio!=null and u.fechaCreacion between :fechaInicio and :fechaFin) "
			+ "or (:fechaFin=null and :fechaInicio!=null and u.fechaCreacion >=:fechaInicio) "
			+ "or (:fechaFin=null and :fechaInicio=null)) "
			+ "and (:nombre='' or u.nombre like %:nombre%) "
			+ "and (:apellidos='' or u.apellidos like %:apellidos%) " 
			+ "and (:tip='' or u.tip like %:tip%) "
			+ "and (:email='' or u.email = :email) "
			+ "and (:perfil=0 or u.perfil.id = :perfil)")
	public Page<Usuarios> buscarUsuarios(long idAuth, String email, String nombre, String apellidos, int perfil,String tip,@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin, Pageable p);
	
	@Query("SELECT u FROM UnidadesUsuarios uu JOIN uu.usuario u WHERE uu.unidad.id = :idunidad "
			+"and (:tip='' or u.tip like %:tip%) and (:nombre='' or u.nombre like %:nombre%) and (:apellidos='' or u.apellidos like %:apellidos%)" )
	public Page<Usuarios> asignados(long idunidad, String tip, String nombre, String apellidos, Pageable p);
	
	@Query("SELECT us FROM Usuarios us WHERE "
			+ "us.id NOT IN (SELECT DISTINCT u FROM UnidadesUsuarios uu LEFT JOIN uu.usuario u WHERE uu.unidad.id = :idunidad) "
			+ "AND (:asignar='' OR (:asignar='PERMANENTE' AND us.id NOT IN (SELECT un.usuario.id FROM UnidadesUsuarios un WHERE un.unidad.id != :idunidad AND un.estado.id=6))) "
			+ "AND us.perfil.id!=1 "
			+ "AND (:tip='' or us.tip like %:tip%) "
			+ "AND (:nombre='' or us.nombre like %:nombre%) "
			+ "AND (:apellidos='' or us.apellidos like %:apellidos%)" )
	public Page<Usuarios> sinasignar(long idunidad, String tip, String nombre, String apellidos, String asignar, Pageable p);

	public Usuarios getOne(Long id);	
}
