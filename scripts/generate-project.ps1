# AI Sales Platform - Source Generator
# Generates all module POMs and Java source files

$ErrorActionPreference = "Stop"
$root = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "..\backend"
Set-Location $root

function Write-File($path, $content) {
    $dir = Split-Path $path -Parent
    if ($dir -and !(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    Set-Content -Path $path -Value $content -Encoding UTF8 -NoNewline
    Add-Content -Path $path -Value "" -NoNewline
}

# --- Common module POM template ---
function CommonPom($artifactId, $deps = "") {
@'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aisales</groupId>
        <artifactId>ai-sales-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>ARTIFACT_ID</artifactId>
    <name>ARTIFACT_ID</name>
    <dependencies>
DEPS
    </dependencies>
</project>
'@ -replace 'ARTIFACT_ID', $artifactId -replace 'DEPS', $deps
}

Write-File "common/common-core/pom.xml" (CommonPom "common-core" @"
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
"@)

Write-File "common/common-exception/pom.xml" (CommonPom "common-exception" @"
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
"@)

Write-File "common/common-security/pom.xml" (CommonPom "common-security" @"
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
"@)

Write-File "common/common-events/pom.xml" (CommonPom "common-events" @"
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
"@)

Write-File "common/common-contracts/pom.xml" (CommonPom "common-contracts" @"
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
"@)

Write-File "common/common-observability/pom.xml" (CommonPom "common-observability" @"
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
        </dependency>
        <dependency>
            <groupId>io.zipkin.reporter2</groupId>
            <artifactId>zipkin-reporter-brave</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
"@)

Write-File "common/common-testing/pom.xml" (CommonPom "common-testing" @"
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
"@)

Write-File "common/common-starter/pom.xml" (CommonPom "common-starter" @"
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-exception</artifactId>
        </dependency>
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-observability</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
"@)

Write-Host "POMs generated. Generating Java sources..."

# Run the Java source generation part via separate script invocation
& "$root\scripts\generate-sources.ps1"

Write-Host "Generation complete."
