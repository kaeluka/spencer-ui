package controllers

import play.api.libs.json.Json
import javax.inject._

import com.github.kaeluka.spencer.analysis.SpencerGraphImplicits._
import com.github.kaeluka.spencer.analysis._
import org.apache.spark.graphx.VertexId
import play.api.cache.Cached
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json.toJson
import play.api.mvc._

import scala.concurrent.duration._

case class QueryDataItem(oid: VertexId, klass: Option[String], allocationSite: Option[String])
case class QueryData(dbname: String, query: String, explanation: String, data: Seq[QueryDataItem])

object QueryControllerUtil {

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
class QueryController @Inject()(lifecycle: ApplicationLifecycle,
                                messagesApi : MessagesApi,
                                mainC: MainController,
                                cached: Cached) extends Controller {

  def json_meta(dbname: String, q: String = "Obj()") = cached(
    {_: RequestHeader => s"json/$q"},
    6.hours.toSeconds.asInstanceOf[Int]) {
    Action { implicit req =>
      implicit val data: SpencerData = mainC.getDB(dbname)
      QueryParser.parseObjQuery(q) match {
        case Right(qObj) =>
          val objs = WithMetaInformation(qObj).analyse.collect()
          Ok(Json.obj(
            "query"   -> q,
            "dbname"  -> dbname,
            "objects" -> Json.toJson(objs.map(
              o => Json.toJson(
                Map(
                  "oid"            -> Some(toJson(o.oid)),
                  "allocationsite" -> o.allocationSite.map(toJson(_)),
                  "class"          -> o.klass.map(toJson(_)),
                  "firstUsage"     -> Some(toJson(o.firstUsage)),
                  "lastUsage"      -> Some(toJson(o.lastUsage)),
                  "thread"         -> o.thread.map(toJson(_))
                ).filter(_._2.isDefined)
              )
            ))))
        case Left(_) =>
          NotAcceptable("could not parse the query '" + q)
      }
    }
  }
  def json_select(dbname: String, q: String) =
//    cached(
//      {_: RequestHeader => s"json/$q"},
//      6.hours.toSeconds.asInstanceOf[Int]) {
      Action { implicit req =>
        implicit val data: SpencerData = mainC.getDB(dbname)
        QueryParser.parseObjQuery(q) match {
          case Right(qObj) =>
            val objs = qObj.analyse.collect()
            Ok(Json.obj(
              "query"   -> q,
              "objects" -> objs
            ))
          case Left(_) =>
            NotAcceptable("could not parse the query '" + q)
        }

      }
//  }

  def query(dbname: String, qs: String) = cached(
    {_: RequestHeader => s"query/$qs"},
    6.hours.toSeconds.asInstanceOf[Int]) {
    Action { implicit req =>
      implicit val data: SpencerData = mainC.getDB(dbname)
      //
      val eitherQueries = qs.split("/").map(QueryParser.parseObjQuery(_))

      if (eitherQueries.exists(_.isLeft)) {
        NotAcceptable("could not parse the query '" + qs + "':\n" + eitherQueries.filter(_.isLeft).mkString(", "))
      } else {
        println("================================ " + qs + " -parsed-> " + eitherQueries.mkString(", "))
        val queries = eitherQueries.map(_.right.get)
        Ok(views.html.query(dbname, queries))
      }
    }
  }
}

