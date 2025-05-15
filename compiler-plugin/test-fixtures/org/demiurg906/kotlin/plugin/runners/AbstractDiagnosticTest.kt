package org.demiurg906.kotlin.plugin.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

abstract class AbstractDiagnosticTest : BaseTestRunner() {
    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            commonFirWithPluginFrontendConfiguration()
            configureFirParser(FirParser.Psi)
        }
    }

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }
}
