package controllers

import scala.concurrent.duration._

import javax.inject._

import com.github.kaeluka.spencer.analysis.SpencerGraphImplicits._
import com.github.kaeluka.spencer.analysis._
import org.apache.spark.graphx.VertexId
import org.apache.spark.rdd.RDD
import play.api.cache.Cached
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import play.api.mvc._

case class PerClassDataItem(klass: Option[String], succ: Int, total: Int)
case class PerClassData(dbname: String, query: String, data: Seq[PerClassDataItem])

object PerClassControllerUtil {
  def successCountAndFailCountTexts(dbname: String, query: String, klass: String, succ: Int, fail: Int) : (String, String) = {
    (if (succ > 0) {
      s"$succ <a href='${routes.QueryController.perobj(dbname, s"And($query InstanceOfClass($klass))")}' class='hint'>show</a>"
    } else {
      succ.toString
    },
      if (fail > 0) {
        s"$fail <a href='${routes.QueryController.perobj(dbname, s"And(Not($query) InstanceOfClass($klass))")}' class='hint'>show</a>"
      } else {
        fail.toString
      })
  }
}

@Singleton
class PerClassController @Inject()(lifecycle: ApplicationLifecycle,
                                   messagesApi : MessagesApi,
                                   mainC: MainController,
                                   cached: Cached) extends Controller {

  def query(dbname: String, q: String) = cached(
    {_: RequestHeader => s"perclass/$q"},
    6.hours.toSeconds.asInstanceOf[Int]) {
    Action { implicit req =>
      implicit val data: SpencerData = mainC.getDB(dbname)
      val qs: Array[String] = q.split("/")

      val queries = qs.map(QueryParser.parseObjQuery(_))
      if (queries.exists(_.isLeft)) {
        NotAcceptable("could not parse the query '" + q + "':\n" + queries.filter(_.isLeft).mkString(", "))
      } else {
        val results = queries.map { case Right(query) => (query.toString, ProportionPerClass(query)
          .analyse.collect
          .map { case (oKlass, (succ, total)) => PerClassDataItem(oKlass, succ, total) }
          .toSeq
          .sortBy(item => item.total.toFloat / item.succ)
          )
        }.toSeq
        Ok(views.html.perclass(dbname, query = q, results.map({ case (q, result) => PerClassData(dbname, q, result) })))
      }
    }

//    query match {
//      case Right(qObj) =>
//        val result = ProportionPerClass(qObj).analyse
//          .collect()
//          .map({case (oKlass, (succ, total)) => PerClassDataItem(oKlass, succ, total)})
//          .toSeq
//          .sortBy(item => item.total.toFloat/item.succ)
//        Ok(views.html.perclass(dbname = dbname, query = q, List(PerClassData(dbname, q, result), PerClassData(dbname, q, result))))
//
//      case Left(msg) =>
//        NotAcceptable()
//    }
  }
}
