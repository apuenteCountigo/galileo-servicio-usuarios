package com.galileo.cu.usuarios.controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galileo.cu.commons.models.AccionEntidad;
import com.galileo.cu.commons.models.Permisos;
import com.galileo.cu.commons.models.TipoEntidad;
import com.galileo.cu.commons.models.Trazas;
import com.galileo.cu.commons.models.UnidadesUsuarios;
import com.galileo.cu.commons.models.Usuarios;
import com.galileo.cu.commons.models.dto.ErrorFeign;
import com.galileo.cu.usuarios.cliente.TraccarFeign;
import com.galileo.cu.usuarios.repositorios.PermisosRepository;
import com.galileo.cu.usuarios.repositorios.TrazasRepository;
import com.galileo.cu.usuarios.repositorios.UnidadesUsuariosRepository;
import com.galileo.cu.usuarios.repositorios.UsuariosRepository;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Component
@RepositoryEventHandler(Usuarios.class)
public class UsuariosEventHandler {

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	private TraccarFeign traccar;

	@Autowired
	private UsuariosRepository userRepository;

	@Autowired
	EntityManager entMg;

	@Autowired
	HttpServletRequest req;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	TrazasRepository trazasRepo;

	@Autowired
	UnidadesUsuariosRepository uniusu;

	@Autowired
	PermisosRepository permisos;

	String descripcionTraza;

	@HandleBeforeCreate
	public void handleUserCreate(Usuarios user) {
		/* Validando Autorización */
		try {
			ValidateAuthorization val = new ValidateAuthorization();
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				log.error("Fallo el Usuario Enviado no Coincide con el Autenticado ");
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado ");
			}
		} catch (Exception e) {
			log.error("Fallo Antes de Crear Usuario Validando Autorización: ", e.getMessage());
			throw new RuntimeException("Fallo Antes de Crear Usuario Validando Autorización: " + e.getMessage());
		}

		entMg.detach(user);

		/* VALIDACIONES */
		List<Usuarios> emails = userRepository.findByEmail(user.getEmail());
		Usuarios tips = userRepository.findByTip(user.getTip());

		if (tips != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fallo, Tip Existente", null);
		} else if (emails.size() > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fallo, Correo Electrónico Existente", null);
		}

