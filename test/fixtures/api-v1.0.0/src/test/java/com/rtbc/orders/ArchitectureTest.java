package com.rtbc.orders;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reglas de dependencia entre capas (ADR-002, riesgo R10).
 */
@AnalyzeClasses(packages = "com.rtbc.orders", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	private static final String DOMAIN = "Domain";
	private static final String APPLICATION = "Application";
	private static final String INFRASTRUCTURE = "Infrastructure";
	private static final String ENTRYPOINT = "Entrypoint";

	@ArchTest
	static final ArchRule las_dependencias_entre_capas_apuntan_hacia_el_dominio = layeredArchitecture()
			.consideringOnlyDependenciesInLayers()
			.layer(DOMAIN).definedBy("..domain..")
			.layer(APPLICATION).definedBy("..application..")
			.layer(INFRASTRUCTURE).definedBy("..infrastructure..")
			.layer(ENTRYPOINT).definedBy("..entrypoint..")
			.whereLayer(ENTRYPOINT).mayNotBeAccessedByAnyLayer()
			.whereLayer(INFRASTRUCTURE).mayNotBeAccessedByAnyLayer()
			.whereLayer(APPLICATION).mayOnlyBeAccessedByLayers(INFRASTRUCTURE, ENTRYPOINT)
			.whereLayer(DOMAIN).mayOnlyBeAccessedByLayers(APPLICATION, INFRASTRUCTURE, ENTRYPOINT);

	@ArchTest
	static final ArchRule el_dominio_no_depende_de_frameworks = classes()
			.that().resideInAPackage("..domain..")
			.should().onlyDependOnClassesThat().resideInAnyPackage("..domain..", "java..")
			.because("el dominio debe probarse sin Spring ni JPA (ADR-002)");
}
