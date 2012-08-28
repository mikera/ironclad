
#include "all-includes.inc"

//////////////////////////////////////////////////////////////////////////////////////////             
// zoomed object
#declare ZOOMED_OBJECT=Minelayer_Tank;




//////////////////////////////////////////////
// Unit creation macro  

#macro Unit_Rotations (Unit)
    union {
        object {Unit scale UNIT_SCALE  rotate z*-150 translate <0, 0, 0>}
        object {Unit scale UNIT_SCALE  rotate z*-90 translate <1, 0, 0>}
    	object {Unit scale UNIT_SCALE  rotate z*-30 translate <2, 0, 0>}
    	object {Unit scale UNIT_SCALE  rotate z*30 translate <3, 0, 0>}
        object {Unit scale UNIT_SCALE  rotate z*90 translate <4, 0, 0>}
        object {Unit scale UNIT_SCALE  rotate z*150 translate <5, 0, 0>}
    }
#end    

#macro Unit_Not_Rotations (Unit)
    union {
        object {Unit scale UNIT_SCALE  translate <0, 0, 0>}
    	object {Unit scale UNIT_SCALE  translate <1, 0, 0>}
    	object {Unit scale UNIT_SCALE  translate <2, 0, 0>}
        object {Unit scale UNIT_SCALE  translate <3, 0, 0>}
        object {Unit scale UNIT_SCALE  translate <4, 0, 0>}
        object {Unit scale UNIT_SCALE  translate <5, 0, 0>}
    }
#end    

#macro Unit_Single_Rotation (Unit)
    union {
        object {Unit scale UNIT_SCALE  }
    }
#end   


#macro Build_Units ()
	#include "components.inc"
	#include "units.inc"      
	#include "buildings.inc"      
	
	union {
		object {Unit_Rotations(ZOOMED_OBJECT) translate <0,0*YS,0>}  

		object {Unit_Single_Rotation(ZOOMED_OBJECT) translate <7,0*YS,0>}             

		translate <-0.5,-0.5*YS,0>
	}    
#end

//////////////////////////////////////////////
// Scene    

  


object {
	#declare P_Unit_Colour= pigment { color <0.1,0.7,0.3> }
	Build_Units() translate <0,0,0>
}  
       
object {
	#declare P_Unit_Colour= pigment { color <0.9,0.2,0.0> }
	Build_Units() translate <8,0,0>
}     

object {
	#declare P_Unit_Colour= pigment { color <0.4,0.4,0.8> }
	Build_Units() translate <16,0,0>
}      

object {
	#declare P_Unit_Colour= pigment { color <0.8,0.6,0.2> }
	Build_Units() translate <24,0,0>
}   

// redeclare to green
#declare P_Unit_Colour= pigment { color <0.3,0.6,0.1> }
#include "components.inc"
#include "units.inc"      
#include "buildings.inc"      


//object {
//	box {<-1,-1,-1>,<100,8,0>}
//	texture {
//		pigment {color <2,2,2>}
//	}
//}


#declare Zoomed_Object = object {
	union {
    		object {ZOOMED_OBJECT scale UNIT_SCALE }    
    		object {Grassland}     
    		//object {Buildings}     
		object {Rails(1) rotate z*90}    
	}
	
	scale 4
}             
             
object {
	Zoomed_Object
	translate <10, 10 ,0>		
}     

object {
	Zoomed_Object
	rotate x*(90-VIEW_ANGLE)
	translate <15, 10 ,0>		
}    

object {
	Zoomed_Object
	rotate z*90
	rotate x*(90-VIEW_ANGLE)   
	translate <20, 10 ,0>		
}

///////////////////////////////////////////////////////////////////////////////////////
// mini-scene
union {
    union {
		object {ZOOMED_OBJECT scale UNIT_SCALE rotate 30*z translate <0,0,0>}    
		object {Ground translate <0,0,0>}    
		object {Ground translate <0,1,0>} 
		object {Village translate <0,1,0>}
		object {Ground translate <0,2,0>}        
		object {Sea translate <0,3,0>}  
		translate <RATIO*0,0,0>
	}      
	
	union {
		object {Ground translate <0,0,0>}    
		object {Ground translate <0,1,0>}   
		object {Steam_Tank scale UNIT_SCALE rotate 30*z translate <0,1,0> }      
		object {Rifles scale UNIT_SCALE rotate 30*z translate <0,0,0> }          
		object {Paddle_Cruiser scale UNIT_SCALE rotate -30*z translate <0,2,0> }
		object {Sea translate <0,2,0>}  
		translate <RATIO*1,0.5,0>
	}     

	union {
		object {Ground translate <0,0,0>}    
		object {Ground translate <0,1,0>}   
		object {Rails(0) translate <0,0,0>}    
		object {Earth(0) translate <0,0,0>}    
		object {Rails(4) translate <0,0,0>}    
		object {Earth(4) translate <0,0,0>}    
		object {Rails(1) translate <0,1,0>}    
		object {Earth(1) translate <0,1,0>}    
		object {Armoured_Train rotate z*90 translate <0,1,0>}    
		object {Sea translate <0,2,0>}  
		translate <RATIO*2,0.0,0>
	}     

	
	intersection {         
	    plane {<0,0,1>,-1}
	    box {<-1,-1.5,-2> <6,6,0>}
	 
		texture {pigment {color <0,0,0>}}
	}
	
	
	translate <0, 6 ,0>	
}