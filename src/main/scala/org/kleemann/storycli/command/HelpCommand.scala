package org.kleemann.storycli.command

import org.kleemann.storycli.GlobalOptions

object HelpCommand extends Command {

    override val commandName = "N/A"

    override val commandLineHelp = "N/A"

    override val oneLineHelp = "N/A"

    override val multiLineHelp = List("N/A")

    def noCommand(): String = "usage: story-cli --help"

    override def run(go: GlobalOptions): Either[String, List[String]] = {

        val longestCommand: Int = Command.all.foldRight(0){ case (cmd, len) => {
            scala.math.max(len, cmd.commandName.length())
        }}

        val text = (
            """story-cli [ (-h | --help) ] [ ( --production | --development) ] <command> [ -h | --help] [ <command option>... ]
            #
            # a helper program that performs linux command line based writing tasks
            #
            # -h, --help : brings up this help text
            #
            # --production : The program is run against production data in ~/stories/
            #                Calls to the server are run on production server data.
            #                This option is not allowed unless the jar that this program
            #                is run from is located under ~/bin/
            #                When run from ~/bin/ the --production option is the default.
            #
            # --development : The program is run against development data in ~/stories/development/
            #                 Calls to the server are run on development server data.
            #                 --development and --production options may not both be specified.
            #
            # <command> is one of
            #""" + Command.all.foldLeft(StringBuilder()){ case(sb, cmd) => {
                sb.append(s"    %${longestCommand}s : %s\n".format(cmd.commandName, cmd.oneLineHelp))
            }}.toString +
         """#
            # story-cli <command> -h
            #     for help on the specific command
            #
            #""").stripMargin('#').linesIterator.toList

        Right(text)
    }
}
