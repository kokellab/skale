package pippin.chem

import org.openscience.cdk.DefaultChemObjectBuilder
import org.openscience.cdk.inchi.{InChIToStructure, InChIGeneratorFactory}
import org.openscience.cdk.interfaces.{IAtomContainer, ITetrahedralChirality}
import org.openscience.cdk.smiles._
import org.openscience.cdk.silent.SilentChemObjectBuilder

object Chem {

	def smilesToInchi(smiles: String): Inchi =
		molToInchi(smilesToMol(smiles))

	def molToInchi(molecule: IAtomContainer): Inchi = {
		val generator = InChIGeneratorFactory.getInstance().getInChIGenerator(molecule)
		Inchi(generator.getInchi, generator.getInchiKey)
	}

	def inchiToSmiles(inchi: String): String =
		SmilesGenerator.absolute().create(inchiToMol(inchi))

	def inchiToInchikey(inchi: String): String =
		molToInchi(inchiToMol(inchi)).inchikey

	def smilesToMol(smiles: String): IAtomContainer =
		new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles)

	def inchiToMol(inchi: String): IAtomContainer = {
		val factory = InChIGeneratorFactory.getInstance()
		factory.getInChIToStructure(inchi, DefaultChemObjectBuilder.getInstance()).getAtomContainer
	}

	def connectivity(inchikey: String) = (inchikey split '-').head

	def nDefinedSterocenters(molecule: IAtomContainer): Int =
		List(molecule.stereoElements) count (_.isInstanceOf[ITetrahedralChirality])

}

case class Inchi(inchi: String, inchikey: String)
