package com.galileo.cu.usuarios.cliente;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.galileo.cu.commons.models.Usuarios;

@FeignClient(name = "servicio-apis")
public interface TraccarFeign {

	@PostMapping("/salvarUsuarioTraccar")
	public Usuarios salvar(@RequestBody Usuarios usuario);

	@PutMapping("/updateUsuarioTraccar")
	public Usuarios actualizarUsuario(@RequestBody Usuarios usuario);

	@DeleteMapping("/borrarUsuarioTraccar")
	String borrar(@RequestBody Usuarios usuario);

	@PostMapping("/permPerfTraccar")
	public String permisosTraccarCambioPerfiles(@RequestBody Usuarios usuario, @RequestParam String cambioPefil);

	@PostMapping("/establecerPermisosInicialesUsuarioNuevoTraccar")
	String establecerPermisosInicialesUsuarioNuevoTraccar(@RequestBody Usuarios usuarios);
}
