package com.fibbo.logparser.util

import java.io.File
import java.nio.file.{Path, Paths}
import java.text.SimpleDateFormat
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.regex.Pattern

import javax.inject.Inject
import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.stream._
import akka.stream.scaladsl.Framing.FramingException
import akka.stream.scaladsl.{Balance, Broadcast, FileIO, Flow, Framing, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source, Zip}
import akka.util.ByteString
import com.fibbo.logparser.di.intf.Kafka
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import io.circe.{HCursor, Json}
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.{ExecutionContext, TimeoutException}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import io.circe.generic.auto._
import io.circe.literal._
import io.circe.syntax._
import io.circe.parser._


case class SourceEvent(id: Integer)

case class DomainEvent(id: Integer, processingTimestamp: Long)

case class Metric(label: String, value: Int)


class StreamUtility @Inject()(config: Config, kafka: Kafka)(implicit system: ActorSystem, ec: ExecutionContext) extends StrictLogging {

  //implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val materializer: Materializer = {
    ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy { e =>
      logger.error("Uncaught exception in stream!", e)
      Supervision.Stop
    })
  }

  //  implicit val materializer = ActorMaterializer()
  //  implicit val mat = ActorMaterializer(
  //    ActorMaterializerSettings(system)
  //      .withSupervisionStrategy(Supervision.restartingDecider)
  //  )

//    val decider: Supervision.Decider = {
//      case _: ArithmeticException => Supervision.Resume
//      case _: FramingException => Supervision.Resume
//      case _ => Supervision.Stop
//    }
//    implicit val materializer = ActorMaterializer(
//      ActorMaterializerSettings(system).withSupervisionStrategy(decider))

  val decider: Supervision.Decider = {
    case _: FramingException => Supervision.Resume
    case _ => Supervision.Stop
  }


  def basic1: Unit = {
    val source: Source[Int, NotUsed] = Source(1 to 5)
    source.runForeach(i => logger.info(s"$i"))
  }

  //  def basic2: Unit = {
  //    val source: Source[Int, NotUsed] = Source(6 to 10)
  //
  //    //val sink2 = Sink.foreach(i: Int => logger.info(s"$i"))
  //    val sink = Sink.fold[Int, Int](0)(_ + _)
  //
  //    source
  //      .runWith(sink)
  //      .map { result => logger.info(s"Completed basic2 : $result") }
  //  }

  def basic3: Unit = {
    val source: Source[Int, NotUsed] = Source(11 to 15)

    source
      .map(_ / 0)
      .recover {
        case _: RuntimeException => "stream truncated"
      }
      .runForeach(i => logger.info(s"$i"))

    //    t match {
    //      case Success(result) => {
    //        result.map{_ => logger.info("Completed basic3")}
    //      }
    //      case Failure(f) => logger.error(s"TRY FAILURE : ${f.getMessage}")
    //    }

  }


  def slowSink =
    Flow[Seq[Metric]]
      .buffer(7, OverflowStrategy.backpressure)
      .delay(5.seconds, DelayOverflowStrategy.backpressure)
      .to(Sink.foreach(e => logger.info(s"New Here - ${e.toString()}")))

  val bufferedSink = Flow[Seq[Metric]].buffer(3, OverflowStrategy.backpressure).via(Flow[Seq[Metric]].throttle(1, 1.second, 1, ThrottleMode.shaping)).toMat(Sink.foreach(e => logger.info(s"New Here - ${e.toString()}")))(Keep.right)


  def fastSource: Source[SourceEvent, NotUsed] =
    Source(1 to 10)
      //Source.fromIterator(() => Iterator.from(1, 1))
      .map { i =>
      logger.info(s"Producing event $i")
      SourceEvent(i)
    }

  def enrichWithTimestamp =
    Flow[SourceEvent]
      .map { e =>
        logger.info(s"Enriching event ${e.id}")
        DomainEvent(e.id, System.currentTimeMillis())
      }

  def countStage =
    Flow[DomainEvent]
      .grouped(2)
      .map { seq => Seq(Metric("count", seq.size)) }

  def process: Unit = {

    fastSource
      .via(enrichWithTimestamp)
      .via(countStage)
      .to(bufferedSink)
      .run()
  }


  def lineSink(filename: String) =
    Flow[String]
      //.alsoTo(Sink.foreach(s => logger.info(s"$filename: $s")))
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)



