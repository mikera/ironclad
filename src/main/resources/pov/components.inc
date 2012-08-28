                                           
                                           
                                           
///////////////////////////////////////////////////////////
// Component textures                                           
                                           
                                           
                               

#declare T_Unit_Brass = texture {
	pigment {
		color rgb <1.5,1.2,0.6>
	}    
	finish {
		reflection 0.35
		phong 0.7
	}

}           

#declare T_Unit_Metal = texture {
	pigment {
	    P_Unit_Colour
	}    	
	finish {  
		reflection 0.1
		phong 0.3
	}   
	normal {
		bumps 1  
		scale 0.01
	}
}        

#declare T_Unit_Colour = texture {
	pigment {
	    P_Unit_Colour
	}    	
}                                              
                                           
                                    

#declare T_Dark = texture {
	pigment {
	    color <0.1,0.1,0.1>
	}    	
	finish {  
		
		phong 1
	}   
}       


#declare T_Wooden_Deck = texture {
	pigment {
	    function {y}
	    pigment_map {
	    	[0 color <0.5,0.3,0.1>]
	    	[1 color <0.2,0.1,0.0>]
	    }
	}    	
	finish {  
	}    
	scale 0.02
}      

#declare T_Light_Wood = texture {
	pigment {
	    function {y}
	    pigment_map {
	    	[0 color <0.8,0.6,0.1>]
	    	[1 color <0.5,0.3,0.1>]
	    }
	}    	
	finish {  
	}    
	scale 0.02
}   

#declare T_Airship_Balloon = texture {
	pigment {
	    color <1,1,1>
	}
	finish {
		specular 1
	}    	

}                                                
                                           
                                           
#declare T_Road =texture {
	pigment {color rgb <0.45,0.4,0.35>}   
	normal {
		bumps 0.5
		scale 0.001
	}
}     


#declare T_Grey_Metal =texture {
	pigment {color rgb <0.4,0.5,0.6>}    
	finish {
		reflection 0.3    
		phong 1
	}
}         

#declare T_Silver_Metal =texture {
	pigment {color rgb <1.3,1.3,1.3>}    
	finish {
		reflection 0.4    
		phong 1
	}
} 


#declare T_Rail_Metal =texture {
	pigment {color rgb <0.6,0.7,0.8>}    
	finish {
		reflection 0.3    
		phong 1
	}
} 

#declare T_Sleeper_Wood =texture {
	pigment {color rgb <0.2,0.1,0.0>}    
}                                           


#declare P_Earth=pigment { 
	 color <0.4,0.3,0.2>
}   

#declare P_Dark_Earth=pigment { 
	 color <0.2,0.15,0.1>
}    

#declare N_Earth=normal {
	bumps 0.5  
	scale 0.04

}

#declare T_Earth=texture { 
	pigment {P_Earth}	
	normal {N_Earth}
}    

#declare T_Dark_Earth=texture { 
	pigment {P_Dark_Earth}	
	normal {N_Earth}
}


// Unit textures



#declare T_Tank_Tracks = texture {
	pigment {
	    color <0.1,0.1,0.1>
	}    	
	finish {  
		
		phong 1
	}   
}      

#declare T_Concrete = texture {
	pigment {
	    color <0.8,0.9,1.0>
	}    	
	normal {
		bumps 1    
		scale 0.1
	}  
	finish {
		specular 0.1
	}
}      

#declare T_Skin = texture {
	pigment {
	    color <1,0.8,0.6>
	}    	
}

                                           
                                           
///////////////////////////////////////////////////////////
// Steam Components                                           

// rivet ring in x-y plane, outer radius 1                                           
#declare Rivet_Ring = union {
	difference {
		cylinder {<0,0,-0.15>,<0,0,0.15> , 1}    
		cylinder {<0,0,-0.2>,<0,0,0.2> , 0.96}
	}
	Revolve(sphere {<1,0,0> , 0.05},z*22.5,16)   
	texture { T_Unit_Brass }
}           


// boiler of height approx 1.2, flat base at <0,0,0>
#declare Boiler = union {
	object {Rivet_Ring translate <0, 0, 0>}
    object {Rivet_Ring translate <0, 0, 1>}
    object {Rivet_Ring translate <0, 0, 2>}        
    union {
    	cylinder {<0,0,0>,<0,0,2.1> , 0.95    }
    	sphere {<0,0,2> , 0.9 }
    	texture { T_Unit_Metal } 
    }        
    scale 0.4
    
}                     


// Brass chimney of height 1, centred at origin
#declare Chimney=union {
	difference {
		cone {
			<0,0,0>,0.15,<0,0,1>,0.1 	
			texture {T_Unit_Brass}  
		}   
		cylinder {
			<0,0,-0.2>,<0,0,2>,0.09 	
			texture {T_Dark}  
		}    
        }
        
	scale <1,1,1>

}   

