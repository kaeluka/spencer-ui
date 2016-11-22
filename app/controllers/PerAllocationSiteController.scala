package controllers

import javax.inject._

import com.github.kaeluka.spencer.analysis.SpencerGraphImplicits._
import com.github.kaeluka.spencer.analysis._
import org.apache.spark.graphx.VertexId
import org.apache.spark.rdd.RDD
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import play.api.mvc._

object PerAllocationSiteControllerUtil {
  def successCountAndFailCountTexts(dbname: String, queries: Seq[String], allocationSite: String, succ: Int, fail: Int) : (String, String) = {
    (if (succ > 0) {
      val newQueries = queries
        .map("And("+_+" AllocatedAt("+allocationSite+"))").mkString("/")
      s"$succ <a href='${routes.QueryController.perobj(dbname, newQueries)}' class='hint'>show</a>"
    } else {
      succ.toString
    },
      if (fail > 0) {
        val newQueries = queries
          .map("And(Not("+_+") AllocatedAt("+allocationSite+"))").mkString("/")
        s"$fail <a href='${routes.QueryController.perobj(dbname, newQueries)}' class='hint'>show</a>"
      } else {
        fail.toString
      })
  }
}

case class PerAllocationSiteDataItem(allocationsite: Option[String],succ: Int, total: Int)
case class PerAllocationSiteData(query: String, data: Seq[PerAllocationSiteDataItem])

@Singleton
class PerAllocationSiteController @Inject()(lifecycle: ApplicationLifecycle, messagesApi: MessagesApi, mainC: MainController) extends Controller {

  def query(dbname: String, qs: String) = Action { implicit req =>
    implicit val data: SpencerData = mainC.getDB(dbname)

    val queries = qs.split("/").map(QueryParser.parseObjQuery(_))
    println("================================ " + qs + " -parsed-> " + queries.mkString(" / "))
    if (queries.exists(_.isLeft)) {
      NotAcceptable("could not parse the query '" + qs + "':\n" + queries.filter(_.isLeft).mkString(", "))
    } else {
      val results = queries.map({
        case Right(query) =>
          PerAllocationSiteData(query.toString, ProportionPerAllocationSite(query)
            .analyse
            .collect
            .toSeq
            .sortBy({ case (_, (x, n)) => n / x.toFloat })
            .map({case ((oFile, oLine), (x, y)) =>
              PerAllocationSiteDataItem(oFile.flatMap(file => oLine.map(line => file+":"+line)), x, y)
            })
          )})
      Ok(views.html.perallocationsite(dbname, qs, results))

    }
  }
}