//  def modifiedKafkaSink(topic: String) = Flow[JsValue].map { msg =>
//    val partition = 0
//    new ProducerRecord[Integer, JsValue](topic, partition, 1, msg)
//  }

    def modifiedKafkaSink(topic: String) = Flow[String].map { msg =>
//      val partition = 0
//      new ProducerRecord[Integer, String](topic, partition, 1, msg)
      new ProducerRecord[Integer, String](topic, msg)
    }


  def processSlowConsumer: Unit = {

    val source: Source[Int, NotUsed] = Source(1 to 10)
    val factorials: Source[BigInt, NotUsed] = source.scan(BigInt(1))((acc, next) => acc * next)
    val sink1 = lineSink("factorial1.txt")
    val sink2 = lineSink("factorial2.txt")
    val slowSink2 = Flow[String].via(Flow[String].throttle(1, 3.second, 1, ThrottleMode.shaping)).toMat(sink2)(Keep.right)
    val bufferedSink2 = Flow[String].buffer(3, OverflowStrategy.backpressure).via(Flow[String].throttle(1, 1.second, 1, ThrottleMode.shaping)).toMat(sink2)(Keep.right)

    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val bcast = b.add(Broadcast[String](2))

      factorials.map { fac =>
        //println(s"fac: $fac")
        fac.toString
      } ~> bcast.in

      bcast.out(0) ~> sink1
      bcast.out(1) ~> bufferedSink2
      ClosedShape
    })

    val g2 = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val in = Source(1 to 10)
      //val out = Sink.ignore
      //      val out = Sink.foreach(r:Int => {
      //        println(s"Merge Result : $r")
      //      })

      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))


      val f1, f2, f3, f4 = Flow[Int].map(_ + 10)
      //val c = Flow[Int].fold[Int, Int](0)(_ + _)

      val sink = Sink.fold[Int, Int](0)(_ + _)
      val sink2 = Sink.foreach[Int] { x =>
        println(s"Last Mile : $x")
      }

      in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> sink2
      bcast ~> f4 ~> merge

      //      in.map{s =>
      //        println(s"Source : $s")
      //        s
      //      } ~> f1.map{ s =>
      //        println(s"Flow : $s")
      //        s
      //      } ~> bcast ~> sink2
      //
      //in ~> f1 ~> bcast ~> sink2

      //bcast.out(0) ~> sink1


      ClosedShape
    })

    println("About to run")
    g.run()
    //val sum: Future[Int] = g.run()
  }

  val g5 = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val A: Outlet[Int] = builder.add(Source.single(0)).out
    val B: UniformFanOutShape[Int, Int] = builder.add(Broadcast[Int](2))
    val C: UniformFanInShape[Int, Int] = builder.add(Merge[Int](2))
    val D: FlowShape[Int, Int] = builder.add(Flow[Int].map(_ + 1))
    val E: UniformFanOutShape[Int, Int] = builder.add(Balance[Int](2))
    val F: UniformFanInShape[Int, Int] = builder.add(Merge[Int](2))
    val G: Inlet[Any] = builder.add(Sink.foreach(println)).in


    C <~ F
    A ~> B ~> C ~> F
    B ~> D ~> E ~> F
    E ~> G

    ClosedShape
  })


  val pureTwitterSink = lineSink("tweeter.txt")
  //val stringBufferedSink = Flow[String].buffer(1, OverflowStrategy.backpressure).via(Flow[String].throttle(1, 1.second, 1, ThrottleMode.shaping))
  //val stringBufferedSink = Flow[String].buffer(10, OverflowStrategy.backpressure).via(Flow[String].throttle(10, 1.second, 1, ThrottleMode.shaping))
  def stringBufferedSink(bufferSize: Int, throttleDelay: FiniteDuration) =
    Flow[String]
    .buffer(bufferSize, OverflowStrategy.backpressure).via(Flow[String]
    .throttle(bufferSize, throttleDelay, 1, ThrottleMode.shaping))

  def stringThrottledSink(throttleDelay: FiniteDuration) = Flow[String].via(Flow[String].throttle(1, throttleDelay, 1, ThrottleMode.shaping))

  //val stringThrottledSink = Flow[String].via(Flow[String].throttle(1, 3.second, 1, ThrottleMode.shaping)).toMat(pureTwitterSink)(Keep.right)


  def emptyStringSource = Source.single("")

