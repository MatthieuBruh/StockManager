package fi.haagahelia.stockmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;


@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class StockManagerApplicationTest {

    @Test
    public void contextLoads() {
        System.out.println("========== CONTEXT =========");
    }

}