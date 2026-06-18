package com.miguelpimenta.buildlog;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Architecture tests powered by Spring Modulith. These are pure static analysis
 * over the compiled classes - no Spring context and no Docker - so they run as
 * part of {@code mvn test}.
 *
 * <p>Each direct sub-package of the application (vehicle, modification, dyno,
 * summary, common) is treated as a module. {@code verify()} fails the build if a
 * module reaches into another module's internals or if a dependency cycle is
 * introduced between modules.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(CarBuildLogApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    @Test
    void writesModuleDocumentation() {
        // Generates C4 component diagrams (PlantUML) and a module canvas under
        // target/spring-modulith-docs.
        new Documenter(modules).writeDocumentation();
    }
}
