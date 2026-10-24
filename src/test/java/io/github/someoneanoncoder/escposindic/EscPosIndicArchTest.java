/*
 * File:      EscPosIndicArchTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Architecture rules for the library.
 * Owns:      No dependencies beyond the JDK, no floating point in the API, internal stays hidden.
 * Uses:      Public API
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-01
 */
package io.github.someoneanoncoder.escposindic;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import io.github.someoneanoncoder.escposindic.testing.ArchTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ArchTest
class EscPosIndicArchTest {

  private static final String ROOT = "io.github.someoneanoncoder.escposindic";
  private static final JavaClasses MAIN =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(ROOT);

  @Test
  @DisplayName("library depends on the JDK (java.*, javax.print) and ZXing only")
  void shouldDependOnJdkOnly_whenAnalysingMainClasses() {
    classes()
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(ROOT + "..", "java..", "javax.print..", "com.google.zxing..")
        .check(MAIN);
  }

  @Test
  @DisplayName("no floating-point fields")
  void shouldHaveNoFloatingPointFields_whenAnalysingMainClasses() {
    fields()
        .should()
        .notHaveRawType(double.class)
        .andShould()
        .notHaveRawType(float.class)
        .check(MAIN);
  }

  @Test
  @DisplayName("no access to System streams")
  void shouldNotTouchSystem_whenAnalysingMainClasses() {
    noClasses().should().accessClassesThat().haveFullyQualifiedName("java.lang.System").check(MAIN);
  }

  @Test
  @DisplayName("no package cycles")
  void shouldHaveNoCycles_whenAnalysingPackages() {
    SlicesRuleDefinition.slices()
        .matching(ROOT + ".(*)..")
        .should()
        .beFreeOfCycles()
        .allowEmptyShould(true)
        .check(MAIN);
  }
}
