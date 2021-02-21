package cz.jenda.alarm.garage

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}

object Main extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = Task { println("Hello world") } >> Task.now(ExitCode.Success)
}
