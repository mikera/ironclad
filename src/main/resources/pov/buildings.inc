




//////////////////////////////////////////////////////////////////
// Building textures


#declare T_Building = texture {   
	pigment {
	    color <0.7,0.6,0.5>
	}    	
	finish {  
	}   
	normal {
		bumps 0.4  
		scale 0.01
	}
}	             






//////////////////////////////////////////////////////////////////
// Village                              
                              
#declare BUILDING_ROTATION=30;

        

#declare Building = object {
	difference {
		box {<-1,-1,0>, <1,1,2> texture {T_Building}}
		plane {<-1,0,-1>, -1.4 texture {T_Unit_Colour}}  
		plane {<1,0,-1>, -1.4 texture {T_Unit_Colour}}  
	}      
	
	scale 0.1          
  
}          

#declare Buildings = union {
	object {Building scale <1 1.5 1> translate <-0.1, -0.15, 0>  }        
	object {Building scale <1 1 2> translate <0.2, 0.15, 0> }
	object {Building scale <1.5 1 1.5>   translate <-0.2, 0.25, 0>}         
	
	rotate BUILDING_ROTATION*z  
}     

#declare Village =union {
	object {Buildings}
	object {Earth_Area}
}      


#declare Factory = union {    
	union {
		object {Building scale <1 2 1.5> translate <-0.15, 0, 0>  }        
		object {Building scale <1 2 1.5> translate <0.0, 0, 0> }
		object {Building scale <1 2 1.5>   translate <0.15, 0, 0>}            
		rotate z*90
	}        
	
	object {Chimney scale 0.5 translate <-0.2,0.2,0>}
	object {Chimney scale 0.2 translate <-0.1,0.25,0>}
	object {Chimney scale 0.2 translate <-0.0,0.25,0>}


	
	object {Boiler scale 0.2 translate <0.25,0,0>}
	
	scale 1.4
	
	rotate BUILDING_ROTATION*z  
}    