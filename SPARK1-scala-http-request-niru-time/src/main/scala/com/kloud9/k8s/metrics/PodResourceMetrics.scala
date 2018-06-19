package com.kloud9.k8s.metrics

import com.kloud9.common.utilities.logger
import org.apache.avro.generic.GenericData
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{explode, from_json, _}
import org.apache.spark.sql.types._

import scala.collection.mutable

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
      .config("es.nodes", "127.0.0.1")
      .config("es.output.json", "true")
      .master("local")
      .getOrCreate()

    import spark.implicits._


   /*http request for node*/

    val metricSchema = new StructType()
          .add("data",
          new StructType()
              .add("result",ArrayType(
                new StructType()
                  .add("metric",
                    new StructType()
                      .add("__name__",StringType)
                      .add("beta_kubernetes_io_arch",StringType)
                      .add("code",StringType)
                      .add("handler",StringType)
                      .add("instance",StringType)
                      .add("job",StringType)
                      .add("kubernetes_io_hostname",StringType)
                      .add("method",StringType))


                  .add("value",ArrayType(StringType)))))





    /* Kafka Stream Creation */
    val ds1 = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "firstTopic")
      .option("checkpointLocation", "/Users/gowthamapatil.rp/aIDI/http")
      .option("failOnDataLoss", "false")
      .load()





    /* Extracting Metrics as Json from Value */

//    val m = ds1.select($"value" cast "string" as "json").select(from_json($"json", metricSchema) as "data").select("data.*")
//    println(ds1.isStreaming)
//
//
//    m.printSchema()


    /* Function for Removing Strings from Memory Value */
    def remove_string: String => Double = {
      print(_)
      _.replaceAll("['Ki','Mi']", "").toDouble

    }

    def remove_string_udf = udf(remove_string)





    /* Transformation */
//    var r = m.select(col("data.result.metric").as("metric"), explode(col("data.result.value")).as("values"))

   //var r = m.select(explode('value) as 'pod)
//
//    var r1 = r.select(col("pod.metadata.name"), col("pod.metadata.namespace"), col("pod.metadata.creationTimestamp"), col("pod.containers").as("con"))
//
//
//    var r2 = r1.select(explode('con) as 'container, col("name"), col("namespace"), col("creationTimestamp"))
//
//    var r3 = r2.withColumn("container_memory", remove_string_udf(col("container.usage.memory")))

    def getVal = udf(getv)

    def getv : mutable.WrappedArray[String] => Int = {
      _.last.toInt
    }

    def getTime = udf(gett)

    def gett : mutable.WrappedArray[String] => Double = {
      _.toArray.apply(0).toDouble
    }


    val m = ds1.select($"value" cast "string" as "json").select(from_json($"json", metricSchema) as "data").select("data.*")
    val m1 = m.select("data.result").select(explode(col("result").as("eventVal")))
    val m2 = m1.withColumn("events", getVal(col("col.value"))).withColumn("epochTime", getTime(col("col.value")))
    m2.printSchema()
    val m4 = m2.withColumn("timeStamp", getTime(col("col.value")))
    val m3 = m2.select(col("col.metric"), col("events"), date_format(col("epochTime").cast("timestamp"),"YYYY-MM-dd HH:mm:ss").cast("timestamp").as("time"))



    m3.printSchema()



    /* Function to write in Elastic Search */
    m4.writeStream.format("org.elasticsearch.spark.sql")
      .option("checkpointLocation", "/Users/gowthamapatil.rp/aIDI/http")
      .start("httprequestuv/pod").awaitTermination()



    /* Function to write Output in Console */
        val query = m4.writeStream
          .outputMode("append")
          .format("console")
          .start().awaitTermination()


  }

}
