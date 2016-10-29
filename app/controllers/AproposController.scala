package controllers

import java.util
import javax.inject._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import com.github.kaeluka.spencer.analysis._
import com.github.kaeluka.spencer.tracefiles.SpencerDB
import com.github.kaeluka.spencer.analysis.SpencerGraphImplicits._
import org.apache.spark.graphx.VertexId
import org.apache.spark.rdd.RDD
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

object AproposControllerUtil {
  def oidWithHint(dbname: String, oklass: Option[String], oid: Long): String = {
    oklass match {
      case Some(klass) =>
        "<span class='oid'>"+oid+"</span> <a class='hint' href='"+routes.AproposController.apropos_class(dbname, klass, oid.toString)+"'>apropos</a>"
      case None =>
        "<span class='oid'>"+oid+"</span> <a class='hint' href='"+routes.AproposController.apropos_noclass(dbname, oid.toString)+"'>apropos</a>"
    }
  }
}
@Singleton
class AproposController @Inject()(lifecycle: ApplicationLifecycle, messagesApi : MessagesApi, mainC: MainController) extends Controller {

  def apropos_noclass(dbname: String, idStr: String) =
    apropos_aux(dbname, None, idStr)

  def apropos_class(dbname: String, klass: String, idStr: String) =
    apropos_aux(dbname, Some(klass), idStr)

  def apropos_aux(dbname: String, klass: Option[String], idStr: String) = Action { implicit req =>
    implicit val data: SpencerData = mainC.getDB(dbname)

    Ok(views.html.apropos(
      dbname,
      klass,
      idStr.toLong,
      Apropos(idStr.toLong).analyse,
      klass.flatMap(SourceCode(_).analyse)))
  }
}
