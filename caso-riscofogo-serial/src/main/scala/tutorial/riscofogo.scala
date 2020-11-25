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


object riscofogo {
	
	def logica_ComputeVegetationFactor(vegetation_class: Double) : Double = {
		

		    if (vegetation_class == 0 || vegetation_class == 13 || vegetation_class==15 || vegetation_class == 16) {
			return 0
		    }  
		    if (vegetation_class == 2 || vegetation_class==11) {
			return 1.5
		    }
		    if (vegetation_class==4) {
			return 1.72
		    }
		    if (vegetation_class==1 || vegetation_class==3 || vegetation_class==5) {
			return 2
		    }
		    if (vegetation_class==6 || vegetation_class==8) {
			return 2.4
		    }
		    if (vegetation_class==7 || vegetation_class==9) {
			return 3
		    }	
		    if (vegetation_class==12 || vegetation_class==14) {
			return 4
		    }
		    if (vegetation_class==10) {
			return 6
		    }
		    return 0
	  }



	def logica_CapByVegetation(igbp_class: Double, pse: Double): Double = {

	//	val ibge_class = ConvertFromIGBP_To_IBGE(igbp_class)
	
		if(igbp_class == 1){
			if(pse<30){
				return pse	
			} else {
				return 30
			}	
		}
		if(igbp_class == 2){
			if(pse<45){
				return pse	
			} else {
				return 45
			}	
		}
		if(igbp_class == 3){
			if(pse<60){
				return pse	
			} else {
				return 60
			}	
		}
		if(igbp_class == 4){
			if(pse<75){
				return pse	
			} else {
				return 75
			}	
		}
		if(igbp_class == 5){
			if(pse<90){
				return pse	
			} else {
				return 90
			}	
		}
		if(igbp_class == 6){
			if(pse<105){
				return pse	
			} else {
				return 105
			}	
		}
		if(igbp_class == 7){
			if(pse<120){
				return pse	
			} else {
				return 120
			}	
		}
		return NODATA
	}
	
	/**
	def sumPAndCalculateFP(_tile: Tile, const: Double): Tile = {
		var colunas = _tile.cols
		val linhas = _tile.rows
		val cellType = _tile.cellType
				
		var i = 0
		var j = 0
		val tile = ArrayTile.empty(cellType, colunas, linhas)

		for(i<-0 to linhas){
			for(j<-0 to colunas){
				var valor_atualizado = exp(_tile.tile.get(i,j)*const)
				tile.setDouble(i, j, valor_atualizado)
			}
		}
		return tile
	}	
	**/


	def multiplicar(tile1: Tile, tile2: Tile): Tile = {	
		
		val tile = tile1*tile2
		return tile
	}
	
	
	def ComputeVegetationFactor(vegetation_class: Tile): Tile = {

		var retorno = 0
		var colunas = vegetation_class.cols
		var linhas = vegetation_class.rows
		var cellType = vegetation_class.cellType

		val tile = ArrayTile.empty(cellType, colunas, linhas)
		
		for(i<-0 to linhas){
			for(j<-0 to colunas){
				 var valor_atualizado = logica_ComputeVegetationFactor(vegetation_class.tile.get(i,j));			
				tile.setDouble(i, j, valor_atualizado)
			}
		}
		return tile 
	}

	def CapByVegetation(igbp_class: Tile, pse: Tile): Tile = {

		var retorno = 0
		var colunas = igbp_class.cols
		var linhas = igbp_class.rows
		var cellType = igbp_class.cellType

		val tile = ArrayTile.empty(cellType, colunas, linhas)
		
		for(i<-0 to linhas){
			for(j<-0 to colunas){
				var valor_atualizado = logica_CapByVegetation(igbp_class.tile.get(i,j), pse.tile.get(i,j));			
				tile.setDouble(i, j, valor_atualizado)
			}
		}
		return tile 
	}

	def calculationRB(tile: Tile, const: Double): Tile = 
	{
		return tile.tile*const
	
	}
	// Cores que podemos colocar :)
	// Console.BLUE
	// Console.CYAN
	// Console.RED
	// Console.YELLOW
	// Console.WHITE
	// Console.MAGENTA
	// Console.GREEN
	

	var nc = ""
	var anos_validos = Set("2016", "2017")
	
