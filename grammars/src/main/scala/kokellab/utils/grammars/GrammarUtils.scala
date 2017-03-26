package kokellab.utils.grammars

import breeze.stats.distributions.{RandBasis, ThreadLocalRandomGenerator}
import org.apache.commons.math3.random.MersenneTwister
import org.parboiled2.{ErrorFormatter, ParseError, Parser}

object GrammarUtils {

	def randBasis(seed: Int): RandBasis = new RandBasis(new ThreadLocalRandomGenerator(new MersenneTwister(seed)))

	def replaceCommon(expression: String): String = commonReplacements.foldLeft(expression) ((e, s) => e.replaceAllLiterally(s._1, s._2))

	def wrapGrammarException[A](expression: String, parser: Parser, parse: () => A): A = try {
			parse()
		} catch {
			case e: ParseError =>
				throw new GrammarException(s"The expression $expression could not be parsed",
					Some(parser.formatError(e, new ErrorFormatter(showExpected = true, showFrameStartOffset = true, showLine = true, showPosition = true, showTraces = true))), Some(e))
		}

	private val commonReplacements = Map(" " -> "", "−" -> "-", "!=" -> "≠", "<=" -> "≤", ">=" -> "≥", "==" -> "=", "~=" -> "≈", "!~=" -> "≉", "∞" -> "Infinity", "infinity" -> "Infinity", "inf" -> "Infinity", "Inf" -> "Infinity")
}
