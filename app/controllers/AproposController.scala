package controllers

import java.sql.ResultSet
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
    println(s"apropos/$dbname/$idStr")
    implicit val data = mainC.getDB(dbname)

    val apropos = Apropos(idStr.toLong).analyse

    Ok(views.html.apropos(
      dbname,
      apropos.klass,
      idStr.toLong,
      apropos,
      apropos.klass.flatMap(SourceCode(_).analyse)))
  }
}
