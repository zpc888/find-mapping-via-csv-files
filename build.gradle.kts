plugins {
    id("java")
    application
}

group = "prot.csv"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mvel:mvel2:2.5.2.Final")
    implementation("org.apache.commons:commons-csv:1.14.0")
    implementation("commons-cli:commons-cli:1.9.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass = "prot.csv.MappingFinder"
}

tasks.test {
    useJUnitPlatform()
}