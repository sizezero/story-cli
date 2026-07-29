package org.kleemann.storycli.meta

case class Premise(oneLine: String)

object Premise {

    val filename = "premise.md"

    def extract(repo: os.Path): Either[String, Premise] =
        pipe(extractFile(repo,filename), list.Premise.create)

    def read(dir: os.Path): Either[String, Premise] =
        pipe(readFile(dir / filename), list.Premise.create)
}
