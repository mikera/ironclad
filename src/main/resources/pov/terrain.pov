
#include "all-includes.inc"



//////////////////////////////////////////////
// Scene    


object {White_Hex}   

object {Grassland translate <2,0,0>}  

object {Rocky_Grassland translate <2,2*YS,0>}            

object {Wooded_Grassland translate <2,4*YS,0>}     


object {Hills translate <8,0,0>}         

object {Rocky_Hills translate <8,2*YS,0>}            

object {Wooded_Hills translate <8,4*YS,0>}            

       
  

object {Sea translate <4,0,0>}        

object {Sea_Rocks translate <4,2*YS,0>}   

object {Deep_Sea translate <4,4*YS,0>}   

object {Coast_Area translate <4,6*YS,0>}   

object {Trench translate <6,0*YS,0>}   



object {Woods translate <10,0,0>}     

object {Mountains translate <14,0,0>}   

object {Impassable_Mountains translate <14,2*YS,0>}     


object {Village translate <0,2*YS,0>}     
            
object {Grey_Rock translate <0,4*YS,0>}   
             
// roads and rail 
#local i=0;
#while (i<6)
	object {Road(i) translate <16,i*2*YS,0>}   
	object {Earth(i) translate <18,i*2*YS,0>}   
	object {Rails(i) translate <20,i*2*YS,0>}
	object {Razor_Wire rotate (z*(-60+60*i)) translate <22,i*2*YS,0> }  
	object {Trench_Connection rotate (z*(-60+60*i)) translate <24,i*2*YS,0> }  
	object {Coastline(i) translate <26,i*2*YS,0> }  
	#local i=i+1;
#end
             
// zoomed object
object {
    union {
    	object {Grassland}     
    	//object {Buildings}     
    	object {Battle_Tank}    
    	object {Rivet_Ring}
	}
	
	scale 4
	translate <10, 22 ,0>	
}             
             

// mini-scene
union {
	union {           
		union {
			object {Grassland translate <0,0,0>}    
			union {
				object {Grassland translate <0,1,0>} 
				object {Buildings translate <0,1,0>}            
				//translate STEP_HEIGHT*z
			}
			object {Trench translate <0,2,0>}      
			object {Revolve(Razor_Wire,z*60,6) translate <0,2,0>}      
			object {Trench_Connection translate <0,2,0>}      
			object {Coastline(1) translate<0,3,0> }
			translate STEP_HEIGHT*z      
		}
		object {Sea translate <0,3,0>}  
		translate <RATIO*0,0,0>
	}      
	
	union {
		union {
			object {Grassland translate <0,0,0>}    
			object {Road(4) translate <0,0,0>}    
			object {Road(1) translate <0,1,0>}    
			object {Earth(4) translate <0,0,0>}    
			object {Earth(1) translate <0,1,0>}    
			object {Grassland translate <0,1,0>}   
			object {Battle_Tank rotate 30*z translate <0,1,0> }
			object {Coastline(1) translate<0,2,0> }
			object {Coastline(0) translate<0,2,0> }
			translate STEP_HEIGHT*z      
		}
		object {Deep_Sea translate <0,2,0>}  
		translate <RATIO*1,0.5,0>
	}       
	
	union {
		union {
			object {Grassland translate <0,0,0>}    
			object {Rails(0) translate <0,0,0>}    
			object {Earth(0) translate <0,0,0>}    
			object {Rails(4) translate <0,0,0>}    
			object {Earth(4) translate <0,0,0>}    
			object {Rails(1) translate <0,1,0>}    
			object {Earth(1) translate <0,1,0>}    
			object {Grassland translate <0,1,0>}   
			object {Coastline(1) translate<0,2,0> }
			object {Coastline(0) translate<0,2,0> }
			translate STEP_HEIGHT*z      
		}
		object {Deep_Sea translate <0,2,0>}  
		translate <RATIO*2,0,0>
	}     

	
	intersection {         
	    plane {<0,0,1>,-1}
	    box {<-1,-2,-2> <6,6,0>}
	 
		texture {pigment {color <0,0,0>}}
	}
	
	
	translate <0, 20 ,0>	
}