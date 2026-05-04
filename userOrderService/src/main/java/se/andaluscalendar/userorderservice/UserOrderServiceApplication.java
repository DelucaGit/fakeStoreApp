package se.andaluscalendar.userorderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class UserOrderServiceApplication {

    public static void main(String[] args) {
        // #region agent log
        try {
            String entry = String.format(
                    "{\"sessionId\":\"521952\",\"runId\":\"startup-debug\",\"hypothesisId\":\"H2\",\"location\":\"UserOrderServiceApplication.java:14\",\"message\":\"Starting app with DB env snapshot\",\"data\":{\"dbUserLocalSet\":%s,\"dbUrlLocalSet\":%s,\"jwtAccessSet\":%s,\"jwtRefreshSet\":%s},\"timestamp\":%d}%n",
                    System.getenv("DB_USERNAME_LOCAL") != null,
                    System.getenv("DB_URL_LOCAL") != null,
                    System.getenv("JWT_ACCESS_SECRET_KEY") != null,
                    System.getenv("JWT_REFRESH_SECRET_KEY") != null,
                    System.currentTimeMillis()
            );
            Files.writeString(Path.of("debug-521952.log"), entry, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
        // #endregion
        SpringApplication.run(UserOrderServiceApplication.class, args);
    }

}
