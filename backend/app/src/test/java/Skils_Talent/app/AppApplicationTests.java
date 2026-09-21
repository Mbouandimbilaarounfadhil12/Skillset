package Skils_Talent.app;

import com.skillset.AppApplication;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AppApplication.class)
@Disabled("Requires a running PostgreSQL instance — use mvn spring-boot:run for integration validation")
class AppApplicationTests {

    @Test
    void contextLoads() {
    }
}
