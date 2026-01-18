package config

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.util.Properties

interface CustomPropertiesFileParameters : ValueSourceParameters {
    val propertiesFile: RegularFileProperty
}

abstract class CustomPropertiesFileValueSource : ValueSource<Map<String, String>, CustomPropertiesFileParameters> {
    override fun obtain(): Map<String, String>? {
        val file = parameters.propertiesFile.orNull?.asFile ?: return null
        if (!file.exists()) return null

        val props = Properties()
        file.inputStream().use { props.load(it) }

        return props.entries.associate { it.key.toString() to it.value.toString() }
    }
}

val Project.localProperties: Provider<Map<String, String>>
    get() = providers
        .of(CustomPropertiesFileValueSource::class.java) {
            parameters.propertiesFile.set(
                project.rootDir.resolve("local.properties")
            )
        }