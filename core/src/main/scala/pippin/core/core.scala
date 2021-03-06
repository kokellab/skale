package pippin

import sys.process._
import java.io.InputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.file.{Files, Path, Paths}
import java.security.MessageDigest
import java.sql.Blob
import java.util.concurrent.{ConcurrentMap, TimeUnit}

import javax.sql.rowset.serial.SerialBlob
import com.google.common.cache.CacheBuilder
import com.google.common.io.{BaseEncoding, ByteStreams}
import com.typesafe.config.{Config, ConfigException, ConfigFactory, ConfigParseOptions}
import com.typesafe.scalalogging.LazyLogging
import pippin.core.exceptions.MultipleElementsException
import scala.language.postfixOps

import scala.util.matching.Regex

package object core extends LazyLogging {

	def parseConfig(path: String): Config = parseConfig(Paths.get(path))
	def parseConfig(path: Path): Config =
		ConfigFactory.parseFile(path.toFile, ConfigParseOptions.defaults().setAllowMissing(false))

	def configOptionString(key: String)(implicit config: Config): Option[String] = configOption(key, config.getString)
	def configOptionInt(key: String)(implicit config: Config): Option[Int] = configOption(key, config.getInt)
	def configOptionBoolean(key: String)(implicit config: Config): Option[Boolean] = configOption(key, config.getBoolean)
	private def configOption[V](key: String, extractor: String => V): Option[V] = try {
		Some(extractor(key))
	} catch {
		case e: ConfigException.Missing => None
	}

	object RegexUtils {
		implicit class RichRegex(val underlying: Regex) extends AnyVal {
			def matches(s: String) = underlying.pattern.matcher(s).matches
		}
	}

	/** Logs an error message for any exception, then rethrows. */
	def withLoggedError[T](errorMessage: String, fn: () => T): T = withLoggedError(fn, errorMessage) // helpful for long functions

	/** Logs an error message for any exception, then rethrows. */
	def withLoggedError[T](fn: () => T, errorMessage: String): T = try {
			fn()
		} catch {
			case e: Exception =>
				logger.error(errorMessage, e)
				throw e
		}

	def only[A](a: Seq[A]) =
		if (a.size == 1) a.head
		else if (a.isEmpty) throw new NoSuchElementException("The sequence is empty")
		else throw new MultipleElementsException(s"Multiple elements in sequence of length ${a.length}")

	val sha1 = MessageDigest.getInstance("SHA-1")
	def bytesToHash(bytes: Iterable[Byte]): Array[Byte] = sha1.digest(bytes.toArray)
	def blobToBytes(blob: Blob): Array[Byte] = ByteStreams.toByteArray(blob.getBinaryStream)
	def bytesToHex(bytes: Iterable[Byte]) = BaseEncoding.base16().lowerCase.encode(bytes.toArray)
	def blobToHex(blob: Blob) = BaseEncoding.base16().lowerCase().encode(blobToBytes(blob))
	def bytesToBlob(bytes: Iterable[Byte]): Blob = new SerialBlob(bytes.toArray)
	def bytesToHashBlob(bytes: Iterable[Byte]): Blob = bytesToBlob(bytesToHash(bytes))
	def bytesToHashHex(bytes: Iterable[Byte]) = bytesToHex(bytesToHash(bytes))

	def hexToBytes(hex: String): Array[Byte] =
		new BigInteger(hex, 16).toByteArray()

	def floatsToBytes(values: Iterable[Float]): Iterable[Byte] =
		values flatMap (value => ByteBuffer.allocate(4).putFloat(value).array())

	def doublesToBytes(values: Iterable[Double]): Iterable[Byte] =
		values flatMap (value => ByteBuffer.allocate(8).putDouble(value).array())

	def intsToBytes(values: Iterable[Int]): Iterable[Byte] =
		values flatMap (value => ByteBuffer.allocate(4).putInt(value).array())

	def shortsToBytes(values: Iterable[Short]): Iterable[Byte] =
		values flatMap (value => ByteBuffer.allocate(2).putShort(value).array())

	def longsToBytes(values: Iterable[Long]): Iterable[Byte] =
		values flatMap (value => ByteBuffer.allocate(8).putLong(value).array())

	/**
		* Looks up all of the given keys in the map.
		* Throws an InvalidDataFormatException if conflicting values are found.
		* If the value is not found, returns None. If it is found (and is unique), returns the value.
		*/
	def uniqueLookupOption[A, B](keys: Set[A], map: Map[A, B]): Option[B] = {
		val newSet: Set[B] = keys.foldLeft(Set.empty[B])((current, next) => {
			if (map contains next) current + map(next) else current
		})
		if (newSet.size < 2) newSet.headOption
		else throw new IllegalArgumentException(s"There were ${newSet.size} conflicting values for keys {${keys.mkString(",")}}")
	}
	def uniqueLookup[A, B](keys: Set[A], map: Map[A, B]): B =
		uniqueLookupOption(keys, map) getOrElse (throw new IllegalArgumentException("No value for keys {${keys.mkString(\",\")}}"))

	def thisGitCommitSha1Hex = ("git rev-parse HEAD" !!).trim
	def thisGitCommitSha1Bytes = hexToBytes(("git rev-parse HEAD" !!).trim)

	def buildCache[K <: AnyRef, V <: AnyRef](
			maxSize: Int = 1000,
			expireAfterReadSeconds: Int = Int.MaxValue, expireAfterWriteSeconds: Int = Int.MaxValue
	): ConcurrentMap[K, V] =
		CacheBuilder.newBuilder()
			.maximumSize(maxSize)
			.expireAfterWrite(expireAfterReadSeconds, TimeUnit.SECONDS)
			.expireAfterAccess(expireAfterReadSeconds, TimeUnit.SECONDS)
			.concurrencyLevel(1)
			.build[K, V]().asMap()

	def trimWhitespaceAndQuotes(string: String): String =
		string.trim.stripPrefix("\"").stripSuffix("\"")

	def wrapFileInputStream(file: Path, task: InputStream => Unit): Unit = {
		val stream: InputStream = null
		try {
			task(Files.newInputStream(file))
		} finally {
			if (stream != null) stream.close()
		}
	}

	def readFileInChunks(file: Path, task: Array[Byte] => Unit) =
		wrapFileInputStream(file, stream => readStreamInChunks(stream, task))

	def readStreamInChunks(stream: InputStream, task: Array[Byte] => Unit, kbInBuffer: Int = 1024) = {
		var status: Int = 1
		var buffer: Array[Byte] = Array.ofDim(kbInBuffer * 1024)
		while (status > -1) {
			status = stream.read(buffer)
			task(buffer.slice(0, status))
		}
	}

}
