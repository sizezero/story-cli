package org.kleemann.storycli.command

import os.home

import org.kleemann.storycli.GlobalOptions

trait Command {

    /**
      * The name of the command as specified on the command line.
      * It just contains letters, number, and hypenns.
      */
    val commandName: String

    /**
      * A one line syntax of the command.
      * TODO: test this and the above value
      */
    val commandLineHelp: String

    /**
     * A one line phrase describing the command's function. It is not a sentence so does not start
     * with caps and does not end in a period.
     */
    val oneLineHelp: String

    /**
      * More details on help.
      */
    val multiLineHelp: List[String]

    def commandSpecificHelp(): List[String] = {
        "" ::
        "command " + commandName + " : " + oneLineHelp ::
        "" ::
        commandLineHelp ::
        "" ::
        ( multiLineHelp ++ List("") ) // unfortunately need an extra newline
    }

    def run(go: GlobalOptions): Either[String, List[String]]
}

object Command {

    protected[command] val commandNameRe = """([a-z]+)""".r

    private def longHelp(): List[String] = List(

    )

    protected[command] def parse(args: List[String]): Either[String, GlobalOptions] = {

        // parse options until we get something that doesn't start with a hyphen
        def loop(args: List[String], production: Boolean, development: Boolean, command: Option[String]):
            Either[String, (Boolean, Boolean, Option[String], List[String])] = {

            if (args.isEmpty) Right(production, development, command, args)
            else {
                val arg = args.head
                arg match {
                    // if help is found just break out of this with a help command
                    case "-h" | "--help" => Right(production, development, Some("help"), Nil)
                    case "--production"  =>
                        if (production) Left("--production specified twice")
                        else if (development) Left("--development and production cannot both be specified")
                        else loop(args.tail, true, development, command)
                    case "--development"  =>
                        if (development) Left("--development specified twice")
                        else if (production) Left("--development and production cannot both be specified")
                        else loop(args.tail, production, true, command)
                    // stop processing args when command is reached
                    case commandNameRe(cmd)  => Right((production, development, Some(cmd), args.tail))
                    case arg => Left(s"unrecognized argument: $arg")
                }
            }
        }
        loop(args, false, false, None) match {
            case Left(error) => Left(error)
            case Right(production, development, command, rest) => {
                command match {
                    case None => Left(HelpCommand.noCommand())
                    case Some("help") => Right(GlobalOptions(args, false, "help", rest))
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

    /**
     * All commands. Order informs how commands are displayed in help.
     * Simple, common commands should come first
     */
    val all = List[Command](
        ListCommand,
        NewCommand,
        CloneCommand,
        SummaryCommand,
        AnalyzeCommand,
        BackupCommand,
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
                if (go.command == "help")
                    HelpCommand.run(go)
                else all.find{ _.commandName == go.command } match {
                    case None          => Left(s"command not found: ${go.command}")
                    case Some(command) => {
                        if (go.rest.contains("-h") || go.rest.contains("--help"))
                            Right(command.commandSpecificHelp())
                        else
                            command.run(go)
                    }
                }
            }
        }
    }
}