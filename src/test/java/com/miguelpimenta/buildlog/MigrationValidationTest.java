package com.miguelpimenta.buildlog;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Applies the Flyway migrations to an empty database and lets Hibernate validate the entities
 * against the result, so entity/migration drift fails the build.
 *
 * <p>Runs on H2 in PostgreSQL mode so it works without Docker. {@code VehicleApiIT} covers the same
 * ground against a real PostgreSQL under {@code mvn verify}.
 */
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:migrationcheck;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.flyway.enabled=true",
      "spring.flyway.baseline-on-migrate=false",
      // The point of the test: Hibernate must accept the Flyway-built schema.
      "spring.jpa.hibernate.ddl-auto=validate"
    })
class MigrationValidationTest {

  @Autowired DataSource dataSource;

  @Test
  void flywayBuildsASchemaTheEntitiesValidateAgainst() {
    // Reaching this point means the context started, which means Flyway migrated and
    // Hibernate's validate accepted the result.
    //
    // Querying the tables proves the migration actually created them: ddl-auto is
    // validate here, so Hibernate creates nothing of its own. If V1 were a no-op these
    // selects would fail rather than quietly pass.
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);

    assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from vehicles", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from modifications", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from dyno_results", Integer.class)).isZero();
  }
}