// Riveted global radius 1 at origin
#declare Rivet_Globe=union {
	sphere {<0,0,0>,1 texture {T_Unit_Metal}}
	object {Rivet_Ring scale 1 }
	object {Rivet_Ring rotate x*90 scale 1 }
	object {Rivet_Ring rotate y*90 scale 1 }
    

}      




//////////////////////////////////////////////////////////////
// Body Components and Shapes

// octagonally edged box from <-1,-1,-1> to <1,1,1>
#declare Octagonal_Box = intersection {
	box {<-1,-1,-1>,<1,1,1>}
	box {<-1,-1,-1>,<1,1,1> scale 1.2 rotate z*45}
}     

// octagonal block height 1 width 4 on origin
#declare Octagonal_Block = difference {
	box {<-2,-2,0>,<2,2,1>}    
	Revolve (plane {<-2,0,-1>,-1.5},z*45,8)
}





//////////////////////////////////////////////////////////////
// Good and miscellaneos objects


// goods box of unit height, on top of origin
#declare Goods_Box = object {
	box {<-0.5,-0.5,0>,<0.5,0.5,1>}
	texture {T_Light_Wood}
}
                    
// six goods boxes, ~ unit lengh in x-direction

#declare Goods_Boxes = union {
	object {Goods_Box scale 0.8 translate <-1,-0.5,0>}
	object {Goods_Box scale 0.8 translate < 0,-0.5,0>}
	object {Goods_Box scale 0.8 translate < 1,-0.5,0>}
	object {Goods_Box scale 0.8 translate <-1, 0.5,0>}
	object {Goods_Box scale 0.8 translate < 0, 0.5,0>}
	object {Goods_Box scale 0.8 translate < 1, 0.5,0>}   
	scale 0.34
}





///////////////////////////////////////////////////////////////
// Weapon components

#declare Gun_Barrel = union {
	// main barrel
	difference {
		cylinder {
			<0, 0, 0> <1, 0, 0> , 0.05
			texture { T_Unit_Brass }
		}     
		cylinder {
			<0, 0, 0> <2, 0, 0> , 0.04
			texture { T_Dark }
		
		}     
	}
	
	// rim
	difference {
		cylinder {<0.75, 0, 0> <0.95, 0, 0> , 0.07}
		cylinder {<0, 0, 0> <1, 0, 0> , 0.05} // main barrel
		cylinder {<0.9, 0.1,0>,<0.9,-0.1,0>,0.03}    
		cylinder {<0.8, 0.1,0>,<0.8,-0.1,0>,0.03}    
		texture { T_Unit_Brass }
	}
} 


#declare GUN_ELEVATION=10;


#declare Turret_Body = union {
	union {
		intersection {     
			sphere {<0, 0, 0> , 0.4}
			cylinder {<0, 0, -0.3> <0, 0, 0.3> , 0.5}         
		}
		texture { T_Unit_Metal scale 0.4}   
	}
	union {
		cylinder {
			<0, 0, -0.3> <0, 0, 0.4> , 0.2
			texture { T_Unit_Metal }   
		}  
		object {Chimney scale 0.4 translate <-0.2,-0.2,0>} 
	}
}         

#declare Gun_Turret = union {
	object {Gun_Barrel translate <0, 0.2, 0.1> rotate -GUN_ELEVATION*y}    
	object {Gun_Barrel translate <0, -0.2, 0.1> rotate -GUN_ELEVATION*y} 
	object {Turret_Body}    
}    

#declare Single_Gun_Turret = union {
	object {Gun_Barrel scale 1.5 translate <0, 0, 0.1> rotate -GUN_ELEVATION*y}    
	object {Turret_Body scale <1,0.8,1>}    
}     

#declare Double_Gun_Turret = union {
	object {Gun_Barrel scale 1.3 translate <0, 0.2, 0.1> rotate -GUN_ELEVATION*y}    
	object {Gun_Barrel scale 1.3 translate <0, -0.2, 0.1> rotate -GUN_ELEVATION*y}    
	object {Turret_Body scale <1.3,1.3,1>}    
}    

///////////////////////////////////////////////////
// Gatling gun


// gatling gun, length one in +x direction from origin
#macro Gatling (n) 
	union {
		Revolve {
			object {Gun_Barrel translate <0,0,0.15>},
			360/n*x,
			n			
		} 
		union {
			cylinder {<0,0,0>,<0.1,0,0>,0.25}
			cylinder {<0.5,0,0>,<0.6,0,0>,0.25}
			texture {T_Unit_Metal}
		}
 	}
