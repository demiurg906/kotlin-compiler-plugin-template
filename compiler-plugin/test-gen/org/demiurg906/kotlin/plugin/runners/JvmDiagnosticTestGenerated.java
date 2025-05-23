

package org.demiurg906.kotlin.plugin.runners;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.demiurg906.kotlin.plugin.GenerateTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler-plugin/testData/diagnostics")
@TestDataPath("$PROJECT_ROOT")
public class JvmDiagnosticTestGenerated extends AbstractJvmDiagnosticTest {
  @Test
  public void testAllFilesPresentInDiagnostics() {
    KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler-plugin/testData/diagnostics"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JVM_IR, true);
  }

  @Test
  @TestMetadata("anotherDiagnosticTest.kt")
  public void testAnotherDiagnosticTest() {
    runTest("compiler-plugin/testData/diagnostics/anotherDiagnosticTest.kt");
  }

  @Test
  @TestMetadata("simple.kt")
  public void testSimple() {
    runTest("compiler-plugin/testData/diagnostics/simple.kt");
  }
}
