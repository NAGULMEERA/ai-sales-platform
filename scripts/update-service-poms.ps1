# Updates service POMs with standard dependencies and mainClass.
$ErrorActionPreference = "Stop"
$root = Join-Path (Split-Path -Parent $PSScriptRoot) "backend\services"

$services = @(
    @{ Artifact = "customer-service"; Main = "com.aisales.customer.CustomerServiceApplication" },
    @{ Artifact = "catalog-service"; Main = "com.aisales.catalog.CatalogServiceApplication" },
    @{ Artifact = "conversation-service"; Main = "com.aisales.conversation.ConversationServiceApplication" },
    @{ Artifact = "appointment-service"; Main = "com.aisales.appointment.AppointmentServiceApplication" },
    @{ Artifact = "ai-service"; Main = "com.aisales.ai.AiServiceApplication" },
    @{ Artifact = "workflow-service"; Main = "com.aisales.workflow.WorkflowServiceApplication" },
    @{ Artifact = "notification-service"; Main = "com.aisales.notification.NotificationServiceApplication" },
    @{ Artifact = "billing-service"; Main = "com.aisales.billing.BillingServiceApplication" },
    @{ Artifact = "integration-service"; Main = "com.aisales.integration.IntegrationServiceApplication" },
    @{ Artifact = "analytics-service"; Main = "com.aisales.analytics.AnalyticsServiceApplication" },
    @{ Artifact = "search-service"; Main = "com.aisales.search.SearchServiceApplication" },
    @{ Artifact = "media-service"; Main = "com.aisales.media.MediaServiceApplication" },
    @{ Artifact = "audit-service"; Main = "com.aisales.audit.AuditServiceApplication" },
    @{ Artifact = "deal-service"; Main = "com.aisales.deal.DealServiceApplication" },
    @{ Artifact = "marketplace-service"; Main = "com.aisales.marketplace.MarketplaceServiceApplication" },
    @{ Artifact = "lead-service"; Main = "com.aisales.lead.LeadServiceApplication" },
    @{ Artifact = "identity-service"; Main = "com.aisales.identity.IdentityServiceApplication"; Extra = @"
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-contracts</artifactId>
        </dependency>
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-events</artifactId>
        </dependency>
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
"@ },
    @{ Artifact = "tenant-service"; Main = "com.aisales.tenant.TenantServiceApplication"; Extra = @"
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-contracts</artifactId>
        </dependency>
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-events</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
"@ }
)

foreach ($svc in $services) {
    $extra = if ($svc.Extra) { $svc.Extra } else { "" }
    $pom = @"
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
    <artifactId>$($svc.Artifact)</artifactId>
    <name>$($svc.Artifact)</name>
    <dependencies>
        <dependency>
            <groupId>com.aisales</groupId>
            <artifactId>common-starter</artifactId>
        </dependency>
$extra
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
                    <mainClass>$($svc.Main)</mainClass>
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
    Set-Content -Path (Join-Path $root "$($svc.Artifact)/pom.xml") -Value $pom -Encoding UTF8
}

Write-Host "Updated $($services.Count) service POM files."
