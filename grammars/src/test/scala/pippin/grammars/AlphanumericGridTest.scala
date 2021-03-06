package pippin.grammars

import org.scalatest._
import flatspec._
import matchers._
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.propspec.AnyPropSpec

class AlphanumericGridTest extends AnyPropSpec with TableDrivenPropertyChecks with Matchers {

	property("Point equality") {
		val grid1 = AlphanumericGrid(4, 6)
		val grid2 = AlphanumericGrid(4, 6)
		val grid3 = AlphanumericGrid(9, 9)
		grid1.Point(1, 1) should equal (grid2.Point(1, 1))
		grid1.Point(1, 2) should not equal (grid2.Point(1, 1))
		grid1.Point(1, 1) should equal (grid3.Point(1, 1))
	}

	property("Point hash code") {
		val grid1 = AlphanumericGrid(4, 6)
		val grid2 = AlphanumericGrid(4, 6)
		val grid3 = AlphanumericGrid(9, 9)
		Set[PointLike](grid1.Point(1, 1)) contains grid1.Point(1, 1) should be (true)
		Set[PointLike](grid1.Point(1, 1)) contains grid2.Point(1, 1) should be (true)
		Set[PointLike](grid1.Point(1, 1)) contains grid2.Point(1, 2) should be (false)
		Set[PointLike](grid1.Point(1, 1)) contains grid3.Point(1, 1) should be (true) // unfortunate, but that's what we're stuck with
	}

	property("Index, rows, and columns") {
		val grid = AlphanumericGrid(4, 6)
		import grid.Point
		Point(1, 1).index should equal (1)
		Point(1, 2).index should equal (2)
		Point(1, 6).index should equal (6)
		Point(2, 1).index should equal (7)
		Point(4, 6).index should equal (4*6)
		Point(1, 1) should equal (new Point(1))
		Point(1, 2) should equal (new Point(2))
		Point(1, 6) should equal (new Point(6))
		Point(2, 1) should equal (new Point(7))
	}

}
