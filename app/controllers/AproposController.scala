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
    "<span class='oid'>"+oid+"</span> <a class='hint' href='"+routes.AproposController.apropos(dbname, oid.toString)+"'>apropos</a>"
  }
}
/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class AproposController @Inject()(lifecycle: ApplicationLifecycle, messagesApi : MessagesApi, mainC: MainController) extends Controller {

  def apropos(dbname: String, idStr: String) = Action { implicit req =>
    implicit val data: SpencerData = mainC.getDB(dbname)

    val query = Apropos(idStr.toLong)

    Ok(views.html.apropos(dbname, idStr.toLong, query.analyse))
  }
}
