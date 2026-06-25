
import org.kleemann.storycli.command.Command

@main def main(args: String*): Unit =
  Command.run(args.toList) match {
    case Left(error) => {
      println("error: "+error)
      sys.exit(1)
    }
    case Right(output) => output.foreach(println(_))
  }
