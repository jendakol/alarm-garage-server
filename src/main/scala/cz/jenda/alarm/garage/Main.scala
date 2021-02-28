package cz.jenda.alarm.garage

import cats.effect.ExitCode
import com.avast.metrics.scalaapi.Monitor
import com.avast.metrics.statsd.StatsDMetricsMonitor
import com.avast.sst.micrometer.statsd.{MicrometerStatsDConfig, MicrometerStatsDModule}
import cz.jenda.cats.micrometer.DefaultCatsEffectMeterRegistry
import io.micrometer.core.instrument.config.NamingConvention
import monix.eval.{Task, TaskApp}
import net.sigusr.mqtt.api.Message
import protocol.alarm.{Report, State}

import java.time.{Duration, LocalDateTime, ZoneId, ZoneOffset}
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration._

object Main extends TaskApp {
  private lazy val monitor = Monitor(
    new StatsDMetricsMonitor("statsd.jenda.eu", 8125, "test.garage", Duration.ofSeconds(30), Executors.newScheduledThreadPool(1))
  )

  private lazy val lastState: AtomicReference[Option[State]] = new AtomicReference(None)

  override def run(args: List[String]): Task[ExitCode] = {
    val config = AppConfiguration.load

    val statsDConfig = MicrometerStatsDConfig(host = "statsd.jenda.eu", prefix = "test.garage.", step = 10.seconds, buffered = false)

    val program = for {
      metricsRegistry <- MicrometerStatsDModule.make[Task](statsDConfig, namingConvention = Some(NamingConvention.dot))
      metrics <- DefaultCatsEffectMeterRegistry.wrap(metricsRegistry)
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

//      for {
//        _ <- metrics.gauge("started")(() =>
//          lastState.get() match {
//            case Some(state) => if (state.started) 1 else 0
//            case None        => 0
//          }
//        )
//        _ <- metrics.gauge("armed")(() =>
//          lastState.get() match {
//            case Some(state) => if (state.armed) 1 else 0
//            case None        => 0
//          }
//        )
//        _ <- metrics.gauge("doorsOpen")(() =>
//          lastState.get() match {
//            case Some(state) => if (state.doorsOpen) 1 else 0
//            case None        => 0
//          }
//        )
//        _ <- metrics.gauge("modemSleeping")(() =>
//          lastState.get() match {
//            case Some(state) => if (state.modemSleeping) 1 else 0
//            case None        => 0
//          }
//        )
//        _ <- metrics.gauge("reconnecting")(() =>
//          lastState.get() match {
//            case Some(state) => if (state.reconnecting) 1 else 0
//            case None        => 0
//          }
//        )
//        _ <- Task.never[Unit]
//      } yield {
//        ExitCode.Success
//      }
    }

//    monitor.gauge[Int]("started")(() =>
//      lastState.get() match {
//        case Some(state) => if (state.started) 1 else 0
//        case None        => 0
//      }
//    )
//
//    monitor.gauge[Int]("armed")(() =>
//      lastState.get() match {
//        case Some(state) => if (state.armed) 1 else 0
//        case None        => 0
//      }
//    )
//
//    monitor.gauge[Int]("doorsOpen")(() =>
//      lastState.get() match {
//        case Some(state) => if (state.doorsOpen) 1 else 0
//        case None        => 0
//      }
//    )
//
//    monitor.gauge[Int]("modemSleeping")(() =>
//      lastState.get() match {
//        case Some(state) => if (state.modemSleeping) 1 else 0
//        case None        => 0
//      }
//    )
//
//    monitor.gauge[Int]("reconnecting")(() =>
//      lastState.get() match {
//        case Some(state) => if (state.reconnecting) 1 else 0
//        case None        => 0
//      }
//    )
//
//    MqttModule.subscribe(config.mqtt, processMessage).use(_ => Task.never) *> Task.now(ExitCode.Success)
  }

  def processMessage(m: Message): Task[Unit] = {
    Task {
      // TODO prevent total failure of whole stream

      val bytes = m.payload.toArray
      val report = Report.parseFrom(bytes)

      lastState.set(Some(report.state))

      val time = report.timestamp.map(LocalDateTime.ofEpochSecond(_, 0, ZoneOffset.ofHours(1)))

      println(s"${LocalDateTime.now()}/$time: ${report.state}")
    }
  }
}
