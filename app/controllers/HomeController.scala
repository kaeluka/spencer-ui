package controllers

import javax.inject._

import play.api._
import play.api.mvc._
import com.github.kaeluka.spencer.analysis._
import com.github.kaeluka.spencer.tracefiles.SpencerDB
import com.github.kaeluka.spencer.analysis.SpencerGraphImplicits._
import org.apache.spark.graphx.VertexId
import org.apache.spark.rdd.RDD
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController (val db : SpencerDB, lifecycle: ApplicationLifecycle) extends Controller {

  @Inject()
  def this(lifecycle: ApplicationLifecycle) = {
    this(new SpencerDB("test"), lifecycle)
    this.db.connect()
    lifecycle.addStopHook {() => Future.successful(this.db.shutdown())}
  }

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def objectQueryResultToHtml(objects: Array[VertexId]) : String = {

    val head = "<div class=\"result-size\">"+objects.size+" objects</div>"
    val table = objects.map(
      "<tr>\n" +
        "<td>"+_+"</td>\n" +
        "</tr>").mkString("<table class=\"result-table\">", "\n", "</table>")

    head+table
  }

  def query = Action { implicit req =>
    req.getQueryString("q") match {
      case Some(q) =>
        implicit val data: SpencerData = this.db
        //
        val query: Option[SpencerAnalyser[RDD[VertexId]]] = QueryParser.parseObjQuery(q)

        query match {
          case Some(q) =>
            val rdd: Array[VertexId] = q.analyse.collect()
            val innerHtml = objectQueryResultToHtml(rdd)

//            Scratch.run
            Ok(views.html.result(q.toString, innerHtml))
          case None =>
            NotAcceptable("could not parse the query")
        }

      case None =>
        NotAcceptable("need a query")
    }
  }

}
