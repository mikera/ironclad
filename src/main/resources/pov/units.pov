
#include "all-includes.inc"



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
		object {Unit_Rotations(Steam_Tank) translate <0,0*YS,0>}  
		object {Unit_Rotations(Rifles) translate <0,1*YS,0>}       
		
		object {Unit_Rotations(Fortress_Turret_Gun) translate <0,2*YS,0>}  
		object {Unit_Not_Rotations(Fortress_Turret_Base) translate <0,2*YS,0>}     
		
		object {Unit_Rotations(Paddle_Cruiser) translate <0,3*YS,0>}                 
		
		object {Unit_Rotations(Construction_Crawler) translate <0,4*YS,0>}             

		object {Unit_Single_Rotation(Mine) translate <6,4*YS,0>}             

		object {Unit_Single_Rotation(Target_Boiler) translate <7,4*YS,0>}             

		object {Unit_Single_Rotation(Target_Globe) translate <7,5*YS,0>}             
		
		object {Unit_Rotations(Patrol_Boat) translate <0,5*YS,0>}                
		
		object {Unit_Rotations(Assault_Zeppelin) translate <0,6*YS,0>}   

        object {Unit_Rotations(Armoured_Train) translate <0,7*YS,0>}   

        object {Unit_Rotations(Transport_Train) translate <0,8*YS,0>}   

        object {Unit_Rotations(Artillery_Train) translate <0,9*YS,0>}   

        object {Unit_Rotations(Artillery_Tank) translate <0,10*YS,0>}   
		
		object {Unit_Single_Rotation(Death_Ray_Balloon) translate <6,6*YS,0>}             
		object {Unit_Single_Rotation(Observation_Balloon) translate <7,6*YS,0>}    
		
		object {Village translate <6,2*YS,0>}   
		object {Factory translate <7,2*YS,0>}   

		object {Unit_Rotations(PillBox_Gun) translate <0,11*YS,0>}  
		object {Unit_Not_Rotations(PillBox_Block) translate <0,11*YS,0>}     

		object {Unit_Single_Rotation(Bunker) translate <6,11*YS,0>}             
		          
		object {Unit_Rotations(Battle_Tank) translate <0,12*YS,0>}

		object {Unit_Rotations(Heavy_Tank) translate <0,13*YS,0>}

		object {Unit_Rotations(Artillery) translate <0,14*YS,0>}

		object {Unit_Rotations(Militia) translate <0,15*YS,0>}       

		object {Unit_Rotations(Transport_Barge) translate <0,16*YS,0>}       

		object {Unit_Rotations(Artillery_Barge) translate <0,17*YS,0>}       
		
		object {Unit_Rotations(Tripod) translate <0,18*YS,0>}       

		object {Unit_Rotations(Heavy_Walker) translate <0,19*YS,0>}      

		object {Unit_Rotations(Minelayer_Tank) translate <0,20*YS,0>}      

		 
		// offset to position units to top left corner
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
