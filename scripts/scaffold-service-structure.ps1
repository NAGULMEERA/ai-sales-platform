# Scaffolds DDD package structure, Flyway init, and ArchUnit tests for microservices.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$backend = Join-Path $root "backend"
$deployment = Join-Path $root "deployment"

function Write-TextFile($path, $content) {
    $dir = Split-Path $path -Parent
    if ($dir -and !(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
}

$services = @(
    @{ Name = "lead"; Port = 8083; Table = "lead"; Db = "lead_db" },
    @{ Name = "customer"; Port = 8084; Table = "customer"; Db = "customer_db" },
    @{ Name = "catalog"; Port = 8085; Table = "catalog_item"; Db = "catalog_db" },
    @{ Name = "conversation"; Port = 8086; Table = "conversation"; Db = "conversation_db" },
    @{ Name = "appointment"; Port = 8087; Table = "appointment"; Db = "appointment_db" },
    @{ Name = "ai"; Port = 8088; Table = "ai_prompt"; Db = "ai_db"; ClassPrefix = "Ai" },
    @{ Name = "workflow"; Port = 8089; Table = "workflow_execution"; Db = "workflow_db" },
    @{ Name = "notification"; Port = 8090; Table = "notification"; Db = "notification_db" },
    @{ Name = "billing"; Port = 8091; Table = "subscription"; Db = "billing_db" },
    @{ Name = "integration"; Port = 8092; Table = "integration_config"; Db = "integration_db" },
    @{ Name = "analytics"; Port = 8093; Table = "analytics_event"; Db = "analytics_db" },
    @{ Name = "search"; Port = 8094; Table = "search_document"; Db = "search_db" },
    @{ Name = "media"; Port = 8095; Table = "media_metadata"; Db = "media_db" },
    @{ Name = "audit"; Port = 8096; Table = "audit_log"; Db = "audit_db" },
    @{ Name = "deal"; Port = 8097; Table = "deal"; Db = "deal_db" },
    @{ Name = "marketplace"; Port = 8098; Table = "marketplace_listing"; Db = "marketplace_db" }
)

foreach ($svc in $services) {
    $name = $svc.Name
    $serviceName = "$name-service"
    $classPrefix = if ($svc.ClassPrefix) { $svc.ClassPrefix } else { (Get-Culture).TextInfo.ToTitleCase($name) }
    $appClass = "${classPrefix}ServiceApplication"
    $base = Join-Path $backend "services/$serviceName"
    $pkg = "com.aisales.$name"
    $pkgPath = $pkg.Replace('.', '/')

    # Remove legacy com.aisales.services.* application class
    $legacyApp = Join-Path $backend "services/$serviceName/src/main/java/com/aisales/services/$name"
    if (Test-Path $legacyApp) { Remove-Item -Recurse -Force $legacyApp }

    Write-TextFile (Join-Path $base "src/main/java/$pkgPath/$appClass.java") @"
package $pkg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "$pkg")
@EnableDiscoveryClient
public class $appClass {

    public static void main(String[] args) {
        SpringApplication.run($appClass.class, args);
    }
}
"@

    Write-TextFile (Join-Path $base "src/main/java/$pkgPath/api/controller/HealthController.java") @"
package $pkg.api.controller;

import com.aisales.common.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/$name/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("service", "$serviceName", "status", "UP"));
    }
}
"@

    Write-TextFile (Join-Path $base "src/main/java/$pkgPath/infrastructure/configuration/ServiceConfiguration.java") @"
package $pkg.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {
}
"@

    Write-TextFile (Join-Path $base "src/main/resources/db/migration/V1__init_${name}_db.sql") @"
CREATE EXTENSION IF NOT EXISTS ""pgcrypto"";

CREATE TABLE $($svc.Table) (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(100)
);

CREATE INDEX idx_$($svc.Table)_tenant ON $($svc.Table) (tenant_id);
"@

    Write-TextFile (Join-Path $base "src/test/java/$pkgPath/ArchitectureTest.java") @"
package $pkg;

import com.aisales.common.testing.architecture.LayeredArchitectureRules;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    @Test
    void shouldRespectLayeredArchitecture() {
        LayeredArchitectureRules.checkPackage("$pkg");
    }
}
"@

    $appYml = @"
server:
  port: $($svc.Port)

spring:
  application:
    name: $serviceName
  config:
    import: optional:configserver:http://localhost:8888
  datasource:
    url: jdbc:postgresql://localhost:5432/$($svc.Db)
    username: aisales
    password: aisales
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
"@
    Write-TextFile (Join-Path $base "src/main/resources/application.yml") $appYml
    Write-TextFile (Join-Path $root "infrastructure/config-server/src/main/resources/config-repo/$serviceName.yml") @"
server:
  port: $($svc.Port)

spring:
  application:
    name: $serviceName
"@

    Write-TextFile (Join-Path $deployment "kubernetes/$serviceName.yml") @"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $serviceName
  namespace: aisales
spec:
  replicas: 1
  selector:
    matchLabels:
      app: $serviceName
  template:
    metadata:
      labels:
        app: $serviceName
    spec:
      containers:
        - name: $serviceName
          image: aisales/$serviceName:1.0.0-SNAPSHOT
          ports:
            - containerPort: $($svc.Port)
---
apiVersion: v1
kind: Service
metadata:
  name: $serviceName
  namespace: aisales
spec:
  selector:
    app: $serviceName
  ports:
    - port: 80
      targetPort: $($svc.Port)
"@

    $pomPath = Join-Path $base "pom.xml"
    if (!(Test-Path $pomPath)) {
        Write-TextFile $pomPath @"
<?xml version=""1.0"" encoding=""UTF-8""?>
<project xmlns=""http://maven.apache.org/POM/4.0.0""
         xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""
         xsi:schemaLocation=""http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd"">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aisales</groupId>
        <artifactId>ai-sales-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>$serviceName</artifactId>
    <name>$serviceName</name>
    <dependencies>
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-testing</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>$pkg.$appClass</mainClass>
                </configuration>
                <executions>
                    <execution>
                        <goals><goal>repackage</goal></goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
"@
    }
}

Write-Host "Scaffolded $($services.Count) services."
