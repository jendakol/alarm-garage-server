package cz.jenda.alarm.garage

import cats.effect.Resource
import monix.eval.Task
import net.sigusr.mqtt.api.QualityOfService.AtLeastOnce
import net.sigusr.mqtt.api.RetryConfig.Custom
import net.sigusr.mqtt.api._
import retry.RetryPolicies

import scala.concurrent.duration._

object MqttModule {
  def subscribe(mqttConfiguration: MqttConfiguration, processMessage: Message => Task[Unit]): TaskResource[Unit] = {
    val retryConfig: Custom[Task] = Custom[Task](
      RetryPolicies
        .limitRetries[Task](5)
        .join(RetryPolicies.fullJitter[Task](2.seconds))
    )

    // TODO security
    val transportConfig = TransportConfig[Task](
      mqttConfiguration.host,
      mqttConfiguration.port,
      // TLS support looks like
      // 8883,
      // tlsConfig = Some(TLSConfig(TLSContextKind.System)),
      retryConfig = retryConfig,
      traceMessages = false
    )

    val sessionConfig = SessionConfig(
      clientId = mqttConfiguration.subscriberName,
      cleanSession = true,
      user = None,
      password = None,
      keepAlive = 5
    )

    val topics = Vector(mqttConfiguration.topic -> AtLeastOnce)

    for {
      session <- Session[Task](transportConfig, sessionConfig)
      _ <- Resource.make(session.subscribe(topics))(_ => session.unsubscribe(topics.map(_._1)))
      _ <- Resource.liftF(session.messages().evalMap(processMessage).compile.drain)
    } yield {}
  }

}
