package tiendita.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Levanta el contexto completo contra la base que construyó Flyway. Con
 * ddl-auto=validate, este test falla si una entidad y su tabla dejan de
 * coincidir — es decir, si alguien agrega un campo y olvida la migración.
 */
@SpringBootTest
@ActiveProfiles("test")
class TienditaApplicationTests {

    @Test
    void elContextoLevanta() {
    }
}