#end
   
   
///////////////////////////////////////////////////
// Artillery   

#declare Artillery_Shell=union {
	cylinder {<0,0,0>,<0.4,0,0>,0.1
		texture {T_Unit_Brass}    
	
	}
	sphere {
		<0,0,0>,1 scale <0.2,0.08,0.08> translate <0.4,0,0>
		texture {T_Grey_Metal}
	}
	cone {
		<sqrt(2)/2,0,0>,sqrt(2)/2,<sqrt(2),0,0>,0 scale <0.2,0.08,0.08> translate <0.4,0,0>
		texture {T_Grey_Metal}
	}
	scale 0.15
}   


#declare Artillery_Gun=union {
	cylinder {<0,0,0>,<2.5,0,0>,0.5
		texture {T_Dark}    
	
	}  
	cylinder {<2.0,0,0>,<2.2,0,0>,0.55
		texture {T_Unit_Brass}    
	
	}   

	 
	cylinder {<-1.2,0,0.7>,<1,0,0.7>,0.3
		texture {T_Unit_Metal}    
	}   

	object {
		Boiler    
		scale 0.8
		rotate y*90
		translate <0.2,0,0.7>
	}

	object {
		Gun_Barrel 
		scale 4    
		translate <2,0,0>
	}
}     


#declare Artillery_Platform=object {
	union {
		difference {
			cylinder {<0,0,0>,<0,0,0.5>,1}
		        cylinder {<0,0,0.3>,<0,0,0.6>,0.8}
			texture {T_Unit_Metal} 
		}                 
		
		Series(
			union {
				cylinder {<0,-0.1,1.5>,<0,0.1,1.5>,0.5
					texture {T_Unit_Metal}    
				}    
				box {
					<-0.5,-0.1,0>,<0.5,0.1,1.5>
					texture {T_Unit_Metal}    
				}   
				translate <0,-0.5,0>
			},
			<0,1,0>,
			2       
		)
			
		cylinder {<0,-0.7,1.5>,<0,0.7,1.5>,0.2
			texture {T_Unit_Brass}    
		}  
		
		object {Rivet_Ring scale 1.05 translate <0,0,0.2>}
		
	} 
}           

// Artillery gun with centre of base at origin, at unit scale
#declare Artillery_Piece =union {
	object {Artillery_Platform scale 1.2}
	object {Artillery_Gun rotate y*-65 translate <0,0,1.5>}
	
	scale 0.08
}




///////////////////////////////////////////////////////////////
// Mechanical components


#declare Wheel=union {
    difference {
    	cylinder {<0, -0.3, 0> <0, 0.3, 0> , 0.95}   
    	cylinder {<0, -0.4, 0> <0, 0.4, 0> , 0.6}  
    }
    cylinder {<0, -0.25, 0> <0, 0.25, 0> , 0.5}  
    
    texture {T_Unit_Brass}
}  





///////////////////////////////////////////////////////////////
// Rail components

#declare RAIL_BASE_HEIGHT=0.12; // height of rail car base plate
#declare RAIL_TOP_HEIGHT=0.25; // height of rail car base plate

// rail wheel of height 0.1, resting on zero plane, rolling in x-direction
#declare Rail_Wheel=union {
    difference {
    	cylinder {<0,-Rail_Size, 0.05> <0,Rail_Size, 0.05> , 0.05}  
    	cylinder {<0,-Rail_Size, 0.05> <0,Rail_Size, 0.05> , 0.05 translate <0,0,-0.05> scale <0.8,1.2,0.8> translate <0,0,0.05>}  
    
    }   
    
    // spokes
    union {
	    Revolve (  
	    	cylinder {<0,0,-0.048>,<0,0,0.048>,Rail_Size*0.5},y*30,12
	    	
	    )      
    	translate z*0.05
    }
    
    texture {T_Unit_Brass}
} 

#declare Rail_Wheel_Set=union {
    object {Rail_Wheel translate <0,Rails_Width,0>}
    object {Rail_Wheel translate <0,-Rails_Width,0>} 

    cylinder {<0,-(Rails_Width+Rail_Size), 0.05> <0,+(Rails_Width+Rail_Size), 0.05> , 0.01
 	   texture {T_Unit_Brass}    
    }  
}     

#declare Cow_Catcher_Side=union {
	difference {
		box {<0,-1,0>,<3,0,1>} // block   
		Series (
			box {<-1,-0.9,0.2>,<4,-0.1,0.3>}, // slits 
			<0,0,0.2>,
			4
		)  
		box {<-0.03,-5,-5>,<0.03,5,5> rotate y*-45 rotate z*-45 translate <3,0,0> inverse}	// plate volume
		scale <0.4,0.8,1>
	} 
	box {<0,-0.2,0>,<1,0.2,0.1>} 
	texture {T_Unit_Brass}   	
}      

