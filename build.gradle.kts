plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.2"
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("net.dv8tion:JDA:6.0.0-preview_DEV")
    implementation("com.github.eduardomcb:discord-webhook:1.0.0")
    implementation("com.google.code.gson:gson:2.13.1")
}

tasks.test {
    useJUnitPlatform()
}
application {
    mainClass = "de.saschat.acb.Main"
}