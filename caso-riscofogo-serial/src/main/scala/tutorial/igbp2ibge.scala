package tutorial

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.render._
import com.typesafe.config.ConfigFactory
import geotrellis.raster.mapalgebra.zonal._
import geotrellis.raster.mapalgebra.focal._
import scala.math._
import geotrellis.raster.summary._
import geotrellis.vector.Extent
import scala.collection.mutable
import geotrellis.spark.mapalgebra.local


object igbp2ibge {

	val ano_2016 = "2016"
        val ano_2017 = "2017"
		
	def ConvertFromIGBP_To_IBGE(igbp_class: Double) : Double = {
		
		if (igbp_class==0 || igbp_class==11 || igbp_class==13 || igbp_class==15 || igbp_class==16) {
			return 0
		}
		if (igbp_class==10) {
			return 1
		}    
		if (igbp_class==12 || igbp_class==14) {
			return 2
		}  
		if (igbp_class==7 || igbp_class==9) {
			return 3
		}  
		if (igbp_class==6 || igbp_class==8) {
			return 4
		}  
		if (igbp_class==1 || igbp_class==3 || igbp_class==5) {
			return 5
		}  
		if (igbp_class==4) {
			return 6
		}  
		if (igbp_class==2) {
			return 7
		}  
		
		return NODATA

	  }
	
	
	
	def main(args: Array[String]): Unit = {

		
		
		println("Obtendo tipo da vegetação...")	
		val vegetation_igbp = SinglebandGeoTiff("data/RiscoFogo/vegetacao/vegetacao_landcover_2012.tif")	 
		
		val vegetation_ibge = vegetation_igbp.tile.mapDouble{x => ConvertFromIGBP_To_IBGE(x)}
		println(" ")
		
		// gerando arquivo de saída 
    		val mb = ArrayMultibandTile(vegetation_ibge).convert(IntConstantNoDataCellType)
		
    		println("Gerando saída...")
    		MultibandGeoTiff(mb, vegetation_igbp.extent, vegetation_igbp.crs).write("data/RiscoFogo/vegetacao/vegetacao_landcover_2012_ibge.tif")
		
		//val mostrar = SinglebandGeoTiff("data/saida/landcover_2012_ibge.tif")	 
		//println(mostrar.tile.asciiDraw())
	}	
}
