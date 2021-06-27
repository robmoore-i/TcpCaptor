import java.nio.file.Paths

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
    testLogging {
        showStandardStreams = true
    }
}

apply<TestKeystoresPlugin>()

configure<PrivateKey> {
    alias.set("selfsigned")
}

configure<Keystore> {
    commonName.set("localhost")
    location.set("Singapore")
    keystoreFilename.set("src/test/resources/test_keystore.jks")
}

configure<Truststore> {
    certificateFilename.set("src/test/resources/server.crt")
    truststoreFilename.set("src/test/resources/test_truststore.jks")
}

/***********************
* TestKeystoresPlugin  *
***********************/

enum class KeyAlgorithm {
    RSA
}

interface PrivateKey {
    val alias: Property<String>
}

interface Keystore {
    val keyAlgorithm: Property<KeyAlgorithm>
    val keySize: Property<Int>
    val validityPeriodDays: Property<Int>
    val commonName: Property<String>
    val organizationalUnit: Property<String>
    val organization: Property<String>
    val location: Property<String>
    val state: Property<String>
    val twoLetterCountryCode: Property<String>
    val password: Property<String>
    val keystoreFilename: Property<String>
}

interface Truststore {
    val certificateFilename: Property<String>
    val certificateAlias: Property<String>
    val password: Property<String>
    val truststoreFilename: Property<String>
}

class TestKeystoresPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val privateKey = target.extensions.create<PrivateKey>("privateKey")
        fun privateKeyAlias() = privateKey.alias.getOrElse("privatekey")

        val keystore = target.extensions.create<Keystore>("keystore")
        fun keystorePassword() = keystore.password.getOrElse("password")

        val createKeystore = target.tasks.register("createKeystore") {
            val keystoreFilename = keystore.keystoreFilename.getOrElse("keystore.jks")
            onlyIf { !Paths.get(keystoreFilename).toFile().exists() }
            outputs.file(keystoreFilename)
            doLast {
                target.exec {
                    val command = "keytool -genkey " +
                            "-keystore $keystoreFilename " +
                            "-keyalg ${keystore.keyAlgorithm.getOrElse(KeyAlgorithm.RSA)} " +
                            "-keysize ${keystore.keySize.getOrElse(2048)} " +
                            "-validity ${keystore.validityPeriodDays.getOrElse(365)} " +
                            "-alias ${privateKeyAlias()} " +
                            "-dname " +
                            "CN=${keystore.commonName.getOrElse("localhost")}," +
                            "OU=${keystore.organizationalUnit.getOrElse("Unknown")}," +
                            "O=${keystore.organization.getOrElse("Unknown")}," +
                            "L=${keystore.location.getOrElse("Unknown")}," +
                            "ST=${keystore.state.getOrElse("Unknown")}," +
                            "C=${keystore.twoLetterCountryCode.getOrElse("Unknown")} " +
                            "-storepass ${keystorePassword()}"
                    commandLine(command.split(" "))
                }
            }
        }

        val truststore = target.extensions.create<Truststore>("truststore")

        val exportCertificate = target.tasks.register("exportCertificate") {
            val certificateFilename = truststore.certificateFilename.getOrElse("test.crt")
            onlyIf { !Paths.get(certificateFilename).toFile().exists() }
            outputs.file(certificateFilename)
            val keystoreFilename = createKeystore.get().outputs.files.singleFile.path
            doLast {
                target.exec {
                    val exportCertificateCommand = "keytool -exportcert " +
                            "-keystore $keystoreFilename " +
                            "-alias ${privateKeyAlias()} " +
                            "-file $certificateFilename " +
                            "-storepass ${keystorePassword()}"
                    commandLine(exportCertificateCommand.split(" "))
                }
            }
            dependsOn(createKeystore)
        }

        target.tasks.register("createTruststore") {
            val outputTruststore = truststore.truststoreFilename.getOrElse("truststore.jks")
            onlyIf { !Paths.get(outputTruststore).toFile().exists() }
            val certificateFilename = exportCertificate.get().outputs.files.singleFile.path
            doLast {
                target.exec {
                    val createTruststoreCommand = "keytool -import " +
                            "-file $certificateFilename " +
                            "-alias ${truststore.certificateAlias.getOrElse("testCA")} " +
                            "-keystore $outputTruststore " +
                            "-storepass ${truststore.password.getOrElse("password")} " +
                            "-noprompt"
                    commandLine(createTruststoreCommand.split(" "))
                }
            }
            dependsOn(exportCertificate)
        }
    }
}