	println("")
	println("")
	println(Console.GREEN+"+---------------------------------------------------------------------------------------+")
	println(Console.GREEN+"|"+Console.GREEN+"\tOoi ! esse script gera o mapa de risco de fogo para um determinado ano :)\t"+Console.GREEN+"|")
	//println("|\t									     \t|")
	println("+---------------------------------------------------------------------------------------+")
	println(Console.GREEN+"|"+Console.GREEN+"\tPodemos realizar os cáculos para os seguintes anos:  		     	\t"+Console.GREEN+"|")
	println(Console.GREEN+"|\t "+Console.GREEN+">2016									\t"+Console.GREEN+ "|")									
	println(Console.GREEN+"|\t "+Console.GREEN+">2017									\t"+Console.GREEN+"|")	
	println("+---------------------------------------------------------------------------------------+")
	println("")
	println("")
	var ano = readLine(Console.WHITE+"Digite o ano: "+Console.WHITE+"")
	
	while(!(anos_validos contains ano))
	{
		println(" ")
		println(Console.RED + s">>>>>Vc errou :( O ano de ${ano} não faz parte da nossa base de dados !<<<<<")
		ano = readLine(Console.WHITE+"Digite o ano, novamente: "+Console.WHITE+"")
			
	}	

	if(ano=="2016")
	{
		nc = ".nc"
	}	
	
	println(Console.WHITE)