//  def jsonTransformation(hashTag: Hashtag, msg: String): JsValue = {
//
//    val tweetResult: JsResult[Tweet] = Json.parse(msg).validate[Tweet]
//    tweetResult match {
//      case tweetResult => {
//        tweetResult match {
//          case JsSuccess(tweet: Tweet, _) => {
//            //logger.info(s"HashTag : $hashTag, Tweet : ${tweet}")
//            val twitterWebsocketResponse = TwitterWebsocketResponse(System.currentTimeMillis, hashTag, tweet, Set.empty, "")
//            Json.toJson(twitterWebsocketResponse)
//          }
//          case e: JsError => {
//            logger.error("Errors: " + JsError.toJson(e).toString())
//            JsError.toJson(e)
//          }
//        }
//      }
//    }
//  }
//
//  def messageTransformer(hashTag: Hashtag, msg: String): TwitterWebsocketResponse = {
//
//    val tweetResult: JsResult[Tweet] = Json.parse(msg).validate[Tweet]
//    tweetResult match {
//      case JsSuccess(tweet: Tweet, _) => {
//        logger.info(s"HashTag : $hashTag, Tweet : ${tweet}")
//        TwitterWebsocketResponse(System.currentTimeMillis, hashTag, tweet, tweet.hashtags, "")
//      }
//      case e: JsError => {
//        logger.error("Errors: " + JsError.toJson(e).toString())
//        TwitterWebsocketResponse(System.currentTimeMillis, hashTag, null, Set.empty, JsError.toJson(e).toString())
//      }
//    }
//  }

  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val in = Source(1 to 10)
    val ignore = Sink.ignore
    val out = Sink.foreach(println)
    //val out: Inlet[Any] = builder.add(Sink.foreach(println)).in

    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))

    val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

    val blockingFlow = Flow[Int].take(0)

    val validReq = Flow[Int].filter(_ % 2 == 0)
    val invalidReq = Flow[Int].filter(_ % 2 != 0)

    in ~> bcast ~> validReq ~> f2 ~> merge ~> out
    bcast ~> invalidReq ~> blockingFlow ~> merge
    ClosedShape
  })




  //  def cleanUpGraph(source: Source[String, _]) = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
  //    import GraphDSL.Implicits._
  //
  //    val out : Sink[String,_] = ???
  //
  //    val bcast = builder.add(Broadcast[String](2))
  //    val merge = builder.add(Merge[String](2))
  //
  //    val blockingFlow = Flow[String].take(0)
  //
  //    val validReq = Flow[String].filter(_.size > 0)
  //    val invalidReq = Flow[String].filter(_.size == 0)
  //
  //    source ~> bcast ~> validReq ~> merge ~> out
  //    bcast ~> invalidReq ~> blockingFlow ~> merge
  //    ClosedShape
  //  })

  val pairUpWithToString =
    Flow.fromGraph(GraphDSL.create() { implicit b ⇒
      import GraphDSL.Implicits._

      // prepare graph elements
      val broadcast = b.add(Broadcast[Int](2))
      val zip = b.add(Zip[Int, String]())

      // connect the graph
      broadcast.out(0).map(identity) ~> zip.in0
      broadcast.out(1).map(_.toString) ~> zip.in1

      // expose ports
      FlowShape(broadcast.in, zip.out)
    })

  val cleanUpGraph = Flow.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val bcast = builder.add(Broadcast[String](2))
    val merge = builder.add(Merge[String](2))
    val blockingFlow = Flow[String].take(0)

    val validReq = Flow[String].filter(_.size > 0)
    val invalidReq = Flow[String].filter { m =>
      m.size == 0 || m.contains("Exceeded")
    }
    bcast ~> validReq ~> merge
    bcast ~> invalidReq ~> blockingFlow ~> merge


    FlowShape(bcast.in, merge.out)
  })

  //  def storageSink(kafkaTopic: String) = Flow.fromGraph(GraphDSL.create() { implicit builder =>
  //    import GraphDSL.Implicits._
  //
  //    val filename: String = s"twitter-cleanup-tweet.log.${new SimpleDateFormat("yyyy-MM-dd-hhmmss").format(new Date)}"
  //
  //    val bcast = builder.add(Broadcast[TwitterWebsocketResponse](3))
  //    val fileSink = lineSink(filename)
  //    val jsonStringTransformer: Flow[TwitterWebsocketResponse, String, NotUsed] = Flow[TwitterWebsocketResponse].map(Json.toJson(_).toString())
  //
  //
  //    bcast.out(0) ~> jsonStringTransformer ~> fileSink.async
  //    bcast.out(1)  ~> jsonStringTransformer ~> modifiedKafkaSink(kafkaTopic) ~> kafka.sink.get.async
  //    FlowShape(bcast.in, bcast.out(2))
  //  })

