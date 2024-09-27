package com.galileo.cu.usuarios.interceptores;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galileo.cu.commons.models.dto.JwtObjectMap;
import com.galileo.cu.usuarios.repositorios.UsuariosRepository;
import com.google.common.base.Strings;

@Component
public class UsuariosInterceptor implements HandlerInterceptor {
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UsuariosRepository usu;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		System.out.println("USUARIOS INTERCEPTOR***" + request.getMethod() + "*******************");

		if (request.getMethod().equals("GET")) {
			System.out.println("Authorization: "+request.getHeader("Authorization"));
			if (!Strings.isNullOrEmpty(request.getHeader("Authorization"))) {
				String token = request.getHeader("Authorization").replace("Bearer ", "");
				System.out.println(token.toString());
				
				try {
					String[] chunks = token.split("\\.");
					Base64.Decoder decoder = Base64.getUrlDecoder();
					String header = new String(decoder.decode(chunks[0]));
					String payload = new String(decoder.decode(chunks[1]));

					System.out.println(payload.toString());

					JwtObjectMap jwtObjectMap = objectMapper.readValue(payload.toString().replace("Perfil", "perfil"),
							JwtObjectMap.class);
					System.out.println(jwtObjectMap.getId());

					System.out.println("Path:" + request.getRequestURI());
					System.out.println("Descripcion:" + jwtObjectMap.getPerfil().getDescripcion());
					
					if ((request.getRequestURI().equals("/usuarios/search/buscarUsuarios")
							|| request.getRequestURI().equals("/usuarios/search/asignados")
							|| request.getRequestURI().equals("/usuarios/search/findByNombre")
							|| request.getRequestURI().equals("/usuarios/search/sinasignar"))
							&& (jwtObjectMap.getPerfil().getDescripcion().equals("Usuario Final")
									|| jwtObjectMap.getPerfil().getDescripcion().equals("Invitado Externo")
									|| jwtObjectMap.getPerfil().getDescripcion().equals("Administrador de Unidad"))) {
						
						System.out.println("-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
						System.out.println("id parametro: " + request.getParameter("idAuth"));
						
						if (jwtObjectMap.getId().equals(request.getParameter("idAuth"))) {
							return true;
						} else {
							System.out.println("EL USUARIO ENVIADO NO COINCIDE CON EL AUTENTICADO");
							response.resetBuffer();
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setHeader("Content-Type", "application/json;charset=UTF-8");
							response.getOutputStream()
									.write("{\"errorMessage\":\"EL USUARIO ENVIADO NO COINCIDE CON EL AUTENTICADO!\"}"
											.getBytes("UTF-8"));
							response.flushBuffer();

							return false;
						}
					}else {
						System.out.println("El Usuario es Super Administrador o Esta Accediendo a rutas Libres");
					}
				} catch (Exception e) {
					System.out.println("NO HAY TOKEN");
					response.resetBuffer();
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setHeader("Content-Type", "application/json;charset=UTF-8");
					String s = "{\"errorMessage\":\"ERROR en Interceptor de Seguriad Servicio-Unidades\",\"errorOficial\":\""
							+ e.getMessage() + "\"}";
					response.getOutputStream().write(s.getBytes("UTF-8"));
					response.flushBuffer();
					return false;
				}

			} else {
				System.out.println("NO HAY TOKEN");
				//buscarTip 
				if (request.getRequestURI().equals("/usuarios/search/buscarTip") || request.getRequestURI().equals("/usuarios/search/buscarTip")) {
					return true;
				}
				response.resetBuffer();
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setHeader("Content-Type", "application/json;charset=UTF-8");
				String s="{\"errorMessage\":\"Necesita enviar un Token Válido "+request.getMethod()+" Servicio-Usuarios!\"}";
				response.getOutputStream().write(s.getBytes("UTF-8"));
				response.flushBuffer();

				return false;
			}
		} else if (request.getMethod().equals("DELETE")) {
			
		} else {
			System.out.println("NO GET");
		}
		return true;
	}

	private HttpServletResponse Msg(HttpServletResponse res, String msg) {

		try {
			res.reset();
			res.resetBuffer();
			res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			res.setHeader("Content-Type", "application/json;charset=UTF-8");
			String s = "{\"errorMessage\":\"" + msg + "\"}";
			res.getOutputStream().write(s.getBytes("UTF-8"));
			res.flushBuffer();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("ERROR, Construyendo Respuesta Interceptor Usuarios");
		} catch (IOException e) {
			throw new RuntimeException("ERROR, Construyendo Respuesta Interceptor Usuarios");
		}

		return res;
	}
}
