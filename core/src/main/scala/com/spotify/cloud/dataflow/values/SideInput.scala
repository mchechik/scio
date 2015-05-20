package com.spotify.cloud.dataflow.values

import java.lang.{Iterable => JIterable}
import java.util.{Map => JMap}

import com.google.cloud.dataflow.sdk.transforms.DoFn
import com.google.cloud.dataflow.sdk.values.PCollectionView

import scala.collection.JavaConverters._

trait SideInput[T] extends Serializable {
  def get[I, O](context: DoFn[I, O]#ProcessContext): T
  val view: PCollectionView[_]
}

private[values] class SingletonSideInput[T](val view: PCollectionView[T]) extends SideInput[T] {
  override def get[I, O](context: DoFn[I, O]#ProcessContext): T = context.sideInput(view)
}

private[values] class IterableSideInput[T](val view: PCollectionView[JIterable[T]]) extends SideInput[Iterable[T]] {
  override def get[I, O](context: DoFn[I, O]#ProcessContext): Iterable[T] = context.sideInput(view).asScala
}

private[values] class MapSideInput[K, V](val view: PCollectionView[JMap[K, JIterable[V]]])
  extends SideInput[Map[K, Iterable[V]]] {
  override def get[I, O](context: DoFn[I, O]#ProcessContext): Map[K, Iterable[V]] = {
    // TODO: reduce copies
    context.sideInput(view).asScala.mapValues(_.asScala.toList.asInstanceOf[Iterable[V]]).toMap
  }
}

class SideInputContext[T] private[dataflow] (private val context: DoFn[T, AnyRef]#ProcessContext) extends AnyVal {
  def apply[S](side: SideInput[S]): S = side.get(context)
}
