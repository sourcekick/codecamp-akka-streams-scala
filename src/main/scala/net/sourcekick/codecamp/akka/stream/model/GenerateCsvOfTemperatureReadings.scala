package net.sourcekick.codecamp.akka.stream.model

import java.nio.file.Paths
import java.time.Instant
import java.util.UUID

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Flow, Keep, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

object GenerateCsvOfTemperatureReadings {

  private val log: Logger = LoggerFactory.getLogger(GenerateCsvOfTemperatureReadings.getClass.getName)

  def main(args: Array[String]) {

    implicit val system = ActorSystem("TimeseriesCsvGenerator")
    implicit val materializer = ActorMaterializer()

    val start1 = Instant.now()

    generate("ordered.csv", 100L)

    val stop1 = Instant.now()
    val duration1 = java.time.Duration.between(start1, stop1).toMillis
    log.info("Took {} ms.", duration1)

    val start2 = Instant.now()

    generateShuffled("shuffled.csv", 200000L)

    val stop2 = Instant.now()
    val duration2 = java.time.Duration.between(start2, stop2).toMillis
    log.info("Took {} ms.", duration2)

    val terminationFuture = system.terminate()
    Await.result(terminationFuture, 100.seconds)
  }

  private val random = Random

  def generate(path: String, numberOfTemperatureReadings: Long)(implicit materializer: ActorMaterializer): Unit = {
    val first = TemperatureReading(UUID.randomUUID().toString,
                                   Instant.parse("2015-01-01T00:00:00Z"),
                                   20.0F,
                                   TemperatureUnit.Celsius)

    // source
    val source: Source[TemperatureReading, NotUsed] = Source.unfold(first)(previous => {
      val t = generateTemperatureReading(previous)
      Option((t, t))
    })

    // flow
    val flow: Flow[TemperatureReading, ByteString, NotUsed] =
      Flow.apply.map(t => ByteString(t.toCsv + "\n"))

    // sink
    val targetPath = Paths.get(path)
    log.info("Going to stream generated time series into file " + targetPath)
    val sink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(targetPath)

    // runnable graph
    val rg: RunnableGraph[Future[IOResult]] =
      source.take(numberOfTemperatureReadings).via(flow).async.toMat(sink)(Keep.right)

    // run
    val ioResult = rg.run()
    val result = Await.result(ioResult, 20000.seconds)
    if (!result.wasSuccessful) {
      log.info("Exception.", result.getError)
    } else {
      log.info("Finished successful.")
    }
  }

  def generateShuffled(path: String, numberOfTemperatureReadings: Long)(
      implicit materializer: ActorMaterializer): Unit = {
    val first = TemperatureReading(UUID.randomUUID().toString,
                                   Instant.parse("2015-01-01T00:00:00Z"),
                                   20.0F,
                                   TemperatureUnit.Celsius)

    // source
    val source: Source[TemperatureReading, NotUsed] = Source.unfold(first)(previous => {
      val t = generateTemperatureReading(previous)
      Option((t, t))
    })

    // shuffle flow
    val shuffleFlow: Flow[TemperatureReading, TemperatureReading, NotUsed] =
      Flow.apply
        .sliding(100, 100)
        .map((s: immutable.Seq[TemperatureReading]) => Random.shuffle(s))
        .flatMapConcat(s => Source.fromIterator(() => s.iterator))

    // flow
    val flow: Flow[TemperatureReading, ByteString, NotUsed] =
      Flow.apply.map(t => ByteString(t.toCsv + "\n"))

    // sink
    val targetPath = Paths.get(path)
    log.info("Going to stream generated time series into file " + targetPath)
    val sink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(targetPath)

    // runnable graph
    val rg: RunnableGraph[Future[IOResult]] =
      source.take(numberOfTemperatureReadings).via(shuffleFlow).via(flow).async.toMat(sink)(Keep.right)

    // run
    val ioResult = rg.run()
    val result = Await.result(ioResult, 20000.seconds)
    if (!result.wasSuccessful) {
      log.info("Exception.", result.getError)
    } else {
      log.info("Finished successful.")
    }
  }

  private def generateTemperatureReading(previous: TemperatureReading): TemperatureReading = {
    TemperatureReading(UUID.randomUUID().toString,
                       previous.instant.plusSeconds(15L),
                       previous.temperature + random.nextFloat() - 0.5F,
                       TemperatureUnit.Celsius)
  }

}