package controllers

import javax.inject._

import com.github.kaeluka.spencer.PostgresSpencerDB
import play.api.cache.Cached
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import play.api.mvc._

import scala.concurrent.Future

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class MainController @Inject()(lifecycle: ApplicationLifecycle,
                               messagesApi : MessagesApi,
                               cached: Cached) extends Controller {

  var dbMap : Map[String, PostgresSpencerDB] = Map[String, PostgresSpencerDB]()

  def getDB(name: String): PostgresSpencerDB = {
    if (! this.dbMap.contains(name)) {
      println(s"creating DB for $name")
      val db: PostgresSpencerDB= new PostgresSpencerDB(name)
      println(s"connecting to DB for $name")
      db.connect()
      println(s"connected to DB for $name")
      this.dbMap = this.dbMap + (name -> db)
      //this.lifecycle.addStopHook { () => Future.successful(db.shutdown()) }
    }
    this.dbMap(name)
  }

  def index = cached("/index") {
    Action { implicit request =>
      implicit val db = getDB("test")
      Ok(views.html.index())
    }
  }

  def doc = cached("/doc") {
    Action { implicit request =>
      Ok(views.html.doc_index())
    }
  }

  def doc_api = cached("/doc/api") {
    Action { implicit request =>
      Ok(views.html.doc_api())
    }
  }

  def playground = Action { implicit request =>
    Ok(views.html.playground())
  }

  def unslash(path: String) = Action { implicit request =>
    MovedPermanently("/"+path)
  }
}