//  def storageSink(kafkaTopic: String, hashTag: Hashtag) = Flow.fromGraph(GraphDSL.create() { implicit builder =>
//    import GraphDSL.Implicits._
//
//    val filename: String = s"twitter-cleanup-tweet.log.${new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date)}"
//
//    val bcast = builder.add(Broadcast[String](3))
//    val fileSink = Flow[JsValue].map(s => ByteString(s.toString() + "\n"))
//      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
//
//    val jsonStringTransformer: Flow[String, JsValue, NotUsed] = Flow[String].map { s =>
//      Json.obj("timestamp" -> System.currentTimeMillis(),
//        "hashtag" -> hashTag.toString,
//        "tweet" -> Json.parse(s))
//    }
//
//    bcast.out(0) ~> jsonStringTransformer ~> fileSink.async
//    bcast.out(1) ~> jsonStringTransformer ~> modifiedKafkaSink(kafkaTopic) ~> kafka.sink.get.async
//    FlowShape(bcast.in, bcast.out(2))
//  })


  def adhocSource[T](source: Source[T, _], timeout: FiniteDuration, maxRetries: Int): Source[T, _] =
    Source.lazily(
      () => source.backpressureTimeout(timeout).recoverWithRetries(maxRetries, {
        case t: TimeoutException =>
          Source.lazily(() ⇒ source.backpressureTimeout(timeout)).mapMaterializedValue(_ => NotUsed)
      })
    )


//  def transformTwitterSource(twitterSource: Source[ByteString, _], hashTag: Hashtag, throttleDelay: Int) = {
//
//    val kafkaTopic = "twitter"
//    val modifiedSource = twitterSource.map { result =>
//      val receivedTweet = result.utf8String
//      logger.info(s"Received Message : $receivedTweet")
//      receivedTweet.trim
//    }
//      //.via(stringThrottledSink(throttleDelay))
//      //.via(stringBufferedSink)
//
//
//      .via(cleanUpGraph)
//      //      .flatMapConcat({
//      //      case "" => {
//      //        logger.info("Empty String found!")
//      //        Source.empty
//      //      }
//      //      case "Exceeded connection limit for user" => {
//      //        logger.info("Twitter connection exceeded!")
//      //        Source.empty
//      //      }
//      //      case other => {
//      //        //logger.info("normal message")
//      //        Source.single(other)
//      //      }
//      //    })
//      .via(storageSink(kafkaTopic, hashTag)).async
//      .map { msg =>
//        //logger.info(s"Buffered and Throttled Message : $msg")
//        //jsonTransformation(hashTag, msg)
//        messageTransformer(hashTag, msg)
//
//        //val tweetInfo = TweetInfo(keyword, (json \ "id").as[Long], (json \ "text").as[String], (json \ "user" \ "name").as[String])
//        //        logger.info(Json.toJson(tweetInfo).toString())
//        //logger.info(Json.prettyPrint(json))
//
//        //Try(Json.parse(bytes.toArray).validate[Tweet])
//      }
//      .recover {
//        //        case _: TimeoutException => logger.info("No messages received for 30 seconds")
//        //        case _: RuntimeException => logger.info( "stream truncated")
//        case e => {
//          logger.error("Error in transformTwitterSource - " + e.getMessage)
//          throw e
//        }
//      }
//    modifiedSource
//  }


  def streamComplete(status: Try[Done]): Unit = {
    status match {
      case Success(_) => logger.info(s"${Thread.currentThread().getName} -- Successfully completed stream")
      case Failure(e) => logger.error("STREAM FAILURE  -->>>>", e)
    }
  }


  def fileProcessorGraph(filename: String, kafkaTopic: String) = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._


    val eventDate = if(filename.endsWith(".log")) LocalDate.now() else LocalDate.parse(filename.split(".log. ")(1).substring(0, 10))

    val in = FileIO.fromPath(Paths.get(filename))
      .withAttributes(ActorAttributes.dispatcher("blocking-io-dispatcher"))
    val delimiter = Framing.delimiter(ByteString("\n"), 100000, true).map(_.utf8String)
    val out = Sink.foreach[String](logger.info(_))

    val lineFilter = Flow[String]
      .filter(_.contains("Full JSON Payload is"))
