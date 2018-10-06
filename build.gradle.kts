group = "ag"
version = "1.0-SNAPSHOT"

plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    testCompile("junit", "junit", "4.12")
    implementation("com.google.code.gson:gson:2.8.5")
    compile("net.objecthunter","exp4j","0.4.8")
    compile("mysql", "mysql-connector-java", "8.0.12")
    compile("com.orientechnologies","orientdb-jdbc","3.0.8")
    compile("org.mongodb","mongodb-driver-sync","3.8.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}