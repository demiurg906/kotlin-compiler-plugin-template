package ru.itmo.kotlin.plugin.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.initIdeaConfiguration
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.junit.jupiter.api.BeforeAll
import ru.itmo.kotlin.plugin.services.ExtensionRegistrarConfigurator
import ru.itmo.kotlin.plugin.services.ExternalCompileDepsProvider
import ru.itmo.kotlin.plugin.services.PluginAnnotationsProvider
import ru.itmo.kotlin.plugin.services.ExternalRuntimeDepsProvider

abstract class BaseTestRunner : AbstractKotlinCompilerTest() {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setUp() {
            initIdeaConfiguration()
        }
    }

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }
}

fun TestConfigurationBuilder.commonFirWithPluginFrontendConfiguration() {
    baseFirDiagnosticTestConfiguration()

    defaultDirectives {
        +FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
        +FirDiagnosticsDirectives.FIR_DUMP
    }

    useCustomRuntimeClasspathProviders(
        ::ExternalRuntimeDepsProvider
    )

    useConfigurators(
        ::PluginAnnotationsProvider,
        ::ExternalCompileDepsProvider,
        ::ExtensionRegistrarConfigurator
    )
}
