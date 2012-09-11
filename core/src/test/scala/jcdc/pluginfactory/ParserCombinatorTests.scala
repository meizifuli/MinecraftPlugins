package jcdc.pluginfactory

import org.scalacheck.Properties
import org.scalacheck.Prop._

object ParserCombinatorTests extends Properties("ParserCombinatorTests") with ParserCombinators {

  property("strings") = forAll { (s:String) => stringToParser(s)(List(s)).get ?= s }

  property("ints") = forAll { (i:Int) => num(List(i.toString)).get ?= i }

  property("any") = forAll { (i: Int,  s: String, l:List[String]) =>
    success((i, s))(l) ?= Success((i, s), l)
  }

  property("odd ok") = forAll { (i:Int) => (i % 2 == 1) ==>
    (oddNum(List(i.toString)).get ?= i)
  }

  property("even ok") = forAll { (i:Int) => (i % 2 == 0) ==>
    (evenNum(List(i.toString)).get ?= i)
  }

  property("odd bad") = forAll { (i:Int) => (i % 2 == 0) ==>
    (oddNum(List(i.toString)) ?= Failure(s"invalid odd-number: $i"))
  }

  property("even bad") = forAll { (i:Int) => (i % 2 == 1) ==>
    (evenNum(List(i.toString)) ?= Failure(s"invalid even-number: $i"))
  }

  property("+ ok") = forAll { (is:List[Int]) => (is.size > 0) ==>
    (num+(is.map(_.toString)) ?= Success(is, Nil))
  }

  property("+ on nil") = { num+(Nil) ?= Failure("expected number, got nothing") }

  property("*") = forAll { (is:List[Int]) => num*(is.map(_.toString)) ?= Success(is, Nil) }
}