	def main(args: Array[String]): Unit = {

		
		// Cálculo do PSE (dias de secura)
		
			

		println(Console.CYAN+"Lendo precipitação precipitação..."+Console.WHITE+"")
		println("Lendo precipitação precipitação 2d-1d...")
//----------------------------------------------------------------------------

		val p1 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201707151200.tif")
		val p4 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201707121200.tif")
		val p5 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201707111200.tif")
		val p10_6 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201707101200.tif")
		val p10_8 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201707081200.tif")
		val p15_13 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201707031200.tif")
		val p15_15 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201707011200.tif")
		val p30_17 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201706291200.tif")
		val p30_19 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201706271200.tif")
		val p30_22 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201706241200.tif")
		val p30_23 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201706231200.tif")
		val p30_24 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201706221200.tif")
		val p30_26 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201706191200.tif")
		val p30_28 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201706171200.tif")
		val p30_30 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201706151200.tif")
		val p60_39 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201706061200.tif")
		val p60_47 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201705291200.tif")
		val p60_50 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201705261200.tif")
		val p60_51 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201705251200.tif")
		val p60_53 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201705231200.tif")
		val p60_56 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201705201200.tif")
		val p60_57 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201705191200.tif")
		val p60_60 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201705161200.tif")
		val p90_62 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201705141200.tif")
	        val p90_67 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201705091200.tif")
		val p90_71 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201705051200.tif")
		val p90_74 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201705021200.tif")
		val p90_76 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704301200.tif")
		val p90_77 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704291200.tif")
		val p90_79 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704271200.tif")
		val p90_80 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704261200.tif")
		val p90_81 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704251200.tif")
		val p90_87 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704191200.tif")
		val p90_90 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704161200.tif")
		val p120_92 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704141200.tif")
		val p120_94 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704121200.tif")
		 val p120_98 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704081200.tif")
		val p120_99 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704071200.tif")
		val p120_102 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704041200.tif")
		val p120_105 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201704011200.tif")
		val p120_107 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201703301200.tif")
		val p120_108 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201703291200.tif")
		val p120_110 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201703271200.tif")
		val p120_112 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201703251200.tif")
		val p120_113 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201703241200.tif")
		val p120_115 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201703221200.tif")
		 val p120_116 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201703211200.tif")
		val p120_117 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201703201200.tif")
		val p120_118 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_2017/S10648241_201703191200.tif")


//----------------------------------------------------------------------------
    		//val p1 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07151200${nc}.tif")
    		val p2 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07141200${nc}.tif")
		println("Lendo precipitação precipitação 3d-2d...")
    		val p3 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07131200${nc}.tif")
		println("Lendo precipitação precipitação 4d-3d...")
    		//val p4 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07121200${nc}.tif")
		println("Lendo precipitação precipitação 5d-4d...")
    		//val p5 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07111200${nc}.tif")

		println("Lendo precipitação precipitação 10d-6d...") 
    		//val p10_6 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07101200${nc}.tif")
   		val p10_7 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07091200${nc}.tif")
   		//val p10_8 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07081200${nc}.tif")
   		val p10_9 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07071200${nc}.tif")
   		val p10_10 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07061200${nc}.tif")

		println("Lendo precipitação precipitação 15d-11d...")
    		val p15_11 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07051200${nc}.tif")
   		val p15_12 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06041200${nc}.tif")
   		//val p15_13 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07031200${nc}.tif")
   		val p15_14 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07021200${nc}.tif")
   		//val p15_15 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}07011200${nc}.tif")

		println("Lendo precipitação precipitação 30d-16d...")
    		val p30_16 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06301200${nc}.tif")
    		//val p30_17 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06291200${nc}.tif")
    		val p30_18 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06281200${nc}.tif")
    		//val p30_19 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06271200${nc}.tif")
    		val p30_20 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06261200${nc}.tif")
    		val p30_21 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06251200${nc}.tif")
    		//val p30_22 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06241200${nc}.tif")
    		//val p30_23 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06231200${nc}.tif")
    		//val p30_24 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06221200${nc}.tif")
    		val p30_25 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06211200${nc}.tif")
    		//val p30_26 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06191200${nc}.tif")
    		val p30_27 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06181200${nc}.tif")
    		//val p30_28 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06171200${nc}.tif")
    		val p30_29 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06161200${nc}.tif")
    		//val p30_30 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06151200${nc}.tif")


		println("Lendo precipitação precipitação 60d-31d...")
		val p60_31 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06141200${nc}.tif")
		val p60_32 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06131200${nc}.tif")
		val p60_33 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06121200${nc}.tif")
		val p60_34 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06111200${nc}.tif")
		val p60_35 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06101200${nc}.tif")
		val p60_36 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06091200${nc}.tif")
		val p60_37 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06081200${nc}.tif")
		val p60_38 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06071200${nc}.tif")
		//val p60_39 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06061200${nc}.tif")
		val p60_40 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06051200${nc}.tif")
		val p60_41 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06041200${nc}.tif")
		val p60_42 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06031200${nc}.tif")
		val p60_43 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06021200${nc}.tif")
		val p60_44 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}06011200${nc}.tif")
		val p60_45 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05311200${nc}.tif")
		val p60_46 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05301200${nc}.tif")
		//val p60_47 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05291200${nc}.tif")
		val p60_48 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05281200${nc}.tif")
		val p60_49 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05271200${nc}.tif")
		//val p60_50 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05261200${nc}.tif")
		//val p60_51 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05251200${nc}.tif")
		val p60_52 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05241200${nc}.tif")
		//val p60_53 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05231200${nc}.tif")
		val p60_54 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05221200${nc}.tif")
		val p60_55 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05211200${nc}.tif")
		//val p60_56 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05201200${nc}.tif")
		//val p60_57 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05191200${nc}.tif")
		val p60_58 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05181200${nc}.tif")
		val p60_59 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05171200${nc}.tif")
		//val p60_60 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05161200${nc}.tif")

