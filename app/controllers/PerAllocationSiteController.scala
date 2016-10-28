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
  def successCountAndFailCountTexts(dbname: String, query: String, allocationSite: String, succ: Int, fail: Int) : (String, String) = {
    (if (succ > 0) {
      s"$succ <a href='${routes.QueryController.query(dbname, s"And($query AllocatedAt($allocationSite))")}' class='hint'>todo: show</a>"
    } else {
      succ.toString
    },
      if (fail > 0) {
        s"$fail <a href='${routes.QueryController.query(dbname, s"And(Not($query) AllocatedAt($allocationSite))")}' class='hint'>show</a>"
      } else {
        fail.toString
      })
  }
}

@Singleton
class PerAllocationSiteController @Inject()(lifecycle: ApplicationLifecycle, messagesApi : MessagesApi, mainC: MainController) extends Controller {

  def query(dbname: String, q: String) = Action { implicit req =>
    implicit val data: SpencerData = mainC.getDB(dbname)

    val query: Either[String, SpencerAnalyser[RDD[VertexId]]] = QueryParser.parseObjQuery(q)
    println("================================ "+q+" -parsed-> "+query.toString)

    query match {
      case Right(qObj) =>
        val objects = ProportionPerAllocationSite(qObj).analyse
          .collect()
          .toSeq
          .sortBy({case (_, (x, n)) => n/x.toFloat})
          .map({case ((oFile, oLine), xy) =>
            (oFile.flatMap(file => oLine.map(line => file+":"+line)), xy)
          })
        Ok(views.html.perallocationsite(dbname = dbname, query = q, data = objects))

      case Left(msg) =>
        NotAcceptable("could not parse the query '" + q + "':\n"+msg)
    }
  }
}
