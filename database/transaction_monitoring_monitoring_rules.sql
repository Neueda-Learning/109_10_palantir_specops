-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: localhost    Database: transaction_monitoring
-- ------------------------------------------------------
-- Server version	8.0.41

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `monitoring_rules`
--

DROP TABLE IF EXISTS `monitoring_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `monitoring_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `type` enum('AMOUNT_THRESHOLD','VELOCITY','NEW_PAYEE','DAILY_LIMIT') NOT NULL,
  `severity` enum('HIGH','MEDIUM','LOW') NOT NULL DEFAULT 'MEDIUM',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `threshold_amount` decimal(15,2) DEFAULT NULL,
  `transaction_count` int DEFAULT NULL,
  `time_window_minutes` int DEFAULT NULL,
  `daily_limit` decimal(15,2) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `monitoring_rules`
--

LOCK TABLES `monitoring_rules` WRITE;
/*!40000 ALTER TABLE `monitoring_rules` DISABLE KEYS */;
INSERT INTO `monitoring_rules` VALUES (1,'High Value Transaction','Alert when a single transaction exceeds $10,000','AMOUNT_THRESHOLD','HIGH',1,10000.00,NULL,NULL,NULL,'2026-08-06 06:17:24','2026-08-06 06:17:24'),(2,'Rapid Transaction Velocity','Alert when more than 5 transactions occur within 10 minutes from the same account','VELOCITY','MEDIUM',1,NULL,5,10,NULL,'2026-08-06 06:17:24','2026-08-06 06:17:24'),(4,'Daily Limit Exceeded','Alert when cumulative daily transaction amount exceeds $50,000','DAILY_LIMIT','HIGH',1,NULL,NULL,NULL,50000.00,'2026-08-06 06:17:24','2026-08-06 06:17:24'),(5,'New Payee Transaction','Alert when a transaction is made to a previously unseen payee','NEW_PAYEE','LOW',1,NULL,NULL,NULL,NULL,'2026-08-06 06:20:49','2026-08-06 06:20:49');
/*!40000 ALTER TABLE `monitoring_rules` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-06  6:32:26
