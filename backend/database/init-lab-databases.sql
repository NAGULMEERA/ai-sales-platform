-- Lab init: platform monolith + lead service DB
CREATE DATABASE ai_sales_platform;
CREATE DATABASE lead_db;
GRANT ALL PRIVILEGES ON DATABASE ai_sales_platform TO aisales;
GRANT ALL PRIVILEGES ON DATABASE lead_db TO aisales;
