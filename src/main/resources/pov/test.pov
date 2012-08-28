#include "all-includes.inc"

object {
	sphere {<2,2,0> 2   }
	texture {
		pigment {
 	       wood
	        color_map {
	          [0.0 color DarkTan]
	          [0.9 color DarkBrown]
	          [1.0 color VeryDarkBrown]
	        }         
	        turbulence 0.05
		    scale 0.2
		}
	}
}             

object {
	sphere {<6,2,0> 2   }
	texture {
		pigment {
			bozo
	        color_map {
	          [0.0 color <0.5,0.6,0.8>]
	          [0.6 color <0.5,0.6,0.8>]    
	          [0.61 color <1.0,0.0,0.0>]
	          [1.0 color <1.0,0.0,0.0>]
	        }   	       
 	        turbulence 1
		}
		
	    normal {
	    	dents   
	    	scale 0.3
	    }          
	    
	    	    
	    finish {
	    	phong 1
	    	reflection 0.2
	    }
	}

}         

object {
	sphere {<10,2,0> 2   }
	texture {
		pigment {
 	       color <0.8,0.6,0.4>
 	       
		}
		
	    normal {
	    	bumps   
	    	scale 0.1
	    }     
	    
	    finish {
	    	Shiny
	    }
	}

}      

object {
	sphere {<6,6,0> 2   }
	  texture {
	    gradient x       //this is the PATTERN_TYPE
	    texture_map {
	      [0.3  pigment{Red} finish{phong 1}]
	      [0.9  pigment{DMFWood4} finish{Shiny}]
	    }

	}

}          

#declare T_Brass=texture {     
        pigment {color <0.8, 0.7, 0.2>}
        finish {
                phong 0.4
                reflection 0.4
        }             
        normal {
                bozo 3
                scale 0.04
        }
}

object {
	sphere {<10,6,0> 2   }
	  texture {
	    T_Brass
	}

}        

object {
	sphere {<2,6,0> 2   }
	  texture {
	    finish {
	      reflection 1
	    }

	}

}    
