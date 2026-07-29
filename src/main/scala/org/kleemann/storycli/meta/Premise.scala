package org.kleemann.storycli.meta

case class Premise(oneLine: String)

object Premise {

    val filename = "premise.md"

    def extract(repo: os.Path): Either[String, Premise] =
        pipe(extractFile(repo,filename), org.kleemann.storycli.meta.list.Premise.create)

    def read(dir: os.Path): Either[String, Premise] =
        pipe(readFile(dir / filename), org.kleemann.storycli.meta.list.Premise.create)
}
