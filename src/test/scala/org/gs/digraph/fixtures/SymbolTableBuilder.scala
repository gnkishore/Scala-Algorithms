/**
 * @see http://algs4.cs.princeton.edu/42directed/SymbolDigraph.java.html
 */
package org.gs.digraph.fixtures

import scala.io.Source
import scala.collection.immutable.TreeMap
import org.gs.digraph.Digraph
import scala.io.BufferedSource
import scala.collection.mutable.ArrayBuffer

/**
 * @author Gary Struthers
 *
 */
trait ManagedResource[T] {
  def loan[U](f: T => U): U
}

trait BufferedSourceBuilder {
  def readURI(uri: String) = new ManagedResource[BufferedSource] {
    /**
     * BufferedSource can only be iterated once
     * @param f
     * @return
     */
    def loan[U](f: BufferedSource => U): U = {
      val bufferdSource = Source.fromURL(uri)
      try {
        f(bufferdSource)
      } finally {
        bufferdSource.close
      }
    }
  }
}
trait SymbolTableBuilder extends BufferedSourceBuilder {

  def buildStringIndex(delimiter: String, savedLines: ArrayBuffer[String]): TreeMap[String, Int] = {
    var st = new TreeMap[String, Int]()
    for {
      a <- savedLines
      s <- a.split(delimiter)
      if (!st.contains(s))
    } {
      val kv = (s, st.size)
      st = st + kv
    }
    st
  }
  
  def invertIndexKeys(st: TreeMap[String, Int]) = {
    val keys = new Array[String](st.size)
    for (name <- st.keys) {
      val keyOpt = st.get(name)
      keyOpt match {
        case Some(x) => keys(x) = name
        case None =>
      }
    }
    keys
  }
}

class SymbolDigraph(st: TreeMap[String, Int], val keys: Array[String], val g: Digraph) {
  def contains(s: String) = st.contains(s)
  def index(s: String) = st.get(s)
  def name(v: Int) = keys(v)
}

trait SymbolDigraphBuilder extends SymbolTableBuilder {
  def buildFromManagedResource(uri: String): SymbolDigraph = {
    val managedResource = readURI(uri)
    val delimiter = "\\s+"
    def buildDigraph(buffSource: BufferedSource): SymbolDigraph = {
      val savedLines = new ArrayBuffer[String]()
      val it = buffSource.getLines
      for (a <- it) savedLines.append(a)
      val st = buildStringIndex(delimiter, savedLines)
      val keys = invertIndexKeys(st)
      val g = new Digraph(st.size)
      for {
        a <- savedLines
      } {
        val s:Array[String] = a.split(delimiter)
        val v = st.get(s(0))
        v match {
          case Some(x) => for {
            i <- 1 until s.size     
            w <- st.get(s(i))
          	} {
          		g.addEdge(x, w)
          }
          case None => 
        }

      }
      new SymbolDigraph(st, keys, g)
    }
    managedResource.loan(buildDigraph)
  }
}