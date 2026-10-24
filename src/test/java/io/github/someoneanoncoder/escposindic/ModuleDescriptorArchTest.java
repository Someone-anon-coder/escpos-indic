/*
 * File:      ModuleDescriptorArchTest.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   Guards the JPMS descriptor: exactly the public API packages are exported.
 * Owns:      The expected list of exported packages and the hidden internal package.
 * Uses:      java.lang.module.ModuleDescriptor on the compiled module-info.class.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Update the list when a public package is added.
 * Plan:      L3
 */
package io.github.someoneanoncoder.escposindic;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.someoneanoncoder.escposindic.testing.ArchTest;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ArchTest
class ModuleDescriptorArchTest {

  private static final String ROOT = "io.github.someoneanoncoder.escposindic";

  /**
   * Reads the descriptor next to this library's own classes, not the first one on the classpath.
   */
  private static ModuleDescriptor descriptor() throws IOException, URISyntaxException {
    Path location =
        Path.of(EscPosBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    try (InputStream in = Files.newInputStream(location.resolve("module-info.class"))) {
      return ModuleDescriptor.read(in);
    }
  }

  @Test
  @DisplayName("the module exports exactly the public API packages and nothing else")
  void shouldExportOnlyPublicPackages_whenModuleDescriptorRead()
      throws IOException, URISyntaxException {
    Set<String> exported =
        descriptor().exports().stream()
            .map(ModuleDescriptor.Exports::source)
            .collect(Collectors.toSet());
    assertThat(exported)
        .containsExactlyInAnyOrder(
            ROOT, ROOT + ".barcode", ROOT + ".layout", ROOT + ".raster", ROOT + ".transport");
  }

  @Test
  @DisplayName("the internal package has classes but is not exported")
  void shouldHideInternal_whenModuleDescriptorRead() throws IOException, URISyntaxException {
    Path classes =
        Path.of(EscPosBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    assertThat(classes.resolve(ROOT.replace('.', '/') + "/internal/TextWrap.class")).exists();
    assertThat(descriptor().exports()).noneMatch(e -> e.source().contains(".internal"));
  }

  @Test
  @DisplayName("every exported package really contains classes")
  void shouldContainClasses_whenPackageIsExported() throws IOException, URISyntaxException {
    ModuleDescriptor module = descriptor();
    for (ModuleDescriptor.Exports export : module.exports()) {
      assertThat(module.packages()).contains(export.source());
    }
  }
}