		user.setPassword(passwordEncoder.encode(user.getPassword()));
		try {
			Usuarios usuarioUpdate = traccar.salvar(user);
			user.setTraccarID(usuarioUpdate.getTraccarID());
			user.setTraccar(usuarioUpdate.getTraccar());

		} catch (Exception e) {
			String err = "Fallo al Insertar Usuario en Traccar, Ver logs, contacte a su administrador.";
			if (e.getMessage().contains("\"message\": \"")) {
				ErrorFeign errorFeign = new ErrorFeign();

				try {
					errorFeign = objectMapper.readValue(e.getMessage(), ErrorFeign.class);
				} catch (JsonMappingException e1) {
					err = "Fallo mapeando el objeto de error enviado por apis, al intentar insertar un usuario en Traccar.";
				} catch (JsonProcessingException e1) {
					err = "Fallo procesando la deserialización del error enviado por apis, al intentar insertar un usuario en Traccar.";
				}
				err = errorFeign.getMessage();
			} else if (e.getMessage().contains("Fallo"))
				err = e.getMessage();

			log.error(err, e.getMessage());
			throw new RuntimeException(err);
		}
	}

	@HandleAfterCreate
	public void handleUserAfterCreate(Usuarios user) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado ");
			}
		} catch (Exception e) {
			log.error("Fallo Despues de Crear Usuario Validando Autorización: ", e.getMessage());
			throw new RuntimeException("Fallo Despues de Crear Usuario Validando Autorización: " + e.getMessage());
		}

		try {
			Trazas traza = new Trazas();
			AccionEntidad accion = new AccionEntidad();
			Usuarios usuario = new Usuarios();
			TipoEntidad entidad = new TipoEntidad();

			entidad.setId(2);
			accion.setId(1);
			usuario.setId(Long.parseLong(val.getJwtObjectMap().getId()));

			traza.setAccionEntidad(accion);
			traza.setTipoEntidad(entidad);
			traza.setUsuario(usuario);
			traza.setIdEntidad(user.getId().intValue());
			traza.setDescripcion("Fue Creado un Nuevo Usuario con TIP: " + user.getTip());
			trazasRepo.save(traza);

		} catch (Exception e) {
			log.error("Fallo al Insertar la Creación del Usuario en la Trazabilidad", e.getMessage());
			throw new RuntimeException("Fallo al Insertar Creación del Usuario en la Trazabilidad");
		}

		// ESTO FUE INCLUIDO POR RAFAEL
		try {
			traccar.establecerPermisosInicialesUsuarioNuevoTraccar(user);
		} catch (Exception exception) {
			log.error("Fallo estableciendo permisos de usuario nuevo en traccar:  ", exception.getMessage());
			throw new RuntimeException("Fallo estableciendo permisos de usuario nuevo en traccar");
		}
	}

	@HandleBeforeSave
	public void handleUserUpdate(Usuarios user) {
		descripcionTraza = "";
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado ");
			}
		} catch (Exception e) {
			log.error("Fallo Despues de Salvar Usuario Validando Autorización: ", e.getMessage());
			throw new RuntimeException("Fallo Despues de Salvar Usuario Validando Autorización: ");
		}

		entMg.detach(user);

		List<Usuarios> emails = userRepository.findByEmailDist(user.getEmail(), user.getId());
		List<Usuarios> tips = userRepository.findByTipDist(user.getTip(), user.getId());

		if (tips.size() > 0) {
			// throw new RuntimeException("Fallo Tip Existente ");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fallo, Tip Existente", null);
		} else if (emails.size() > 0) {
			// throw new RuntimeException("Fallo Email Existente ");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fallo, Correo Electrónico Existente", null);
		}

		if (user.getPassword() != null && user.getPassword() != ""
				&& req.getMethod().equals("PATCH")) {
			descripcionTraza = "Fue Modificada la Contraseña del Usuario con TIP: " + user.getTip();
			try {
				traccar.actualizarUsuario(user);
			} catch (Exception e) {
				log.error("Fallo, Actualizando Contraseña de Usuario en Traccar: ", e.getMessage());
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Fallo Actualizando Contraseña de Usuario en Traccar", null);
			}
			user.setPassword(passwordEncoder.encode(user.getPassword()));

		} else {
			// keeps the last password
			user.setPassword(null);
			Usuarios storedUser = userRepository.findById(user.getId()).get();
			user.setPassword(storedUser.getPassword());
		}

		Usuarios userTmp = userRepository.findById(user.getId()).get();
		if (user.getPerfil() != userTmp.getPerfil()) {
			descripcionTraza = "Fue Modificado el Perfil del Usuario con TIP: " + user.getTip();
			String abreviaturasPerfiles = "";
			if (userTmp.getPerfil().getDescripcion() == "Super Administrador") {
				abreviaturasPerfiles = "sa_";
				if (user.getPerfil().getDescripcion() == "Administrador de Unidad") {
					abreviaturasPerfiles += "au";
				} else if (user.getPerfil().getDescripcion() == "Usuario Final") {
					abreviaturasPerfiles += "uf";
				} else if (user.getPerfil().getDescripcion() == "Invitado Externo") {
					abreviaturasPerfiles += "ie";
				}
			} else if (userTmp.getPerfil().getDescripcion() == "Administrador de Unidad") {
				abreviaturasPerfiles = "au_";
				if (user.getPerfil().getDescripcion() == "Super Administrador") {
					abreviaturasPerfiles += "sa";
					List<UnidadesUsuarios> uu = uniusu.buscarUsuario(user.getId());
					if (uu != null) {
						uniusu.deleteAll(uu);
					}
					user.setUnidad(null);
					List<Permisos> p = permisos.buscarUsuario(user.getId());
					if (p != null) {
						permisos.deleteAll(p);
					}
				} else if (user.getPerfil().getDescripcion() == "Usuario Final") {
					abreviaturasPerfiles += "uf";
				} else if (user.getPerfil().getDescripcion() == "Invitado Externo") {
					abreviaturasPerfiles += "ie";
				}
			} else if (userTmp.getPerfil().getDescripcion() == "Usuario Final") {
				abreviaturasPerfiles = "uf_";
				if (user.getPerfil().getDescripcion() == "Super Administrador") {
					abreviaturasPerfiles += "sa";
					List<UnidadesUsuarios> uu = uniusu.buscarUsuario(user.getId());
					if (uu != null) {
						uniusu.deleteAll(uu);
					}
					user.setUnidad(null);
					List<Permisos> p = permisos.buscarUsuario(user.getId());
					if (p != null) {
						permisos.deleteAll(p);
					}
				} else if (user.getPerfil().getDescripcion() == "Administrador de Unidad") {
					abreviaturasPerfiles += "au";
					List<Permisos> p = permisos.buscarUnidad(user.getId());
					if (p != null) {
						permisos.deleteAll(p);
					}
				} else if (user.getPerfil().getDescripcion() == "Invitado Externo") {
					abreviaturasPerfiles += "ie";
				}
			} else if (userTmp.getPerfil().getDescripcion() == "Invitado Externo") {
				abreviaturasPerfiles = "ie_";
				if (user.getPerfil().getDescripcion() == "Super Administrador") {
					abreviaturasPerfiles += "sa";
					List<UnidadesUsuarios> uu = uniusu.buscarUsuario(user.getId());
					if (uu != null) {
						uniusu.deleteAll(uu);
					}
					user.setUnidad(null);
					List<Permisos> p = permisos.buscarUsuario(user.getId());
					if (p != null) {
						permisos.deleteAll(p);
					}
				} else if (user.getPerfil().getDescripcion() == "Administrador de Unidad") {
					abreviaturasPerfiles += "au";
					List<Permisos> p = permisos.buscarUnidad(user.getId());
					if (p != null) {
						permisos.deleteAll(p);
					}
				} else if (user.getPerfil().getDescripcion() == "Usuario Final") {
					abreviaturasPerfiles += "uf";
				}
			}

			try {
				traccar.permisosTraccarCambioPerfiles(user, abreviaturasPerfiles);
			} catch (Exception e) {
				log.error("Fallo Enviando Cambio de Perfil a Traccar", e.getMessage());
				throw new RuntimeException("Fallo Enviando Cambio de Perfil a Traccar");
			}
		}
	}

	@HandleAfterSave
	public void handleUserAfterUpdate(Usuarios user) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				log.error("Fallo el Usuario Enviado no Coincide con el Autenticado ");
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado ");
			}
		} catch (Exception e) {
			log.error("Fallo Despues de Salvar Usuario Validando Autorización: ", e.getMessage());
			throw new RuntimeException("Fallo Despues de Salvar Usuario Validando Autorización: ");
		}

		try {
			if (Strings.isNullOrEmpty(descripcionTraza)) {
				descripcionTraza = "Fue Actualizado el Usuario con TIP: " + user.getTip();
			}
			ActualizarTraza(val, user.getId().intValue(), 2, 3, descripcionTraza,
					"Fallo al Actualizar el Usuario en la Trazabilidad");

		} catch (Exception e) {
			log.error("Fallo al Actualizar el Usuario en la Trazabilidad", e.getMessage());
			throw new RuntimeException("Fallo al Actualizar el Usuario en la Trazabilidad");
		}
	}

	@HandleBeforeDelete
	public void handleUserDelete(Usuarios user) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado ");
			}
		} catch (Exception e) {
			log.error("Fallo Antes de Eliminar Usuario Validando Autorización: ", e.getMessage());
			throw new RuntimeException("Fallo Antes de Eliminar Usuario Validando Autorización: ");
		}

		try {
			traccar.borrar(user);
		} catch (Exception e) {
			log.error("Fallo al eliminar Usuario en Traccar ", e.getMessage());
			throw new RuntimeException("Fallo al eliminar Usuario en Traccar ");
		}
	}

	@HandleAfterDelete
	public void handleUserAfterDelete(Usuarios user) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado ");
			}
		} catch (Exception e) {
			log.error("Fallo Despues de Eliminar Usuario Validando Autorización: ", e.getMessage());
			throw new RuntimeException("Fallo Despues de Eliminar Usuario Validando Autorización: ");
		}

		try {
			Trazas traza = new Trazas();
			AccionEntidad accion = new AccionEntidad();
			Usuarios usuario = new Usuarios();
			TipoEntidad entidad = new TipoEntidad();

			entidad.setId(2);
			accion.setId(2);
			usuario.setId(Long.parseLong(val.getJwtObjectMap().getId()));

			traza.setAccionEntidad(accion);
			traza.setTipoEntidad(entidad);
			traza.setUsuario(usuario);
			traza.setIdEntidad(user.getId().intValue());
			traza.setDescripcion("Fue Eliminado el Usuario con TIP: " + user.getTip());
			trazasRepo.save(traza);

		} catch (Exception e) {
			log.error("Fallo al Insertar Eliminación del Usuario en la Trazabilidad", e.getMessage());
			throw new RuntimeException("Fallo al Insertar Eliminación del Usuario en la Trazabilidad");
		}
	}

	private void ActualizarTraza(ValidateAuthorization val, int idEntidad, int idTipoEntidad,
			int idAccion, String trazaDescripcion, String errorMessage) {
		try {
			System.out.println("Actualizar Traza " + trazaDescripcion);
			Trazas traza = new Trazas();
			AccionEntidad accion = new AccionEntidad();
			Usuarios usuario = new Usuarios();
			TipoEntidad entidad = new TipoEntidad();

			entidad.setId(idTipoEntidad);
			accion.setId(idAccion);
			usuario.setId(Long.parseLong(val.getJwtObjectMap().getId()));

			traza.setAccionEntidad(accion);
			traza.setTipoEntidad(entidad);
			traza.setUsuario(usuario);
			traza.setIdEntidad(idEntidad);
			traza.setDescripcion(trazaDescripcion);
			trazasRepo.save(traza);
		} catch (Exception e) {
			log.error(errorMessage, e.getMessage());
			throw new RuntimeException(errorMessage);
		}
	}
}