		println("Lendo precipitação precipitação 90d-61d...")
    		val p90_61 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05151200${nc}.tif")
    		//val p90_62 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05141200${nc}.tif")
    		val p90_63 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05131200${nc}.tif")
    		val p90_64 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05121200${nc}.tif")
    		val p90_65 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05111200${nc}.tif")
    		val p90_66 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05101200${nc}.tif")
    		//val p90_67 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05091200${nc}.tif")
    		val p90_68 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05081200${nc}.tif")
    		val p90_69 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05071200${nc}.tif")
    		val p90_70 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05061200${nc}.tif")
    		//val p90_71 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05051200${nc}.tif")
    		val p90_72 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05041200${nc}.tif")
    		val p90_73 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05031200${nc}.tif")
    		//val p90_74 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05021200${nc}.tif")
    		val p90_75 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}05011200${nc}.tif")
    		//val p90_76 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04301200${nc}.tif")
    		//val p90_77 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04291200${nc}.tif")
    		val p90_78 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04281200${nc}.tif")
    		//val p90_79 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04271200${nc}.tif")
    		//val p90_80 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04261200${nc}.tif")
    		//val p90_81 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04251200${nc}.tif")
    		val p90_82 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04241200${nc}.tif")
    		val p90_83 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04231200${nc}.tif")
    		val p90_84 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04221200${nc}.tif")
    		val p90_85 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04211200${nc}.tif")
    		val p90_86 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04201200${nc}.tif")
    		//val p90_87 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04191200${nc}.tif")
    		val p90_88 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04181200${nc}.tif")
    		val p90_89 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04171200${nc}.tif")
    		//val p90_90 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04161200${nc}.tif")

		println("Lendo precipitação precipitação 120d-91d...")
    		val p120_91 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04151200${nc}.tif")
    		//val p120_92 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04141200${nc}.tif")
    		val p120_93 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04131200${nc}.tif")
    		//val p120_94 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04121200${nc}.tif")
    		val p120_95 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04111200${nc}.tif")
    		val p120_96 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04101200${nc}.tif")
    		val p120_97 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04091200${nc}.tif")
    		//val p120_98 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04081200${nc}.tif")
    		//val p120_99 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04071200${nc}.tif")
    		val p120_100 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04061200${nc}.tif")
    		val p120_101 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04051200${nc}.tif")
    		//val p120_102 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04041200${nc}.tif")
    		val p120_103 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04031200${nc}.tif")
    		val p120_104 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04021200${nc}.tif")
    		//val p120_105 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}04011200${nc}.tif")

    		val p120_106 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03311200${nc}.tif")
    		//val p120_107 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03301200${nc}.tif")
    		//val p120_108 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03291200${nc}.tif")
    		val p120_109 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03281200${nc}.tif")
    		//val p120_110 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03271200${nc}.tif")
    		val p120_111 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03261200${nc}.tif")
    		//val p120_112 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03251200${nc}.tif")
    		//val p120_113 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03241200${nc}.tif")
    		val p120_114 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03231200${nc}.tif")
    		//val p120_115 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03221200${nc}.tif")
    		//val p120_116 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03211200${nc}.tif")
    		//val p120_117 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03201200${nc}.tif")
    		//val p120_118 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03191200${nc}.tif")
    		val p120_119 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03181200${nc}.tif")
    		val p120_120 = SinglebandGeoTiff(s"data/RiscoFogo/precipitacao_${ano}/S10648241_${ano}03171200${nc}.tif")
		println(Console.YELLOW+"Carregou direitinho..."+Console.WHITE)
		println(" ")
		
		// Combina as tiles no intervalo determinado 
		println(Console.CYAN+"Realizando a soma dos tiles..."+Console.WHITE+"")
		val p1_total = p1.tile;
		println("Realizando a soma dos tiles no intervalo de tempo 2d-1d...")
		val p2_total = p2.tile
		println("Realizando a soma dos tiles no intervalo de tempo 3d-2d...")
		val p3_total = p3.tile
		println("Realizando a soma dos tiles no intervalo de tempo 4d-3d...")
		val p4_total = p4.tile
		println("Realizando a soma dos tiles no intervalo de tempo 5d-4d...")
		val p5_total = p5.tile
		println("Realizando a soma dos tiles no intervalo de tempo 10d-6d...")
		val p10_total = p10_6.tile + 
				p10_7.tile + 
				p10_8.tile + 
				p10_9.tile + 
				p10_10.tile 
		println("Realizando a soma dos tiles no intervalo de tempo 15d-11d...")
		val p15_total = p15_11.tile +
				p15_12.tile +
				p15_13.tile +
				p15_14.tile +
				p15_15.tile 	
		println("Realizando a soma dos tiles no intervalo de tempo 30d-16d...")
		val p30_total = p30_16.tile +
				p30_17.tile +
				p30_18.tile +
				p30_19.tile +
				p30_20.tile +
				p30_21.tile +
				p30_22.tile +
				p30_23.tile +
				p30_24.tile +
				p30_25.tile +
				p30_26.tile +
				p30_27.tile +
				p30_28.tile +
				p30_29.tile +
				p30_30.tile 
		println("Realizando a soma dos tiles no intervalo de tempo 60d-31d...")
		val p60_total = p60_31.tile + 
				p60_32.tile +
				p60_33.tile +
				p60_34.tile +
				p60_35.tile +
				p60_36.tile +
				p60_37.tile +
				p60_38.tile +
				p60_39.tile +
				p60_40.tile +
				p60_41.tile +
				p60_42.tile +
				p60_43.tile +
				p60_44.tile +
				p60_45.tile +
				p60_46.tile +
				p60_47.tile +
				p60_48.tile +
				p60_49.tile +
				p60_50.tile +
				p60_51.tile +
				p60_52.tile +
				p60_53.tile +
				p60_54.tile +
				p60_55.tile +
				p60_56.tile +
				p60_57.tile +
				p60_58.tile +
				p60_59.tile +
				p60_60.tile 
		println("Realizando a soma dos tiles no intervalo de tempo 90d-61d...")
		val p90_total = p90_61.tile +
				p90_62.tile +
				p90_63.tile +	
				p90_64.tile +	
				p90_65.tile +	
				p90_66.tile +	
				p90_67.tile +	
				p90_68.tile +	
				p90_69.tile +	
				p90_70.tile +	
				p90_71.tile +	
				p90_72.tile +	
				p90_73.tile +	
				p90_74.tile +	
				p90_75.tile +	
				p90_76.tile +	
				p90_77.tile +	
				p90_78.tile +	
				p90_79.tile +	
				p90_80.tile +	
				p90_81.tile +	
				p90_82.tile +	
				p90_83.tile +	
				p90_84.tile +	
				p90_85.tile +	
				p90_86.tile +	
				p90_87.tile +	
				p90_88.tile +	
				p90_89.tile +	
				p90_90.tile 
		println("Realizando a soma dos tiles no intervalo de tempo 120d-91d...")
		val p120_total = p120_91.tile +
				 p120_92.tile +
				 p120_93.tile +
				 p120_94.tile +
				 p120_95.tile +
				 p120_96.tile +
				 p120_97.tile +
				 p120_98.tile +
				 p120_99.tile +
				 p120_100.tile +
				 p120_101.tile +
				 p120_102.tile +
				 p120_103.tile +
				 p120_104.tile +
				 p120_105.tile +
				 p120_106.tile +
				 p120_107.tile +
				 p120_108.tile +
				 p120_109.tile +
				 p120_110.tile +
				 p120_111.tile +
				 p120_112.tile +
				 p120_113.tile +
				 p120_114.tile +
				 p120_115.tile +
				 p120_116.tile +
				 p120_117.tile +
				 p120_118.tile +
				 p120_119.tile +
				 p120_120.tile 
		println(Console.YELLOW+"Somou tudo direitinho :) ...")	
		println(" ")
						
		println(Console.CYAN+"Realizando o cálculo do fp..."+Console.WHITE+"")		
		println("Realizando o cálculo do fp1...")
		var fp1 = p1_total.tile.mapDouble{x => x*(-0.14)}
		fp1 = fp1.tile.mapDouble{x => math.pow(2.718281828459045235360287,x)}
		println("Realizando o cálculo do fp2...")
		var fp2 = p2_total.tile.mapDouble{x => x*(-0.07)}	
		fp2 = fp2.tile.mapDouble{x => math.pow(2.718281828459045235360287,x)}
		println("Realizando o cálculo do fp3...")
		var fp3 = p3_total.tile.mapDouble{x => x*(-0.04)}
		fp3 = fp3.tile.mapDouble{x => math.pow(2.718281828459045235360287,x)}
		println("Realizando o cálculo do fp4...")
		var fp4 = p4_total.tile.mapDouble{x => x*(-0.03)}
		fp4 = fp4.tile.mapDouble{x => math.pow(2.718281828459045235360287,x)}
		println("Realizando o cálculo do fp5...")
		var fp5 = p5_total.tile.mapDouble{x => x*(-0.02)}
		fp5 = fp5.tile.mapDouble{x => math.pow(2.718281828459045235360287,x)}
		println("Realizando o cálculo do fp10...")
		var fp10 = p10_total.tile.mapDouble{x => x*(-0.01)}
		fp10 = fp10.tile.mapDouble{x => math.pow(2.718281828459045235360287,x)}
		println("Realizando o cálculo do fp15...")
		var fp15 = p15_total.tile.mapDouble{x => x*(-0.008)}
		fp15 = fp15.tile.mapDouble{x => math.pow(2.718281828459045235360287,x)}
		println("Realizando o cálculo do fp30...")
		var fp30 = p30_total.tile.mapDouble{x => x*(-0.004)}
		fp30 = fp30.tile.mapDouble{x => math.pow(2.718281828459045235360287,x)}
		println("Realizando o cálculo do fp60...")
		var fp60 = p60_total.tile.mapDouble{x => x*(-0.002)}
		fp60 = fp60.tile.mapDouble{x => math.pow(2.718281828459045235360287,x)}	
		println("Realizando o cálculo do fp90...")
		var fp90 = p90_total.tile.mapDouble{x => x*(-0.001)}
		fp90 = fp90.tile.mapDouble{x => math.pow(2.718281828459045235360287,x)}
		println("Realizando o cálculo do fp120...")
		var fp120 = p120_total.tile.mapDouble{x => x*(-0.0007)}
		fp120 = fp120.tile.mapDouble{x => math.pow(2.718281828459045235360287,x)}	
		println(Console.YELLOW+"Realizou os cálculos direitinho :) ...")
		println(" ")
		
		println("")
		fp1 = fp1 * 105
                var PSE = multiplicar(fp1, fp2)
		    PSE= multiplicar(PSE, fp3)
		    PSE = multiplicar(PSE, fp4)
		    PSE = multiplicar(PSE, fp5)
		    PSE = multiplicar(PSE, fp10)
		    PSE = multiplicar(PSE, fp15)
		    PSE = multiplicar(PSE, fp30)
		    PSE = multiplicar(PSE, fp60)
		    PSE = multiplicar(PSE, fp90)
		    PSE = multiplicar(PSE, fp120)
		println(Console.YELLOW+"Finalizado cálculo do PSE... :) "+Console.WHITE+"")
		println(" ")
		
		println(Console.CYAN+"Obtendo tipo da vegetação..."+Console.WHITE+"")	
		val vegetation_type = SinglebandGeoTiff("data/RiscoFogo/vegetacao/vegetacao_landcover_2012.tif")	 
		
		val A = ComputeVegetationFactor(vegetation_type.tile)
		
		val vegetation_type_ibge = SinglebandGeoTiff("data/RiscoFogo/vegetacao/vegetacao_landcover_2012_ibge.tif")	
		println(Console.CYAN+"Ajuste dos dias de secura pelo máximo..."+Console.WHITE+"")
		var _PSE = CapByVegetation(vegetation_type_ibge.tile,PSE)
		
		var A_mult_PSE =  multiplicar(A, _PSE)
		var A_mult_PSE_subtract_90 = A_mult_PSE.tile.mapDouble{x => (x-90)*3.1416/180}
		var sin_A_mult_PSE = A_mult_PSE_subtract_90.tile.localSin()  
		var som_1 = sin_A_mult_PSE.tile.mapDouble{ x => x+1.0}
		var RB_1 = som_1.mapDouble{x => 0.9*x*0.5}
		
		println(Console.CYAN+"Cálculo de risco de fogo..."+Console.WHITE+"")
		var ufac = SinglebandGeoTiff(s"data/RiscoFogo/umidade/UMRS${ano}071618${ano}071618.tif")	
		val _ufac = ufac.tile.mapDouble{x => x*(-0.006)+1.3}
		println(Console.CYAN+"Fator de umidade..."+Console.WHITE+"")
				
		var tfac = SinglebandGeoTiff(s"data/RiscoFogo/temperatura/TEMP${ano}071618${ano}071618.tif")
		val _tfac = tfac.tile.mapDouble{x => x*(0.02)+0.4}
				
		println(Console.CYAN+"Cálculo de temperatura..."+Console.WHITE+"")	
		val ufac_tfac = multiplicar(_ufac, _tfac)
		val valor_total = multiplicar(RB_1,ufac_tfac)
		val valor_final = valor_total.tile.normalize(0,8,0,1)	
		// gerando arquivo de saída 
    		val mb = ArrayMultibandTile(valor_final).convert(DoubleConstantNoDataCellType)
		//mb.foreach{x => println(x.getDouble())}		
    		println(Console.CYAN+"Gerando mapa de risco de fogo..."+Console.WHITE+"")
    		MultibandGeoTiff(mb, p1.extent, p1.crs).write(s"data/saida/mapa_risco_fogo_${ano}.tif")	
		
		
	}	
}
