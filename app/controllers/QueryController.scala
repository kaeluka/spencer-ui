package controllers

import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.Calendar

import play.api.libs.json.Json
import javax.inject._

import com.github.kaeluka.spencer.analysis.SpencerGraphImplicits._
import com.github.kaeluka.spencer.analysis._
import org.apache.spark.graphx.VertexId
import org.apache.spark.sql.DataFrame
import play.api.cache.Cached
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json.toJson
import play.api.mvc._

import scala.concurrent.duration._

case class QueryDataItem(oid: VertexId, klass: Option[String], allocationSite: Option[String])
case class QueryData(dbname: String, query: String, explanation: String, data: Seq[QueryDataItem])

@Singleton
class QueryController @Inject()(lifecycle: ApplicationLifecycle,
                                messagesApi : MessagesApi,
                                mainC: MainController,
                                cached: Cached) extends Controller {

  def json_meta(dbname: String, q: String = "Obj()") = {
    cached(
      { _: RequestHeader => s"json_meta/$q" },
      2.hours.toSeconds.asInstanceOf[Int]) {
      Action { implicit req =>
        implicit val data = mainC.getDB(dbname)
        val cacheDuration = 3600
        QueryParser.parseObjQuery(q) match {
          case Right(qObj) =>
            val metaQuery: WithMetaInformation = WithMetaInformation(qObj)
            val objs: ResultSet = metaQuery.analyseJDBC
//            val N = objs.count().asInstanceOf[Int]
            val oid = scala.collection.mutable.ArrayBuffer[Long]()
            val allocationSite = scala.collection.mutable.ArrayBuffer[String]()
            val klass = scala.collection.mutable.ArrayBuffer[String]()
            val firstusage = scala.collection.mutable.ArrayBuffer[Long]()
            val lastusage = scala.collection.mutable.ArrayBuffer[Long]()
            val thread = scala.collection.mutable.ArrayBuffer[String]()
            //            val numFieldWrites = new Array[Long](N)
            //            val numFieldReads = new Array[Long](N)
            val numCalls = scala.collection.mutable.ArrayBuffer[Long]()
            while (objs.next) {
              oid += objs.getLong("id")
              val file = Option(objs.getString("allocationsitefile"))
              val line = objs.getLong("allocationsiteline") match {
                case 0 => None
                case n => Some(n)
              }
              allocationSite += file.getOrElse("undefined")+":"+line.getOrElse("undefined")
              klass          += Option(objs.getString("klass")).getOrElse("undefined")
              firstusage     += objs.getLong("firstusage")
              lastusage      += objs.getLong("lastusage")
              thread         += objs.getString("thread")
              //              numFieldWrites(i) = 0 // objs(i).numFieldWrites
              //              numFieldReads(i) =  0
              numCalls       += objs.getLong("numCalls")
            }
            objs.close()
            Ok(Json.obj(
              "query" -> q,
              "dbname" -> dbname,
              "variables" -> toJson(metaQuery.availableVariables),
              "data" -> Json.obj(
                "oid" -> toJson(oid),
                "allocationSite" -> toJson(allocationSite),
                "klass" -> toJson(klass),
                "firstusage" -> toJson(firstusage),
                "lastusage" -> toJson(lastusage),
                "thread" -> toJson(thread),
                //                "numFieldWrites" -> toJson(numFieldWrites),
                //                "numFieldReads" -> toJson(numFieldReads),
                "numCalls" -> toJson(numCalls)
              )
            ))
          case Left(_) =>
            NotAcceptable("could not parse the query '" + q)
        }
      }
    }
  }

  def json_select(dbname: String, q: String) =
  //    cached(
  //      {_: RequestHeader => s"json/$q"},
  //      6.hours.toSeconds.asInstanceOf[Int])
  {
    val cacheDuration = 3600
    Action { implicit req =>
      implicit val data = mainC.getDB(dbname)
      import data.sqlContext.implicits._
      QueryParser.parseObjQuery(q) match {
        case Right(qObj) =>
          println(
            s"""=========================================================
                |JSON_SELECT:    $dbname/$q
                |TIME:           ${new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance.getTime)}
                |REMOTE ADDRESS: ${req.remoteAddress}
                |DEPENDENCIES:
                |${qObj.dependencyTree()}
                |=========================================================""".stripMargin)
          val objsRS = qObj.analyseJDBC : ResultSet
          var objs = scala.collection.mutable.ArrayBuffer[Long]()
          while (objsRS.next()) {
            objs += objsRS.getLong("id")
          }
          objsRS.close()

          Ok(Json.obj(
            "query" -> q,
            "objects" -> objs.result()
          )).withHeaders((s"Cache-Control", s"public, max-age=$cacheDuration"))
        case Left(_) =>
          NotAcceptable("could not parse the query '" + q)
      }
    }
  }

  def json_percentage(dbname: String, q: String) = {
    Action { implicit req =>
      implicit val data = mainC.getDB(dbname)
      val result = data.getObjPercentage(q)
      Ok(Json.obj(
        "query" -> q,
        "data" -> result
      ))
    }
  }

  def json_field_percentage(dbname: String, q: String) =
    json_field_percentage_min(dbname, q, 10)

  def json_field_percentage_min(dbname: String, q: String, min: Int) =
  {
    Action { implicit req =>
      implicit val data = mainC.getDB(dbname)
      val result: Option[Seq[(String, Float)]] = data.getFieldPercentage(q, min)
      val ret = result match {
        case Some(res) => {
          val fields = res.map(_._1)
          val percentages = res.map(_._2)
          Ok(Json.obj(
            "query"       -> q,
            "fields"      -> fields,
            "percentages" -> percentages
          ).toString)
        }
        case None      => NotAcceptable("error")
      }
      ret

    }
  }
  def json_class_percentage(dbname: String, q: String) =
    json_class_percentage_min(dbname, q, 10)

  def json_class_percentage_min(dbname: String, q: String, min: Int) =
  {
    Action { implicit req =>
      implicit val data = mainC.getDB(dbname)
      val result: Option[Seq[(String, Float)]] = data.getClassPercentage(q, min)
      val ret = result match {
        case Some(res) => {
          val classes = res.map(_._1)
          val percentages = res.map(_._2)
          Ok(Json.obj(
            "query" -> q,
            "classes" -> classes,
            "percentages" -> percentages
          ).toString)
        }
        case None      => NotAcceptable("error")
      }
      ret

    }
  }

  def query(dbname: String, qs_ : String) =
//    cached(
//      {_: RequestHeader => s"query/$qs"},
//      6.hours.toSeconds.asInstanceOf[Int])
    {
      Action { implicit req =>
        val qs = QueryParser.unescape(qs_)
        val eitherQueries = qs.split("/").map(QueryParser.parseObjQuery(_))

        if (eitherQueries.exists(_.isLeft)) {
          NotAcceptable("could not parse the query '" + qs + "':\n" + eitherQueries.filter(_.isLeft).mkString(", "))
        } else {
          val queries = eitherQueries.map(_.right.get)
          if (qs != QueryParser.unescape(queries.mkString("/"))) {
            println("redirecting..")
            Redirect(routes.QueryController.query(dbname, queries.mkString("/")))
          } else {
            Ok(views.html.query(dbname, queries))
          }
        }
      }
    }
}

