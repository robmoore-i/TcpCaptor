import java.nio.file.Paths
import kotlin.io.path.exists

plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("org.hamcrest:hamcrest-core:2.2")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

enum class KeyAlgorithm {
    RSA
}

interface KeystoreDefinitionExtension {
    val outputFile: Property<String>
    val keyAlgorithm: Property<KeyAlgorithm>
    val keySize: Property<Int>
    val validityPeriodDays: Property<Int>
    val privateKeyAlias: Property<String>
    val commonName: Property<String>
    val organizationalUnit: Property<String>
    val organization: Property<String>
    val location: Property<String>
    val state: Property<String>
    val twoLetterCountryCode: Property<String>
    val password: Property<String>
}

class TestKeystoresPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val keystore = target.extensions.create<KeystoreDefinitionExtension>("keystore")
        target.tasks.register("createKeystore") {
            val outputFile = keystore.outputFile.getOrElse("keystore.jks")
            onlyIf { !Paths.get(outputFile).toFile().exists() }
            doLast {
                target.exec {
                    val command = "keytool -genkey " +
                            "-keystore $outputFile " +
                            "-keyalg ${keystore.keyAlgorithm.getOrElse(KeyAlgorithm.RSA)} " +
                            "-keysize ${keystore.keySize.getOrElse(2048)} " +
                            "-validity ${keystore.validityPeriodDays.getOrElse(365)} " +
                            "-alias ${keystore.privateKeyAlias.getOrElse("privatekey")} " +
                            "-dname " +
                            "CN=${keystore.commonName.getOrElse("localhost")}," +
                            "OU=${keystore.organizationalUnit.getOrElse("Unknown")}," +
                            "O=${keystore.organization.getOrElse("Unknown")}," +
                            "L=${keystore.location.getOrElse("Unknown")}," +
                            "ST=${keystore.state.getOrElse("Unknown")}," +
                            "C=${keystore.twoLetterCountryCode.getOrElse("Unknown")} " +
                            "-storepass ${keystore.password.getOrElse("password")}"
                    commandLine(command.split(" "))
                }
            }
        }
    }
}

apply<TestKeystoresPlugin>()

configure<KeystoreDefinitionExtension> {
    outputFile.set("testing/keystore.jks")
    privateKeyAlias.set("selfsigned")
    commonName.set("localhost")
    location.set("Singapore")
}