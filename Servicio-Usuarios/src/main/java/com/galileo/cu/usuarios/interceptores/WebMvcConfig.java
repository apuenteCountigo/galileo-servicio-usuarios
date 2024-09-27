package com.galileo.cu.usuarios.interceptores;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.MappedInterceptor;

@Component
public class WebMvcConfig {
	@Autowired
	UsuariosInterceptor usuariosInterceptor;

	@Bean
    public MappedInterceptor uniIntercept() {
        return new MappedInterceptor(new String[]{"/**"}, usuariosInterceptor);
    }
}