//      .map(_.split(" :: ")(1))
//      .filter(_.startsWith("{"))
      .recoverWithRetries(1, {
        case e: RuntimeException => {
          logger.error( "Runtime Exeption", e)
          Source.single("Streaming stoped because of error in the pipeline")
        }
      })



    val sinkFilename = s"combined-file-${DateTimeFormatter.ofPattern("yyyy-mm-dd hh:mm:ss").format(LocalDateTime.now())}.log"
    val fileSink = lineSink(sinkFilename)
    val bcast = builder.add(Broadcast[String](4))
    //val merge = builder.add(Merge[String](2))

    val jsonSinkFilename = s"combined-json-file-${DateTimeFormatter.ofPattern("yyyy-mm-dd hh:mm:ss").format(LocalDateTime.now())}.log"
    val jsonFileSink = lineSink(jsonSinkFilename)

    val jsonTransformer = Flow[String]
      .map { line =>
        parse(line) match {
          case Right(json) => {

            val newJson = json.mapObject { jsonObject =>
              val s = jsonObject
                .add("eventTime", Json.fromLong(Instant.now.getEpochSecond))
                .add("processingTime", Json.fromLong(Instant.now.getEpochSecond))

//              s = if(s.contains("OSV")) s.add("osv", s.apply("OSV").get).remove("OSV") else s
//              s = if(s.contains("Opin")) s.add("opin", s.apply("Opin").get).remove("Opin") else s
//              s = if(s.contains("Npin")) s.add("npin", s.apply("Npin").get).remove("Npin") else s
              s
            }
            newJson.noSpaces
          }
          case Left(e) => {
            logger.error("Parse Error", e)
            "Not a valid JSON"
          }
        }
      }

    val jsonTransformer2 = Flow[String]
      .map(parseLineWithRegex(_, eventDate.toString))
      .filter(_._4.startsWith("{"))
      .map { c =>
        val eventTimestamp = c._1
        val reference = c._2
        val threadId = c._3
        val jsonString = c._4

        parse(jsonString) match {
          case Right(json) => {

            val newJson = json.mapObject { jsonObject =>
              val s = jsonObject
                .add("eventTime", Json.fromLong(eventTimestamp))
                .add("processingTime", Json.fromLong(Instant.now.getEpochSecond))
                .add("reference", Json.fromString(reference))
                .add("threadId", Json.fromString(threadId))

//              s = if(s.contains("OSV")) s.add("osv", s.apply("OSV").get).remove("OSV") else s
//              s = if(s.contains("Opin")) s.add("opin", s.apply("Opin").get).remove("Opin") else s
//              s = if(s.contains("Npin")) s.add("npin", s.apply("Npin").get).remove("Npin") else s
              s
            }
            newJson.noSpaces
          }
          case Left(e) => {
            logger.error("Parse Error", e)
            "Not a valid JSON"
          }
        }
      }



//    val blockingFlow = Flow[String].take(0)

    in ~> delimiter ~> lineFilter ~> bcast ~> Sink.onComplete(streamComplete)
    bcast ~> fileSink.async
    bcast ~> jsonTransformer2 ~> jsonFileSink.async
    bcast ~> modifiedKafkaSink(kafkaTopic) ~> kafka.sink.async


//    in ~> delimiter ~> lineFilter ~> bcast ~> merge ~> fileSink.async
//    bcast ~> jsonTransformer ~> merge

    //in ~> delimiter ~> lineFilter ~> Sink.ignore

