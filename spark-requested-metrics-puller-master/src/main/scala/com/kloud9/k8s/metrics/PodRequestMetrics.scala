package com.kloud9.k8s.metrics

import com.kloud9.common.utilities.logger
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{explode, from_json, _}
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

object PodRequestMetrics {

  val path = "path/to/HDFS/dir"

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
      .config("es.nodes", "127.0.0.1")
      .config("es.output.json", "true")
      .master("local")
      .getOrCreate()

    import spark.implicits._


    /* Metrics Schema for request Metrics */

    var metricSchema = new StructType()
      .add("items",
        ArrayType (
          new StructType()
            .add("spec",
              new StructType()
                .add("containers",
                  ArrayType(
                    new StructType()
                      .add("resources",
                        new StructType()
                          .add("requests",
                            new StructType()
                              .add("cpu", IntegerType)
                              .add("memory", DoubleType)))
                  )))
        ))
    /* Kafka Stream Creation */
    val ds1 = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "capacity")
      .option("checkpointLocation", path)
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


    /* Function to write in Elastic Search */
    m.writeStream.format("org.elasticsearch.spark.sql")
      .option("checkpointLocation", path)
      .start("request/metrics").awaitTermination()

    /* Function to write Output in Console */
    val query = m.writeStream
      .outputMode("append")
      .format("console")
      .start().awaitTermination()


  }

}