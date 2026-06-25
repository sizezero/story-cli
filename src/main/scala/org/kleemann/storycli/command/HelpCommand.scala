package org.kleemann.storycli.command

import org.kleemann.storycli.GlobalOptions

object HelpCommand extends Command {

    def run(go: GlobalOptions, rest: List[String]): Either[String, List[String]] = {
        Right(List("TODO: some help"))
    }
}
