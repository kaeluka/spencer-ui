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
class SpencerUIController(val db : SpencerDB, lifecycle: ApplicationLifecycle) extends Controller {

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

  def objectQueryResultAsHtml(implicit db: SpencerData, objects: Array[(VertexId, Option[String], Option[String])]) : String = {

    //val head = "<div class=\"result-size \">"+objects.size+" objects</div>"

    val head =
      "  <thead>\n" +
        "    <tr>\n" +
        "      <th>Obj. ID</th>\n" +
        "      <th>Class</th>\n" +
        "      <th>Allocation Site</th>\n"+
        "    </tr>\n" +
        "  </thead>\n"
    val table = objects.map {
      case (id, optKlass, optAllocationSite) =>
        "    <tr>\n" +
          "      <td class=\"object-id\">" + id + "</td>\n" +
          "      <td>" + optKlass.getOrElse("N/A") + "</td>\n" +
          "      <td>" + optAllocationSite.getOrElse("N/A") + "</td>\n" +
          "    </tr>"
    }.mkString("<table id=\"resultTable\" class=\"tablesorter\">\n"+head+"\n  <tbody>\n", "\n", "\n  </tbody>\n</table>")

    table
  }

  def query = Action { implicit req =>
    req.getQueryString("q") match {
      case Some(q) =>
        implicit val data: SpencerData = this.db
        //
        val query: Option[SpencerAnalyser[RDD[VertexId]]] = QueryParser.parseObjQuery(q)

        query match {
          case Some(q) =>
            val objects = WithMetaInformation(q).analyse.collect()
            val innerHtml = objectQueryResultAsHtml(data, objects)

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
