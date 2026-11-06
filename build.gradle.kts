plugins {
    java
    application
}

group = "com.back"
version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.6")
}

application {
    mainClass = "com.back.Main"
}

// 콘솔에서 ./gradlew run 할 때 키보드 입력을 앱으로 넘긴다.
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.test { useJUnitPlatform() }
