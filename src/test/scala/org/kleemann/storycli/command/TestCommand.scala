package org.kleemann.storycli.command

import org.kleemann.storycli.GlobalOptions

class TestCommand extends munit.FunSuite {
    test("scratch") {
        assertEquals(1, 1)
    }

    test("empty") {
        assertEquals( 
            Command.parse(Nil), 
            Left("no command specified") )
    }

    test("default option is development") {
        // all of our tests run in development mode
        assertEquals( 
            Command.parse(List("help")), 
            Right(GlobalOptions(false, "help"), Nil) )
    }

    test("explicit development") {
        assertEquals( 
            Command.parse(List("--development", "help")), 
            Right(GlobalOptions(false, "help"), Nil) )
    }

    test("production and development conflict") {
        Command.parse(List("--development", "--production", "help")) match {
            case Left(_) => assert(true) // don't care what the error message is
            case _       => assert(false)
        }

        Command.parse(List("--production", "--development", "help")) match {
            case Left(_) => assert(true) // don't care what the error message is
            case _       => assert(false)
        }
    }

    test("all versions of help") {
        val expected = Right(GlobalOptions(false, "help"), Nil)
        assertEquals( Command.parse(List("-h")),     expected )
        assertEquals( Command.parse(List("--help")), expected )
        assertEquals( Command.parse(List("help")),   expected )
    }

    test("--production can't be specified from a non-production location") {
        Command.parse(List("--production", "help")) match {
            case Left(_) => assert(true) // don't care what the error message is
            case _       => assert(false)
        }
    }

    test("unrecognized argument") {
        assertEquals( 
            Command.parse(List("***")), 
            Left("unrecognized argument: ***") )
    }
}
