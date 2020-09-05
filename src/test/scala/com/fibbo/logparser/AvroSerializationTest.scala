package com.fibbo.logparser

import java.io.File

import com.fibbo.logparser.domain.{Greeting, GreetingEmotion}
import org.apache.avro.file.{DataFileReader, DataFileWriter}
import org.apache.avro.io.{DatumReader, DatumWriter}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}
import org.scalatest.FunSuite

class AvroSerializationTest extends FunSuite {

//  test("Test using file (de)serialization") {
//    val sadGreeting = Greeting(greetingEmotion = GreetingEmotion.SADNESS, greetingDetail = "Boo hoo! Hello.")
//
//    val GreetingFileName = "greetingTest.avro"
//    val GreetingAvroFile = new File(GreetingFileName)
//    GreetingAvroFile.delete()
//
////    val userDatumWriter = new SpecificDatumWriter[Nothing](classOf[Nothing])
////    val dataFileWriter = new DataFileWriter[Nothing](userDatumWriter)
////    dataFileWriter.create(user1.getSchema, new Nothing("users.avro"))
////    dataFileWriter.append(user1)
////    dataFileWriter.append(user2)
////    dataFileWriter.append(user3)
//
//    // serialize
//    val speculativeScreenplayDatumWriter: DatumWriter[Greeting] = new SpecificDatumWriter[Greeting](Greeting.SCHEMA$)
//    val dataFileWriter: DataFileWriter[Greeting] = new DataFileWriter[Greeting](speculativeScreenplayDatumWriter)
//    dataFileWriter.create(Greeting.SCHEMA$, GreetingAvroFile)
//    dataFileWriter.append(sadGreeting)
//    dataFileWriter.close()
//
//    // Deserialize from disk
//    val speculativeScreenplayDatumReader: DatumReader[Greeting] = new SpecificDatumReader[Greeting](Greeting.SCHEMA$)
//    val dataFileReader: DataFileReader[Greeting] =
//      new DataFileReader[Greeting](new File(GreetingFileName), speculativeScreenplayDatumReader)
//
//    var deserialisedGreeting: Greeting = null // a var ... OMG, ahhhhhhh - avert your eyes!!!!
//    while (dataFileReader.hasNext) {
//      deserialisedGreeting = dataFileReader.next(deserialisedGreeting)
//      println("okay: " + deserialisedGreeting + "; dokies")
//    }
//
//    GreetingAvroFile.delete()
//
//    assert(sadGreeting.greetingEmotion == GreetingEmotion.SADNESS)
//  }

}