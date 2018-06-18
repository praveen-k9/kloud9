package com.kloud9.k8s.metrics

import com.kloud9.common.utilities.logger
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{explode, from_json, _}
import org.apache.spark.sql.types._
import org.apache.spark.SparkConf

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

    val sparkConf = new SparkConf().setAppName("K8s_Cluster_Pod_Metrics_Puller")
    sparkConf.set("spark.sql.crossJoin.enabled", "true")

    val spark = SparkSession
      .builder()
      // .appName("K8s_Cluster_Pod_Metrics_Puller")
      .config(sparkConf)
      .config("es.index.auto.create", "true")
      .config("es.resource", "test")
      .config("es.nodes", "127.0.0.1")
      .config("es.output.json", "true")
      .master("local")
      .getOrCreate()

    import spark.implicits._


    /* Metrics Schema for stat/summary*/

    val metricSchema = new StructType()
      .add("node",
        new StructType()
          .add("nodeName" , StringType)
          .add("cpu",
            new StructType()
              .add("time", StringType)
              .add("usageNanoCores", DoubleType)
              .add("usageCoreNanoSeconds", DoubleType))
          .add("memory",
            new StructType()
              .add("time", StringType)
              .add("availableBytes", DoubleType)
              .add("usageBytes", DoubleType)
              .add("workingSetBytes", DoubleType)
              .add("rssBytes", DoubleType)
              .add("pageFaults", DoubleType)
              .add("majorPageFaults", DoubleType))
          .add("network",
            new StructType()
              .add("time", StringType)
              .add("name", StringType)
              .add("rxBytes", DoubleType)
              .add("rxErrors", DoubleType)
              .add("txBytes", DoubleType)
              .add("txErrors", DoubleType)
            //                .add("interfaces",
            //                  ArrayType(
            //                    new StructType()
            //                      .add("name", StringType)
            //                      .add("rxBytes", StringType)
            //                      .add("rxErrors", StringType)
            //                      .add("txBytes", StringType)
            //                      .add("txErrors",StringType)
            //                  ))
          )
          .add("fs",
            new StructType()
              .add("time", StringType)
              .add("availableBytes", DoubleType)
              .add("capacityBytes", DoubleType)
              .add("usedBytes", DoubleType)
              .add("inodesFree", StringType)
              .add("inodes", StringType)
              .add("inodesUsed", StringType))
        //              .add("runtime",
        //                      new StructType()
        //                        .add("imageFs",
        //                          ArrayType(
        //                            new StructType()
        //                              .add("time", StringType)
        //                              .add("availableBytes", StringType)
        //                              .add("capacityBytes", StringType)
        //                              .add("usedBytes", StringType)
        //                              .add("inodesFree", StringType)
        //                              .add("inodes", StringType)
        //                              .add("inodesUsed", StringType)
        //                          )))

      )
      .add("pods",
        ArrayType(
          new StructType()
            .add("podRef",
              new StructType()
                .add("name", StringType)
                .add("namespace", StringType)
                .add("uid", StringType))
            .add("startTime", StringType)
            .add("containers",
              ArrayType(
                new StructType()
                  .add("name", StringType)
                  .add("rootfs",
                    new StructType()
                      .add("time", StringType)
                      .add("availableBytes", DoubleType)
                      .add("capacityBytes", DoubleType)
                      .add("usedBytes", DoubleType)
                      .add("inodesfree", DoubleType)
                      .add("inodes", DoubleType)
                      .add("inodesUsed", DoubleType))
              ))
            .add("cpu",
              new StructType()
                .add("time", StringType)
                .add("usageNanoCores", DoubleType)
                .add("usageCoreNanoSeconds", DoubleType))
            .add("memory",
              new StructType()
                .add("time", StringType)
                .add("usageBytes", DoubleType)
                .add("workingSetBytes", DoubleType)
                .add("rssBytes", DoubleType)
                .add("pageFaults", DoubleType)
                .add("majorPageFaults", DoubleType))
            .add("network",
              new StructType()
                .add("time", StringType)
                .add("name", StringType)
                .add("rxBytes", DoubleType)
                .add("rxErrors", DoubleType)
                .add("txBytes", DoubleType)
                .add("txErrors", DoubleType))
            .add("volume",
              ArrayType(
                new StructType()
                  .add("time", StringType)
                  .add("availableBytes", DoubleType)
                  .add("capacityBytes", DoubleType)
                  .add("usedBytes", DoubleType)
                  .add("inodesFree", DoubleType)
                  .add("inodes", DoubleType)
                  .add("inodesUsed", DoubleType)
                  .add("name", StringType)
              ))
        )
      )


    /* Metrics Schema for http://127.0.0.1:8001/api/v1/nodes/minikube */

    val metricSchemaNodes = new StructType()
      .add("metadata" ,
        new StructType()
          .add("name" ,StringType))
      .add("status",
        new StructType()
          .add("capacity",
            new StructType()
              .add("pods" , StringType)
              .add("cpu" , StringType)
              .add("memory" , StringType))
          .add("allocatable",
            new StructType()
                .add("cpu", StringType)
                .add("memory", StringType)
                .add("pods", StringType)))

    /* Kafka Stream Creation */
    val ds1 = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "summaryTopic")
      .option("checkpointLocation", "/Users/suman.c/Desktop/AIDI/new")
      .option("failOnDataLoss", "false")
      .load()


    val ds2 = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "statusTopic")
      .option("checkpointLocation", "path/to/")
      .option("failOnDataLoss", "false")
      .load()



    /* Extracting Metrics as Json from Value */

    val m = ds1.select($"value" cast "string" as "json").select(from_json($"json", metricSchema) as "data").select("data.*")
    println(ds1.isStreaming)


    m.printSchema()


    val n = ds2.select($"value" cast "string" as "json").select(from_json($"json", metricSchemaNodes) as "data").select("data.*")
    println(ds2.isStreaming)

    n.printSchema()

    /* Function for Removing Strings from Memory Value */
    def remove_string: String => Double = {
      print(_)
      _.replaceAll("['Ki','Mi']", "").toDouble

    }

    def remove_string_udf = udf(remove_string)


    /* Transformation */

    var r = m.select(col("node.*"), explode('pods) as 'pod)



    // r1.printSchema()

    var nodeSummary=n.select(col("status.capacity.cpu").as("cpuCapacity"),
      remove_string_udf(col("status.capacity.memory")).as("memoryCapacity") ,
      col("metadata.name").as("nName"))

    //    val stream1 = r.as("data1")
    //
    //    val stream2 = nodeSummary.as("data2")
    //
    //    val joinds = stream1.join(stream2, col("data1.node.nodeName") === col("data2.nName") , "inner")

   // val joinds = r.join(nodeSummary, col("nodeName") === col("nName") , "inner")

   // joinds.printSchema()


    //var capCpu = n.select(col("status.capacity.cpu").as("suman"))

    //capCpu.printSchema()

    println("!!!!!!!!!!!!!!!!!")

   // val x = r.collect()

    //var r1 = r.withColumn("cpuCapacity", n.select("*").col("status.capacity.cpu"))

    //r1.printSchema()
    println("##########")

    var r2 = r.withColumn("cpuUsagePercentage", (col("pod.cpu.usageNanoCores") * 0.000001) / (n.select("*").col("status.capacity.cpu") * 1000))

    var r3 = r2.withColumn("memoryUsagePercentage", (col("pod.memory.usageBytes") / 1000) / col("memoryCapacity"))

    var r4 = r3.withColumn("netWorkInboundTraffic",col("pod.network.rxBytes")/1000)

    var r5 = r4.withColumn("networkOutboundTraffic", col("pod.network.txBytes")/1000)


    println("@@@@@@@@@@@@@@@@@")
    r5.printSchema()

    /* Function to write in Elastic Search */
//    r5.writeStream.format("org.elasticsearch.spark.sql")
//      .option("checkpointLocation", "/Users/suman.c/Desktop/AIDI/new")
//      .start("muki/pod").awaitTermination()



    /* Function to write Output in Console */
    val query = r5.writeStream
      .outputMode("append")
      .format("console")
      .start().awaitTermination()


  }

}
