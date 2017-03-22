package controllers

import javax.inject._

import com.github.kaeluka.spencer._
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
      {_: RequestHeader => s"json_benchmarks"},
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
        Ok(views.html.datasets(benchmarks))
      }
    }
  }

  def benchmark(dbname: String) = {
    {
      Action { implicit req =>
        val benchmarks = PostgresSpencerDBs.getAvailableBenchmarks().filter(_.name == dbname)
        benchmarks.length match {
          case 0 => NotFound(s"benchmark $dbname does not exist.")
          case 1 => Ok(views.html.dataset(benchmarks.head))
          case n => InternalServerError(s"BUG: found $n benchmarks with name $dbname")
        }
      }
    }
  }
}

