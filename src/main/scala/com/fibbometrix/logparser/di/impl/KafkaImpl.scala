package com.fibbometrix.logparser.di.impl

import javax.inject.Inject
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.{Sink, Source}
import com.fibbometrix.logparser.di.intf.Kafka
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization._

import scala.util.{Failure, Success, Try}

class KafkaImpl @Inject() (config: Config) extends Kafka with StrictLogging {



  import akka.kafka.ProducerSettings
  import org.apache.kafka.common.serialization.StringSerializer

  lazy val maybePrefix = Try(config.getString("kafka.prefix")).toOption

  lazy val kafkaUrl = config.getString("kafka.url")

  def producerSettings: ProducerSettings[Integer, String] = {
      val intSerializer = new IntegerSerializer
      val stringSerializer = new StringSerializer
      //val jsValueSerializer = new JsValueSerializer()
      val producerConfig = config.getConfig("kafka.producer")
      ProducerSettings[Integer, String](producerConfig, intSerializer, stringSerializer)
        .withBootstrapServers(kafkaUrl)
  }

  def consumerSettings: ConsumerSettings[Integer, String] = {
      val stringDeserializer = new StringDeserializer
      val intDeserializer = new IntegerDeserializer
      //val jsValueDeserializer = new JsValueDeserializer()

      val consumerConfig = config.getConfig("kafka.consumer")
      val groupId = maybePrefix.getOrElse("") + "main4"

      ConsumerSettings(consumerConfig, intDeserializer, stringDeserializer)
        .withBootstrapServers(kafkaUrl)
        .withGroupId(groupId)
        .withClientId("mg")

  }

  def sink: Sink[ProducerRecord[Integer, String], _] = {
    Producer.plainSink(producerSettings)
  }

  def source(topic: String): Source[ConsumerRecord[Integer, String], _] = {
    val topicWithPrefix = maybePrefix.getOrElse("") + topic
    val subscriptions = Subscriptions.topics(topicWithPrefix)

    //    val partition = 1
    //    val subscriptions = Subscriptions.assignment(
    //      new TopicPartition("topicWithPrefix", partition)
    //    )
    Consumer.plainSource(consumerSettings, subscriptions)
    //consumerSettings.map(Consumer.plainPartitionedSource(_, subscriptions2))
  }


//class JsValueSerializer extends Serializer[JsValue] {
//  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = Unit
//  override def close(): Unit = Unit
//  override def serialize(topic: String, jsValue: JsValue): Array[Byte] = jsValue.toString().getBytes
//}
//
//class JsValueDeserializer extends Deserializer[JsValue] {
//  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = Unit
//  override def close(): Unit = Unit
//  override def deserialize(topic: String, data: Array[Byte]): JsValue = Json.parse(data)

}
