package org.kleemann.storycli.command

import os.home

import org.kleemann.storycli.GlobalOptions

trait Command {
    def run(go: GlobalOptions): Either[String, List[String]]
}

object Command {

    protected[command] def parse(args: List[String]): Either[String, GlobalOptions] = {

        val commandRe = """([a-z]+)""".r

        // parse options until we get something that doesn't start with a hyphen
        def loop(args: List[String], help: Boolean, production: Boolean, development: Boolean, command: Option[String]): 
            Either[String, (Boolean, Boolean, Boolean, Option[String], List[String])] = {

            if (args.isEmpty) Right(help, production, development, command, args)
            else {
                val arg = args.head
                arg match {
                    // if help is found just break out of this with a help command
                    case "-h" | "--help" => Right(true, production, development, Some("help"), Nil)
                    case "--production"  =>
                        if (production) Left("--production specified twice")
                        else if (development) Left("--development and production cannot both be specified")
                        else loop(args.tail, help, true, development, command)
                    case "--development"  =>
                        if (development) Left("--development specified twice")
                        else if (production) Left("--development and production cannot both be specified")
                        else loop(args.tail, help, production, true, command)
                    // stop processing args when command is reached
                    case commandRe(cmd)  => Right((help, production, development, Some(cmd), args.tail))
                    case arg => Left(s"unrecognized argument: $arg")
                }
            }
        }
        loop(args, false, false, false, None) match {
            case Left(error) => Left(error)
            case Right(help, production, development, command, rest) => {
                command match {
                    case None => Left("no command specified")
                    case Some(command) => {
                        // see if we are under the bin directory
                        // vs code gives
                        // /home/robert/stories/development/story-cli/.bloop/root/bloop-bsp-clients-classes/classes-Metals-2MpJCK-NTSaOBi_5mvIdVQ==/
                        // sbt run gives
                        // /home/robert/stories/development/story-cli/target/bg-jobs/sbt_72a7bb2e/job-1/target/988d37f6/3e32f1de/story-cli_3-0.1.0-SNAPSHOT.jar
                        val jarPath: String = classOf[org.kleemann.storycli.command.Command].getProtectionDomain.getCodeSource.getLocation.toURI.getPath
                        val binPath: String = (os.home / "bin").toString
                        val isProductionLocation: Boolean = jarPath.startsWith(binPath)
                        if (isProductionLocation) {
                            if (production)       Right(GlobalOptions(args, true, command, rest))
                            else if (development) Right(GlobalOptions(args, false, command, rest))
                            // production is the default when running from production location
                            else                  Right(GlobalOptions(args, true,  command, rest))
                        } else {
                            if (production)       Left("Cannot specify --production when running from a non-production location")
                            else if (development) Right(GlobalOptions(args, false, command, rest))
                            // development is the default when running from production location
                            else                  Right(GlobalOptions(args, false,  command, rest))
                        }
                    }
                }
            }
        }
    }

    private val all: Map[String, Command] = Map[String, Command](
        "help"     -> HelpCommand,
        "new"      -> NewCommand,
        "list"     -> ListCommand,
        "checkout" -> CheckoutCommand
    )

    /**
     * Parses the command line and runs the command. Commands have side-effects so this method does as well.
     *
     * @param args
     * @return Either an error String or a list of output Strings
     */
    def run(args: List[String]): Either[String, List[String]] = {
        parse(args) match {
            case Left(error) => Left(error)
            case Right(go) => {
                all.get(go.command) match {
                    case None          => Left(s"command not found: ${go.command}")
                    case Some(command) => command.run(go)
                }
            }
        }
    }
}