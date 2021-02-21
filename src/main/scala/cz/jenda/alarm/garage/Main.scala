package cz.jenda.alarm.garage

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import net.sigusr.mqtt.api.Message
import protocol.alarm.State

import java.time.LocalDateTime

object Main extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = {
    val config = AppConfiguration.load

    MqttModule.subscribe(config.mqtt, processMessage).use(_ => Task.never) >> Task.now(ExitCode.Success)
  }

  def processMessage(m: Message): Task[Unit] = {
    Task {
      val bytes = m.payload.toArray
      val state = State.parseFrom(bytes)
      println(s"${LocalDateTime.now()}: $state")
    }
  }
}
