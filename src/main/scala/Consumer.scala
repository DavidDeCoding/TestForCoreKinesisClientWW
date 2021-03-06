import java.io.{File, FileOutputStream, PrintWriter}
import java.util.concurrent.CountDownLatch

import akka.actor.{Actor, Props}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.weightwatchers.core.eventing.consumer.ConsumerWorker.{ConsumerShutdown, ConsumerWorkerFailure, EventProcessed, ProcessEvent}
import com.weightwatchers.core.eventing.consumer.KinesisConsumer
import com.weightwatchers.core.eventing.consumer.KinesisConsumer.ConsumerConf
import com.weightwatchers.core.eventing.models.{CompoundSequenceNumber, ConsumerEvent}

import scala.collection.mutable
import scala.concurrent.Future

/**
  * Created by david.de on 2/17/17.
  */

class TestEventProcessor(shutdownLatch: CountDownLatch) extends Actor with LazyLogging {
  import scala.concurrent.duration._

  implicit val timeout = akka.util.Timeout(5 minutes)

  val pq: mutable.PriorityQueue[CompoundSequenceNumber] =
    mutable.PriorityQueue.empty[CompoundSequenceNumber](
      implicitly[Ordering[CompoundSequenceNumber]].reverse
    )

  val checkpointlogFileName = "./checkpointlog"
  val writer = new PrintWriter(
    new FileOutputStream(
      new File(checkpointlogFileName),
      true))

  override def receive: Receive = {
    case ProcessEvent(event) =>
      pq += event.sequenceNumber

      if (pq.size == 1) {

        while (pq.nonEmpty) {
          val currentSequenceNumber = pq.dequeue
          writer.append("\n" + currentSequenceNumber.sequenceNumber)
          writer.flush()
          sender ! EventProcessed(currentSequenceNumber)
        }

        writer.append("\n--------------------------------------------------------------")
        writer.flush()

        // To test Graceful shutdown by not acking.
        context.become(notAckableReceive())

        // To test Graceful shutdown by hook.
        shutdownLatch.countDown()


      }
  }

  def notAckableReceive(): Receive = {
    case ProcessEvent(event) =>
  }

}


object Consumer extends App {

  val system = akka.actor.ActorSystem.create("test-system")
  val config = ConfigFactory.load()
  val shutdown = new CountDownLatch(1)
  val eventProcessor = system.actorOf(Props(new TestEventProcessor(shutdown)), "test-processor")
  implicit val executor = system.dispatcher
  val consumer = KinesisConsumer(ConsumerConf(config.getConfig("kinesis"), "my-consumer"), eventProcessor, system)

  Future {
    consumer.start()
  }


  // To test Graceful shutdown by hook.
  shutdown.await()
  System.exit(0)
}
