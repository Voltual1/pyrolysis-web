package org.duangsuse.bin.pat

import org.duangsuse.bin.Reader
import org.duangsuse.bin.Writer
import org.duangsuse.bin.type.Cnt
import org.duangsuse.bin.OptionalSized
import org.duangsuse.bin.Sized as ExactlySized

interface Pattern<T> {
  fun read(s: Reader): T
  fun write(s: Writer, x: T)
  fun writeSize(x: T): Cnt

  interface Sized<T>: Pattern<T>, OptionalSized
  interface StaticallySized<T>: Sized<T>, ExactlySized {
    override fun writeSize(x: T): Cnt = size
  }

  abstract class BySized<T>(self: Pattern<*>): Sized<T> {
    override val size: Cnt? = (self as? Sized<*>)?.size
  }
  abstract class BySizedFully<T>(private val self: Pattern<T>): BySized<T>(self) {
    final override val size: Cnt? = super.size
    final override fun writeSize(x: T): Cnt = self.writeSize(x)
  }
}