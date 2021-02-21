package cz.jenda.alarm

import cats.effect.Resource
import monix.eval.Task

package object garage {
  type TaskResource[A] = Resource[Task, A]
}
