package cz.jenda.alarm.garage

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import net.sigusr.mqtt.api.Message

object Main extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = {
    val config = AppConfiguration.load

    MqttModule.subscribe(config.mqtt, processMessage).use(_ => Task.never) >> Task.now(ExitCode.Success)
  }

  def processMessage(m: Message): Task[Unit] = {
    Task {
      println(new String(m.payload.toArray))
    }
  }
}
