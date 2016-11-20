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
  def oidWithHint(dbname: String, oid: Long): String = {
    "<span class='oid'>"+oid+"</span>"
  }
}

@Singleton
class AproposController @Inject()(lifecycle: ApplicationLifecycle, messagesApi : MessagesApi, mainC: MainController) extends Controller {

  def apropos(dbname: String, idStr: String) = Action { implicit req =>
    implicit val data: SpencerData = mainC.getDB(dbname)

    val apropos = Apropos(idStr.toLong).analyse

    Ok(views.html.apropos(
      dbname,
      apropos.klass,
      idStr.toLong,
      apropos,
      apropos.klass.flatMap(SourceCode(_).analyse)))
  }
}