//    in ~> delimiter ~> lineFilter ~> bcast ~> validReq ~> f2 ~> merge ~> out
//    bcast ~> invalidReq ~> blockingFlow ~> merge
    ClosedShape
  })

  def processFile(filename: String) = {


    val lineFilter = Flow[String]
      .filter(_.contains("Full JSON Payload is"))
      .map(_.split(" :: ")(1))

    val finalSink = Sink.foreach[String](logger.info(_))
    //val finalSink = Sink.ignore


//    val loggingMaterializer = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy { e =>
//      logger.info(s"Exception: $e")
//      Supervision.Stop
//    })

    FileIO.fromPath(Paths.get(filename))
      //.withAttributes(ActorAttributes.dispatcher("blocking-io-dispatcher"))

      .via(Framing.delimiter(ByteString("\n"), 100000, true).map(_.utf8String))
      .log("Stream Error")
      .via(lineFilter)
      //.alsoTo(lineSink(s"combined-file-${DateTimeFormatter.ofPattern("yyyy-dd-mm kk:hh:ss").format(LocalDateTime.now())}.log"))

      .withAttributes(
        Attributes.logLevels(
          //onElement = Logging.WarningLevel,
          onFinish = Logging.InfoLevel,
          onFailure = Logging.DebugLevel
        )
      )
//      .recoverWithRetries(10, {
//        case _: RuntimeException => {
//          logger.error( "Runtime Exeption ")
//          Source.single("Streaming Halted")
//        }
//      })
      .log("Stream Error")
      .mapAsync(1) { u =>
        Future.successful {

        }
      }


      //      .recover {
      //        case e: Exception => e.getMessage
      //      }
      //      .recoverWithRetries(attempts = 10, {
      //        case e: FramingException => Source.single(s"Unreadable line : ${e.getMessage}")
      //      })
//      .addAttributes(ActorAttributes.supervisionStrategy {
//        case ex: Throwable =>
//          logger.error("Error parsing row event: {}", ex)
//          Supervision.Restart
//      })
      //.withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider))
      .runWith(Sink.ignore)

  }

  def consumeKafka(topic: String): Unit = {
    kafka.source(topic)
      .log("Kafka Stream Insight")
      .withAttributes(
        Attributes.logLevels(
          //onElement = Logging.WarningLevel,
          onFinish = Logging.InfoLevel,
          onFailure = Logging.DebugLevel
        )
      )
      .map(_.value())
      .via(stringBufferedSink(2, 10.seconds))
      .alsoTo(Sink.foreach { i =>
        //
        val formattedJson = parse(i) match {
          case Right(json) => {
            json.spaces4
          }
          case Left(e) => {
            logger.error("Parse Error", e)
            "Not a valid JSON"
          }
        }
        logger.info(s"KAFKA :: ${formattedJson}")
      })
      .runWith(Sink.onComplete(streamComplete))
  }

  def parseLineWithRegex(line: String, eventDate: String): (Long, String, String, String) = {
    val regex = "(^\\w+) (\\w+-\\w+) : (\\d{2}:\\d{2}:\\d{2},\\d{3}) ([^ ]*)  \\[(.*?)\\] (\\w+) - (.*) \\[(.*?)\\] :: (.*)$"
    //val line = "default thread-x : 17:22:43,076 INFO  [TransactionLogger] 02RU01504F86819FGU69 - Full JSON Payload is [JsonReceiver Requests] :: {\"phone_no\":\"2348102249222\",\"cmd\":\"BNP\",\"bver\":\"4.0.1\",\"appid\":\"SkyeMobileIP\",\"d_id\":\"4\",\"alias\":\"POLARIS-1130277819\",\"act_type\":\"CA\",\"country\":\"NG\",\"osv\":\"Android 26 (8.0.0)\",\"imei\":\"355090086310168\",\"acn_no\":\"1130277819\"}"

    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(line)

    var eventTimestamp: Long = 0L
    var threadId: String = ""
    var reference: String = ""
    var json: String = ""

    while (matcher.find()) {
//      logger.info("Full match: " + matcher.group(0));
//      for(i <- 1 to matcher.groupCount) {
//        logger.info("Group " + i + ": " + matcher.group(i));
//      }
      val datetime = LocalDateTime.parse(eventDate + " " + matcher.group(3), DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss,SSS")).atZone(ZoneId.of("GMT+01:00"))
      eventTimestamp = datetime.toEpochSecond()
      threadId = matcher.group(2)
      reference = matcher.group(6)
      json = matcher.group(9)
    }
    (eventTimestamp, reference, threadId, json)
  }
}
