import java.util.UUID

import com.amazonaws.services.kinesis.producer.{UserRecordFailedException, UserRecordResult}
import com.typesafe.config.ConfigFactory
import com.weightwatchers.core.eventing.models.ProducerEvent
import com.weightwatchers.core.eventing.producer.KinesisProducerKPL

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Success

/**
  * Created by david.de on 2/21/17.
  */
object Producer extends App {
  val kinesisConfig = ConfigFactory.load().getConfig("kinesis")
  val producerConfig = kinesisConfig.getConfig("my-producer")
  val streamName = producerConfig.getString("stream-name")

  val kpl = KinesisProducerKPL(producerConfig.getConfig("kpl"), streamName)

  var counter = 0

  var fProducerEvents: ListBuffer[Future[UserRecordResult]] = new ListBuffer[Future[UserRecordResult]]()

  for(i <- 1 to 1000) {

    val producerEvent = ProducerEvent(UUID.randomUUID.toString, String.valueOf(counter))

    println("Adding Records!")
    val callback: Future[UserRecordResult] = kpl.addUserRecord(producerEvent)

    fProducerEvents += callback
  }

  val futureList: Future[List[UserRecordResult]] = Future.sequence(fProducerEvents.toList)

  futureList onSuccess {
    case _ =>
      println("Successful!")
  }

  Await.ready(
    futureList,
    20 seconds)

}
