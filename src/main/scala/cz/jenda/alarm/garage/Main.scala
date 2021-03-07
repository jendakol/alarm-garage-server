package cz.jenda.alarm.garage

import cats.effect.ExitCode
import com.avast.sst.micrometer.statsd.{MicrometerStatsDConfig, MicrometerStatsDModule}
import cz.jenda.cats.micrometer.DefaultCatsMeterRegistry
import io.micrometer.core.instrument.config.NamingConvention
import monix.eval.{Task, TaskApp}
import net.sigusr.mqtt.api.Message
import protocol.alarm.{Report, State}

import java.time.{Duration, LocalDateTime, ZoneOffset}
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.concurrent.duration._

object Main extends TaskApp {
  private lazy val lastState: AtomicReference[Option[State]] = new AtomicReference(None)

  private val totalTime = new AtomicReference[Double](0)
  private val totalTimeCount = new AtomicInteger()
  private val totalEventsCount = new AtomicInteger()

  override def run(args: List[String]): Task[ExitCode] = {
    val config = AppConfiguration.load

    val statsDConfig = MicrometerStatsDConfig(host = "statsd.jenda.eu", prefix = "test.garage.", step = 10.seconds, buffered = false)

    val program = for {
      metricsRegistry <- MicrometerStatsDModule.make[Task](statsDConfig, namingConvention = Some(NamingConvention.dot))
      metrics <- DefaultCatsMeterRegistry.wrap(metricsRegistry)
      _ <- MqttModule.subscribe(config.mqtt, processMessage)
    } yield {
      metrics
    }

    program.use { metrics =>
      metrics.gauge("started")(() =>
        lastState.get() match {
          case Some(state) => if (state.started) 1 else 0
          case None        => 0
        }
      ) *> metrics.gauge("armed")(() =>
        lastState.get() match {
          case Some(state) => if (state.armed) 1 else 0
          case None        => 0
        }
      ) *> metrics.gauge("doorsOpen")(() =>
        lastState.get() match {
          case Some(state) => if (state.doorsOpen) 1 else 0
          case None        => 0
        }
      ) *> metrics.gauge("modemSleeping")(() =>
        lastState.get() match {
          case Some(state) => if (state.modemSleeping) 1 else 0
          case None        => 0
        }
      ) *> metrics.gauge("reconnecting")(() =>
        lastState.get() match {
          case Some(state) => if (state.reconnecting) 1 else 0
          case None        => 0
        }
      ) *> Task.never[ExitCode]
    }
  }

  def processMessage(m: Message): Task[Unit] = {
    Task {
      val bytes = m.payload.toArray
      val report = Report.parseFrom(bytes)

      lastState.set(Some(report.state))

      val time = report.timestamp.map { t =>
        LocalDateTime.ofEpochSecond(t / 1000, ((t % 1000) * 1000000).toInt, ZoneOffset.ofHours(1))
      }

      val now = LocalDateTime.now()
      val diff = time.map(Duration.between(_, now))

      if (totalEventsCount.getAndIncrement() > 0) {
        diff.foreach { d =>
          totalTime.updateAndGet((t: Double) => t + d.toMillis.toDouble.abs / 1000)
          totalTimeCount.incrementAndGet()
        }
      }

      println(s"$now/$time/$diff: ${report.state}")

      if (totalTimeCount.get() > 0 && totalTimeCount.get() % 20 == 0) {
        println(s"AVG diff: ${totalTime.get() / totalTimeCount.get()}s")
      }
    }.attempt *> Task.unit
  }
}
