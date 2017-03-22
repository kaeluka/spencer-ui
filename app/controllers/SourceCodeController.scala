package controllers

import javax.inject._

import com.github.kaeluka.spencer.analysis._
import play.api.mvc._

@Singleton
class SourceCodeController @Inject()(mainC: MainController) extends Controller {

  def query(dbname: String, klass: String) = Action { implicit req =>
    implicit val data = mainC.getDB(dbname)
    Ok(views.html.sourceCode(dbname, klass, SourceCode(klass).analyse))
  }
}
