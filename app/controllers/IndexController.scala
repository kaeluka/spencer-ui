package controllers

import java.util
import javax.inject._

import com.github.kaeluka.spencer.analysis.SpencerGraphImplicits._
import com.github.kaeluka.spencer.analysis._
import com.github.kaeluka.spencer.tracefiles.SpencerDB
import org.apache.spark.graphx.VertexId
import org.apache.spark.rdd.RDD
import play.api.data.Forms._
import play.api.data.{Form, _}
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import play.api.mvc._

import scala.concurrent.Future


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class IndexController @Inject()(lifecycle: ApplicationLifecycle, messagesApi : MessagesApi) extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index()(messagesApi.preferred(request)))
  }
}
