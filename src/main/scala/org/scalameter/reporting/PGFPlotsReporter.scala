package org.scalameter
package reporting



import java.io._
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import org.scalameter.utils.Tree
import scala.collection._
import scala.Numeric.Implicits._
import scala.Fractional.Implicits._



/** Produces a pgfplots-based tex file that can be embedded into a Latex document.
 */
case class PGFPlotsReporter[T: Fractional](
  errorBars: Boolean = true,
  plotColors: Seq[String] = Seq(
    "fill={rgb:red,1;green,4;blue,5},",
    """fill={rgb:red,1;green,3;blue,1},
    postaction={
      pattern color=white,
      pattern=north east lines
    },""",
    """fill={rgb:red,5;green,1;blue,1},
    postaction={
      pattern color=white,
      pattern=north west lines
    },""",
    """fill=white,
    postaction={
      pattern color={rgb:red,1;green,4;blue,5},
      pattern=crosshatch
    },""",
    "fill=white",
  )
) extends Reporter[T] {

  val sep = File.separator

  def report(result: CurveData[T], persistor: Persistor) {
  }

  def report(result: Tree[CurveData[T]], persistor: Persistor) = {
    val resultdir = currentContext(Key.reports.resultDir)

    new File(s"$resultdir").mkdirs()

    def reportScope(scope: (Context, Seq[CurveData[T]])): Unit = {
      val (ctx, curves) = scope
      val filename = s"$resultdir$sep${ctx.scope}.tex"
      var writer: PrintWriter = null

      if (curves.isEmpty) return

      try {
        writer = new PrintWriter(new FileWriter(filename, false))
        PGFPlotsReporter.writeContext(ctx, curves, writer)
      } finally {
        if (writer != null) writer.close()
      }
    }

    result.scopes.foreach(reportScope)

    true
  }

  def writeContext[T: Fractional](
    ctx: Context, curves: Seq[CurveData[T]], pw: PrintWriter
  ): Unit = {
    import pw._
    import pw.{print => p}

    val formatter = new DecimalFormat
    formatter.setMaximumFractionDigits(3)
    def round(x: Double): String = formatter.format(x)

    val keys = curves.head.measurements.head.params.axisData.keys
    assert(keys.size == 1)
    val xlabel = keys.head.fullName
    val fract = implicitly[Fractional[T]]
    var ymin: Double = Double.MaxValue
    var ymax: Double = Double.MinValue
    val paramvalues = mutable.TreeSet[Double]()
    for (cd <- curves; m <- cd.measurements) {
      val y = fract.toDouble(m.data.avg)
      ymin = math.min(y, ymin)
      ymax = math.max(y, ymax)
      paramvalues += m.parameters(xlabel)
    }
    val ymaxUp = math.pow(10, math.floor(math.log10(ymax)))
    val xCoords = paramvalues.mkString(", ")
    val yTicks = (1 to 8).map(n => round((ymaxUp - 0.0) * n / 8)).mkString(", ")
    val yMaxValue = s"${ymaxUp}"
    val header = s"""\\begin{tikzpicture}[scale=0.80]
\\begin{axis}[
  height=5.0cm,
  every axis plot post/.style={/pgf/number format/fixed},
  ylabel=$$footprint / MB$$,
  xlabel=$$\\#keys$$,
  grid=both,
  every axis y label/.style={
    at={(ticklabel* cs:1.05)},
    xshift=35,
    yshift=-15,
    anchor=south
  },
  every axis x label/.style={
    at={(ticklabel* cs:1.05)},
    xshift=-8,
    yshift=-15,
    anchor=north,
    rotate=-90,
  },
  scaled y ticks=false,
  yticklabel style={
    /pgf/number format/fixed,
  },
  xticklabel style={
    rotate=-90,
  },
  ytick={$yTicks},
  ybar=0.1pt,
  bar width=4.4pt,
  x=0.485cm,
  ymin=0,
  ymax=$yMaxValue,
  x,
  axis on top,
  xtick=data,
  enlarge x limits=0.06,
  symbolic x coords={
    $xCoords
  },
  % restrict y to domain*=0:75,
  visualization depends on=rawy\\as\\rawy,
  after end axis/.code={
    \\draw[line width=3pt, white, decoration={snake, amplitude=1pt}, decorate] (rel axis cs:0,1.05) -- (rel axis cs:1,1.05);
  },
  axis lines*=left,
  clip=false,
  legend style={
    legend columns=3,
    at={(0.025, 0.90)},
    anchor=north west
  },
  draw opacity=0.4,
  major grid style={
    draw opacity=0.3,
  },
]
    """
    p(header)

    for ((curve, i) <- curves.zipWithIndex) {
      val addplot = s"""
\\addplot[
  fill=${plotColors(i)},
  ${if (errorBars) "error bars/.cd," else ""}
  error bar style=red,
  y dir=both,
  y explicit,
]
      """
      p(addplot)
    }

    val footer = s"""
\\end{axis}
\\end{tikzpicture}"""
    p(footer)
  }
}