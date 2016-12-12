package controllers

import javax.inject._

import com.github.kaeluka.spencer.analysis.SpencerGraphImplicits._
import com.github.kaeluka.spencer.analysis._
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import play.api.mvc._

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

    val indegreeHistory = InDegreeMap(InDegreeSpec.HEAP).analyse.filter(_._1 == idStr.toLong)

    if (indegreeHistory.count > 0) {
      println(indegreeHistory.first._2.mkString("indegree history:\n\t- ", ",\n\t- ", ""))
    }

    Ok(views.html.apropos(
      dbname,
      apropos.klass,
      idStr.toLong,
      apropos,
      apropos.klass.flatMap(SourceCode(_).analyse)))
  }
}
