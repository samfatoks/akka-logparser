package com.fibbometrix.logparser.di.intf

import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord


trait Kafka {
  val maybePrefix: Option[String]
  def sink: Sink[ProducerRecord[Integer, String], _]
  def source(topic: String): Source[ConsumerRecord[Integer, String], _]
}
