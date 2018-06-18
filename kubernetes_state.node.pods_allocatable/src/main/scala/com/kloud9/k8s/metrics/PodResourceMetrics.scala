package com.kloud9.k8s.metrics

import com.kloud9.common.utilities.logger
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{from_json, _}
import org.apache.spark.sql.types._

/**
  * Consumes messages from one or more topics in Kafka and does wordcount.
  * Usage: PodMetrics <brokers> <topics>
  * <brokers> is a list of one or more Kafka brokers
  * <groupId> is a consumer group name to consume from topics
  * <topics> is a list of one or more kafka topics to consume from
  *
  * Example:
  * $ bin/run-example streaming.PodMetrics broker1-host:port,broker2-host:port \
  * consumer-group topic1,topic2
  */

object PodResourceMetrics {

  def main(args: Array[String]) {
    if (args.length < 3) {
      System.err.println(
        s"""
           |Usage: PodMetrics <brokers> <topics>
           |  <brokers> is a list of one or more Kafka brokers
           |  <groupId> is a consumer group name to consume from topics
           |  <topics> is a list of one or more kafka topics to consume from
           |
        """.stripMargin)
      System.exit(1)
    }

    logger.setStreamingLogLevels()

    val Array(brokers, groupId, topics) = args

    // Create context with 2 second batch interval

    val spark = SparkSession
       .builder()
      .appName("K8s_Cluster_Pod_Metrics_Puller")
      .config("es.index.auto.create", "true")
      .config("es.resource", "test")
      .config("es.nodes", "localhost")
      .config("es.output.json", "true")
      .master("local")
      .getOrCreate()

    import spark.implicits._


    /* Metrics Schema */
    val metricSchema = new StructType()
          .add("pods",
            ArrayType(
              new StructType()
                .add("podRef",
                  new StructType()
                    .add("name", StringType)
                    .add("namespace", StringType)
                    .add("uid", StringType))
                .add("startTime", StringType)
                .add("cpu",
                  new StructType()
                    .add("time", StringType)
                    .add("usageNanoCores", StringType)
                    .add("usageCoreNanoSeconds", StringType))
                .add("memory",
                  new StructType()
                    .add("time", StringType)
                    .add("usageBytes", StringType)
                    .add("workingSetBytes", StringType)
                    .add("rssBytes", StringType)
                    .add("pageFaults", StringType)
                    .add("majorPageFaults", StringType))
                .add("network",
                  new StructType()
                    .add("time", StringType)
                    .add("name", StringType)
                    .add("rxBytes", StringType)
                    .add("rxErrors", StringType)
                    .add("txBytes", StringType)
                    .add("txErrors", StringType))
                .add("volume",
                  ArrayType(
                    new StructType()
                      .add("time", StringType)
                      .add("availableBytes", StringType)
                      .add("capacityBytes", StringType)
                      .add("usedBytes", StringType)
                      .add("inodesFree", StringType)
                      .add("inodes", StringType)
                      .add("inodesUsed", StringType)
                      .add("name", StringType)
                  ))))


    /* Kafka Stream Creation */
    val ds1 = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "firstTopic")
      .option("checkpointLocation", "path/to/")
      .option("failOnDataLoss", "false")
      .load()





    /* Extracting Metrics as Json from Value */

    val m = ds1.select($"value" cast "string" as "json").select(from_json($"json", metricSchema) as "data").select("data.*")
    println(ds1.isStreaming)


    m.printSchema()


    /* Function for Removing Strings from Memory Value */
    def remove_string: String => Double = {
      print(_)
      _.replaceAll("['Ki','Mi']", "").toDouble

    }

    def remove_string_udf = udf(remove_string)





    /* Transformation */





    /* Function to write in Elastic Search */
       m.writeStream.format("org.elasticsearch.spark.sql")
       .option("checkpointLocation", "path/to/")
       .start("demo/pod").awaitTermination()



    /* Function to write Output in Console */
        val query = m.writeStream
          .outputMode("append")
          .format("console")
          .start().awaitTermination()


  }

}
