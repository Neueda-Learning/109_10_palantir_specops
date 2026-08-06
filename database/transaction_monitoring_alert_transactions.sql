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
-- Table structure for table `alert_transactions`
--

DROP TABLE IF EXISTS `alert_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_transactions` (
  `alert_id` bigint NOT NULL,
  `transaction_id` bigint NOT NULL,
  PRIMARY KEY (`alert_id`,`transaction_id`),
  KEY `transaction_id` (`transaction_id`),
  CONSTRAINT `alert_transactions_ibfk_1` FOREIGN KEY (`alert_id`) REFERENCES `alerts` (`id`),
  CONSTRAINT `alert_transactions_ibfk_2` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alert_transactions`
--

LOCK TABLES `alert_transactions` WRITE;
/*!40000 ALTER TABLE `alert_transactions` DISABLE KEYS */;
INSERT INTO `alert_transactions` VALUES (75,57),(71,64),(75,121),(63,131),(58,160),(68,192),(59,287),(72,342),(62,368),(56,379),(70,416),(62,534),(56,622),(60,626),(61,635),(71,666),(59,730),(58,758),(65,797),(62,816),(74,828),(1,871),(2,872),(73,872),(3,873),(4,874),(72,874),(5,875),(70,875),(6,876),(62,876),(7,877),(68,877),(8,878),(57,878),(9,879),(10,880),(66,880),(11,881),(59,881),(12,882),(13,883),(14,884),(63,884),(15,885),(67,885),(16,886),(17,887),(71,887),(18,888),(19,889),(74,889),(20,890),(21,891),(22,892),(56,892),(23,893),(75,893),(24,894),(25,895),(65,895),(26,896),(27,897),(28,898),(60,898),(29,899),(64,899),(30,900),(36,901),(37,902),(38,903),(39,904),(40,905),(41,906),(42,907),(43,908),(44,909),(45,910),(46,911),(47,912),(48,913),(49,914),(50,915),(51,916),(52,917),(53,918),(54,919),(55,920),(33,921),(33,922),(33,923),(33,924),(33,925),(33,926),(33,927),(33,928),(33,929),(33,930),(34,931),(34,932),(34,933),(34,934),(34,935),(34,936),(34,937),(34,938),(34,939),(34,940),(31,941),(31,942),(31,943),(31,944),(31,945),(31,946),(31,947),(31,948),(31,949),(31,950),(32,951),(32,952),(32,953),(32,954),(32,955),(32,956),(32,957),(32,958),(32,959),(32,960),(35,961),(35,962),(35,963),(35,964),(35,965),(35,966),(35,967),(35,968),(35,969),(35,970),(69,971),(69,972),(69,973),(69,974),(69,975),(69,976),(69,977),(69,978),(69,979),(69,980),(58,981),(58,982),(58,983),(58,984),(58,985),(58,986),(58,987),(58,988),(58,989),(58,990),(61,991),(61,992),(61,993),(61,994),(61,995),(61,996),(61,997),(61,998),(61,999),(61,1000);
/*!40000 ALTER TABLE `alert_transactions` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-06  6:32:25