#declare Cow_Catcher=union {
	object {Cow_Catcher_Side}
	object {Cow_Catcher_Side scale <1,-1,1>}     
	scale Rail_Car_Width
} 

#declare Rail_Small_Wheel_Unit=union {
    #local WS=0.9; // Wheel scale
    union {
    	box {<-0.1,-(Rails_Width-Rail_Size),0.04>  , <0.1,+(Rails_Width-Rail_Size),0.08>}
    	box {<-0.07,-0.02,0.07>  , <0.07,0.02,0.095>}
 		texture {T_Dark}
    }
    object {Rail_Wheel_Set scale <WS,1,WS> translate <-0.08,0,0>}
    object {Rail_Wheel_Set scale <WS,1,WS> translate <0.08,0,0>}   
    
    translate <0,0,Rail_Height>  // elevate to rest on rails
}   

#declare Rail_Base=union {
	box {<-0.4,-Rail_Car_Width,0.1>, <0.4,Rail_Car_Width,0.12>}
    texture {T_Unit_Metal}
}        

#declare Rail_Car=union {
	box {<-0.12,-Rail_Car_Width,RAIL_BASE_HEIGHT>, <0.12,Rail_Car_Width,RAIL_TOP_HEIGHT>
    	texture {T_Unit_Metal}
	}  
}       

#declare Rail_Engine=union {
	box {<-0.12,-Rail_Car_Width,RAIL_BASE_HEIGHT>, <0.12,Rail_Car_Width,0.15> // base block
    	texture {T_Unit_Metal}
	}                         
	box {<-0.08,-Rails_Width,0.14>, <0.03,Rails_Width,0.25> // cabin
    	texture {T_Unit_Metal}
	}  
	object {Chimney scale 0.1 scale <1,1,0.5> translate <-0.05,0,RAIL_TOP_HEIGHT>}

	object {Boiler scale 0.12 rotate y*90 translate <0.03,0,0.16>}            
	
	object {Cow_Catcher translate <0.1,0,Rail_Height+0.01>}
}


#declare Rail_Gun_Block=union {
	box {<-0.12,-Rail_Car_Width,0.1>, <0.12,Rail_Car_Width,0.2>
    	texture {T_Unit_Metal}
	}  
	object {Gun_Turret scale 0.15 translate <-0.022,0,0.2>}

	object {Cow_Catcher translate <0.1,0,Rail_Height+0.01>}

}   


///////////////////////////////////////////////////////////////
// Tripod Components

// Tripod hood of approx radius 1

#declare Tripod_Hood=union {
	difference {
		sphere {<0,0,0>,1 scale <1,0.8,0.5>} // flattened hood top
		sphere {<1.5,0,0>,1 scale <1,1,1>} // flattened hood top   
		texture {T_Unit_Metal} 
	} 
	sphere {<0,0,0>,1 scale <1,0.8,0.5> scale 0.9 texture {T_Dark}} // blackened glass
	cone {<0.0,0,0.45>,0.6,<0.0,0,0.55>,0.5 scale <0.8,0.5,1> translate <0.3,0,0> texture {T_Unit_Brass} }       
	object {Rivet_Globe scale 0.3 translate <0.3,0,0.5>}

	object {Chimney scale <1.5,1,1> translate <-0.5,0,0>}
	//object {Chimney scale 0.7 translate <-0.5,-0.4,0>}
}  

// tripod joint centred at origin, radius 1
#declare Tripod_Joint = union {
	cylinder {<0,-0.3,0>,<0,0.3,0>,0.6 texture {T_Unit_Metal}}   
	difference {
		cylinder {<0,-0.2,0>,<0,0.2,0>,1 }   
		cylinder {<0,-0.3,0>,<0,0.3,0>,0.8 }   
	
		texture {T_Unit_Brass}
	}
}

// Tripod leg of shoulder height 1, facing in negative x direction and centred above origin
#declare Tripod_Leg =union {
	cone {<0,0,0>,0.2,<0,0,0.1>,0.15 scale <1,0.8,1> translate <-1,0,0>  texture {T_Unit_Metal} } // foot
	cone {<-1,0,0.05>,0.06,<-0.85,0,1.3>,0.1 texture {T_Unit_Metal} }  // lower leg                   
	object {Tripod_Joint scale 0.15 translate <-0.85,0,1.3>}
	cone {<-0.85,0,1.3>,0.1,<-0.4,0,1>,0.12 texture {T_Unit_Metal} }  // upper leg
	object {Tripod_Joint scale 0.15 translate <-0.4,0,1>}
	
}



   