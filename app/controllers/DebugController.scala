package controllers

import java.util
import java.util.Map.Entry
import java.util.function.Consumer
import javax.inject._

import akka.actor.ActorSystem
import com.datastax.driver.core.TableMetadata
import com.fasterxml.jackson.databind.JsonNode
import com.github.kaeluka.spencer.analysis.SpencerDB
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._
import play.libs.Json
import play.twirl.api.Html

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

@Singleton
class DebugController @Inject()(ws: WSClient, mainController: MainController)(implicit val executionContext: ExecutionContext) extends Controller {

  def debug = Action.async {
    mainController.getDB("test") //as a side effect, starts a cluster
    val response: Future[WSResponse] = ws.url("http://127.0.0.1:4040/api/v1/applications/").get()
    response.map(r=> {
      val json: JsonNode = Json.parse(r.body)
      var i = 0
      var appId = None.asInstanceOf[Option[String]]
      while (json.has(i) && appId.isEmpty) {
        if (json.get(i).get("name").toString.equals("\"spencer-analyse\"")) {
          appId = Some(json.get(i).get("id").toString)
        }
        i += 1
      }
      appId.get.replace("\"", "")
    }).flatMap(appId => {
      val jobs = ws.url(s"http://127.0.0.1:4040/api/v1/applications/$appId/jobs").get()
      jobs.map(r=> Ok(views.html.debug(Json.parse(r.body))))
    }).recover {
      case e => ServiceUnavailable(Html(e.toString))
    }
  }

  def clearCaches(dbname: String) = Action {
    val db = mainController.getDB(dbname)
    db.clearCaches(Some(dbname))
//    val tables = db.session.getCluster.getMetadata.getKeyspace(dbname).getTables
//    tables.forEach(new Consumer[TableMetadata] {
//      override def accept(t: TableMetadata): Unit = {
//        if (t.getName.startsWith("cache_")) {
//          db.session.execute(s"DROP TABLE ${t.getName}")
//        }
//      }
//    })
    Ok("all caches cleared")

  }
}
