package controllers

import java.util
import javax.inject._

import com.github.kaeluka.spencer.analysis.SpencerGraphImplicits._
import com.github.kaeluka.spencer.analysis._
import com.github.kaeluka.spencer.tracefiles.SpencerDB
import org.apache.spark.graphx.VertexId
import org.apache.spark.rdd.RDD
import play.api.cache.{CacheApi, Cached}
import play.api.data.Forms._
import play.api.data.{Form, _}
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import play.api.mvc._
import play.twirl.api.Html

import scala.concurrent.Future

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class MainController @Inject()(lifecycle: ApplicationLifecycle,
                               messagesApi : MessagesApi,
                               cached: Cached) extends Controller {

  var dbMap : Map[String, SpencerDB] = Map[String, SpencerDB]()

  def getDB(name: String): SpencerDB = {
    if (! this.dbMap.contains(name)) {
      val db: SpencerDB = new SpencerDB(name)
      db.connect()
      this.dbMap = this.dbMap + (name -> db)
      this.lifecycle.addStopHook { () => Future.successful(db.shutdown()) }
    }
    this.dbMap(name)
  }

  def index = cached("index_page") {
    Action { implicit request =>
      implicit val db = getDB("test")
      Ok(views.html.index())
    }
  }

  def playground = Action { implicit request =>
    Ok(views.html.playground())
  }
}
