package org

import language.implicitConversions
import language.postfixOps
import language.existentials



import collection._


package object scalameter {

  type KeyValue = (Key[T], T) forSome { type T }

  private[scalameter] object dyn {
    val currentContext = new MonadicDynVar(Context.topLevel)
    val log = new MonadicDynVar[Log](Log.Console)
    val events = new MonadicDynVar[Events](Events.None)
  }

  def currentContext: Context = dyn.currentContext.value

  def log: Log = dyn.log.value

  def events: Events = dyn.events.value

  val inline = new MeasureBuilder[Unit, Double](
    Context.inlineBenchmarking,
    Warmer.Zero,
    MeasureBuilder.timeMeasurer,
    MeasureBuilder.unitRegen,
    MeasureBuilder.doNothing,
    MeasureBuilder.doNothing,
    MeasureBuilder.average
  )

  /* decorators */

  @deprecated("Use Aggregator.apply", "0.5")
  implicit def fun2ops(f: Seq[Double] => Double) = new {
    def toAggregator(n: String) = Aggregator(n)(f)
  }

  /* misc */

  import utils.ClassPath

  @deprecated("Use utils.ClassPath.default", "0.5")
  def defaultClasspath = ClassPath.default

  @deprecated("Use utils.ClassPath.extract", "0.5")
  def extractClasspath(classLoader: ClassLoader, default: => String): String =
    ClassPath.extract(classLoader, default)

}


package scalameter {

  case class MeasurementData(complete: Seq[Double], success: Boolean) 
  
}
