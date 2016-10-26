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

case class QueryData(dbname: String, query: String)

case class ResultObj(id: Long, klass: Option[String], allocationSite: Option[String])

case class ResultsForm(dbname: String, query: String, results: List[ResultObj], selection: List[Boolean])


object QueryControllerUtil {

  def objectQueryResultAsHtml(dbname: String, query: String, objects: Seq[ResultObj]): String = {

    def classNameToTd(optKlass: Option[String]): String = {

      val inner = optKlass match {
        case Some(klass) => {
          klass +
            " <a class='suggestion' href='" + routes.QueryController.query(dbname, "InstanceOfClass(" + klass + ")") +"'>all instances</a>" +
            " <a class='suggestion' href='" + routes.QueryController.query(dbname, "And("+query+ " InstanceOfClass(" + klass + "))") +"'>.. + all instances</a>"
        }
        case None => {
          "<span class='empty'>N/A</span>"
        }
      }

      "      <td>"+inner+"</td>\n"
    }

    //val head = "<div class=\"result-size \">"+objects.size+" objects</div>"

    val head =
      "  <thead>\n" +
        "    <tr>\n" +
        "      <th></th>"+
        "      <th>Obj. ID</th>\n" +
        "      <th>Class</th>\n" +
        "      <th>Allocation Site</th>\n" +
        "    </tr>\n" +
        "  </thead>\n"
    val table = objects.map {
      x =>
        "    <tr>\n" +
          "      <td><input name='chk-"+(x.id)+"' type='checkbox'/></td>\n" +
          "      <td class=\"object-id\">" + AproposControllerUtil.oidWithHint(dbname, x.id) + "</td>\n" +
          classNameToTd(x.klass) +
          "      <td>" +
          x.allocationSite
            .filter(str => {
              !(str.startsWith("<") || str.contains(":-1"))
            })
            .getOrElse("<span class='empty'>N/A</span>") +
          "</td>\n" +
          "    </tr>"
    }.mkString("\n")

    "<table id=\"resultTable\" class=\"tablesorter\">\n" +
      head +
      "\n  <tbody>\n" +
      table +
      "\n  </tbody>\n" +
      "</table>"
  }

}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class QueryController @Inject()(lifecycle: ApplicationLifecycle, messagesApi : MessagesApi, mainC: MainController) extends Controller {

  def queryDataPost(dbname: String) = Action { implicit request =>
    val selected = request.rawQueryString.split("&").map(_.replace("=on", "").replace("chk-", "").toLong).mkString(" ")

    Redirect(routes.QueryController.query("test", "Set("+selected+")"))
  }

  def query(dbname: String, q: String) = Action { implicit req =>
    implicit val data: SpencerData = mainC.getDB(dbname)
    //
    val query: Either[String, SpencerAnalyser[RDD[VertexId]]] = QueryParser.parseObjQuery(q)
    println("================================ "+q+" -parsed-> "+query.toString)

    query match {
      case Right(qObj) =>
        val objects = WithMetaInformation(qObj).analyse.collect().map(ResultObj.tupled).toList

        val f = ResultsForm(dbname, query = q, results = objects.toList, selection = List())
//        val resultsForm = Form(
//          Forms.mapping(
//            "selection" -> list(boolean)
//          )(ResultsForm.apply)(ResultsForm.unapply))

//        Ok(views.html.result(q, innerHtml))
        Ok(views.html.result(dbname, q, "", objects))

      case Left(msg) =>
        NotAcceptable("could not parse the query '" + q + "':\n"+msg)
    }
  }
}
