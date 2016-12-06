package controllers

import java.util
import javax.inject._

import scala.concurrent.duration._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import com.github.kaeluka.spencer.analysis._
import com.github.kaeluka.spencer.tracefiles.SpencerDB
import com.github.kaeluka.spencer.analysis.SpencerGraphImplicits._
import org.apache.spark.graphx.VertexId
import org.apache.spark.rdd.RDD
import play.api.cache.Cached
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

case class PerObjDataItem(oid: VertexId, klass: Option[String], allocationSite: Option[String])
case class PerObjData(dbname: String, query: String, data: Seq[PerObjDataItem])

object PerObjControllerUtil {

  def classNameToTd(dbname: String, query: String, optKlass: Option[String]): String = {

    val inner = optKlass match {
      case Some(klass) =>
        klass +
          s" <a class='hint' href='" + routes.PerObjController.perobj(dbname, "InstanceOfClass(" + klass + ")") +"'>all instances</a>" +
          " <a class='hint' href='" + routes.PerObjController.perobj(dbname, "And("+query+ " InstanceOfClass(" + klass + "))") +"'>.. + all instances</a>" +
          (if (! klass.startsWith("[")) {
            " <a class='hint' href='" + routes.SourceCodeController.query(dbname, klass) +"'>view source</a>"
          } else {
            ""
          })
      case None =>
        "<span class='empty'>N/A</span>"
    }

    s"      <td>$inner</td>\n"
  }

}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class PerObjController @Inject()(lifecycle: ApplicationLifecycle,
                                 messagesApi : MessagesApi,
                                 mainC: MainController,
                                 cached: Cached) extends Controller {

  def perobj(dbname: String, qs: String) = cached(
    {_: RequestHeader => s"perobj/$qs"},
    6.hours.toSeconds.asInstanceOf[Int]) {
    Action { implicit req =>
      implicit val data: SpencerData = mainC.getDB(dbname)
      //
      val queries = qs.split("/").map(QueryParser.parseObjQuery(_))

      if (queries.exists(_.isLeft)) {
        NotAcceptable("could not parse the query '" + qs + "':\n" + queries.filter(_.isLeft).mkString(", "))
      } else {
        println("================================ " + qs + " -parsed-> " + queries.mkString(", "))
        val results = queries.map {
          case Right(qObj) =>
            val objects = WithMetaInformation(qObj).analyse.collect().map(
              o => PerObjDataItem(o.oid, o.klass, o.allocationSite)
            ).toList
            PerObjData(dbname, qObj.toString, objects)
        }
        val allObjs = Obj().analyse.collect.toSet
        Ok(views.html.result(dbname, qs, "todo: remove", allObjs, results))
      }
    }
  }
}

