package tiendita.api.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final String[] origenes;

    public ConfiguracionWeb(@Value("${cors.allowed-origins}") String[] origenes) {
        this.origenes = origenes;
    }

    /**
     * Solo hace falta en desarrollo, cuando el frontend corre en su propio
     * puerto. En la Pi, Caddy sirve las dos cosas desde el mismo origen y CORS
     * ni interviene — la lista se queda como red de seguridad.
     */
    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/**")
                .allowedOriginPatterns(origenes)
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
