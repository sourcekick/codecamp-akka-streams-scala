package net.sourcekick.codecamp.akka.stream.kata01

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import org.scalatest.{AsyncWordSpec, Matchers, ParallelTestExecution}

import scala.collection.immutable.Seq

class Kata01NumberSourceSpec extends AsyncWordSpec with Matchers with ParallelTestExecution {

  /*
. Please do not read this test code while solving any of the katas! Spoiler warning!
.
. Just run it with right-click on the test class name above.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
   */

  "Kata01NumberSource" must {

    "create the correct source" in {

      implicit val system = ActorSystem("Kata01")
      implicit val materializer = ActorMaterializer()

      val numberSource = Kata01NumberSource.createSourceOfNaturalNumbers1to10() // Source[Int, NotUsed]

      val runnableGraph = numberSource.toMat(Sink.seq)(Keep.right) // RunnableGraph[Future[Seq[Int]]]

      val future = runnableGraph.run() // Future[Seq[Int]]

      future.map(integers => {
        integers.size shouldBe 10
        integers shouldBe Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
      })
    }
  }

}