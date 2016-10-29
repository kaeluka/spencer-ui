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

@Singleton
class SourceCodeController @Inject()(mainC: MainController) extends Controller {

  def query(dbname: String, klass: String) = Action { implicit req =>
    implicit val data: SpencerData = mainC.getDB(dbname)

    val query = SourceCode(klass)

    val result = query.analyse

    Ok(views.html.sourceCode(dbname, klass, result))
  }
}
