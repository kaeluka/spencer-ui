package controllers

import play.api.libs.json.Json
import javax.inject._

import com.github.kaeluka.spencer.analysis.SpencerGraphImplicits._
import com.github.kaeluka.spencer.analysis._
import com.github.kaeluka.spencer._
import org.apache.spark.graphx.VertexId
import play.api.cache.Cached
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json.toJson
import play.api.mvc._

import scala.concurrent.duration._

@Singleton
class BenchmarksController @Inject()(lifecycle: ApplicationLifecycle,
                                messagesApi : MessagesApi,
                                mainC: MainController,
                                cached: Cached) extends Controller {

  def json_benchmarks() = {
    cached(
      {_: RequestHeader => s"benchmarks"},
      2.hours.toSeconds.asInstanceOf[Int])
    {
      Action { implicit req =>
        val benchmarks = PostgresSpencerDBs.getAvailableBenchmarks()
        Ok(toJson(benchmarks.map(_.name)))
      }
    }
  }

  def benchmarks() = {
    cached(
      {_: RequestHeader => s"benchmarks"},
      2.hours.toSeconds.asInstanceOf[Int])
    {
      Action { implicit req =>
        val benchmarks = PostgresSpencerDBs.getAvailableBenchmarks()
        Ok(views.html.benchmarks(benchmarks))
      }
    }
  }

  def benchmark(dbname: String) = {
    {
      Action { implicit req =>
        NotAcceptable("Not implemented yet")
      }
    }
  }
}

