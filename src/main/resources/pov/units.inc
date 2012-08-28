
 


        
// Note: all units scaled to hex size, centre of base at <0,0,0>, facing in positive x direction


///////////////////////////////////////////////////////////////////////////////
// Steam Tank

#declare Tank_Body = union {
    union {
	    object {Octagonal_Box scale <-1,-0.5,0.15> translate <0,0,0.35>}
	    object {Octagonal_Box scale <-0.8,-0.4,0.2> translate <0,0,0.35>}
 	   texture { T_Unit_Metal scale 0.4}
    }
                                                      
    box {<-0.8,-0.3,0.1> <0.8,0.3,0.3> texture {T_Dark}}
    
}   

#declare Track_Part = union {
	box {<-0.1,-0.4,-0.05> <0.1,0.4,0.05>}    
	
	box {<-0.2,-0.3,-0.05> <0.2,-0.2,0.0>}    
	box {<-0.2, 0.3,-0.05> <0.2, 0.2,0.0>}  
		
	translate <0,0,1>
} 

#declare Half_Track=union {
    object {Revolve(Track_Part,y*15,24) translate <-2, 0, 0> }  
    object {Series(Track_Part,<0.25,0,0>,16) translate <-2, 0, 0> }   
    texture {T_Tank_Tracks}
}       



#declare Track = union {
	// track elements
	object {Half_Track}   
	object {Half_Track rotate y*180}  
	
	// main wheels
	object {Wheel translate <-2, 0, 0>}     
	object {Wheel translate <2, 0, 0>}     
	
	// mini wheels
	object {Series (Wheel,<-3, 0 ,0>,3) scale <0.25,1,0.25> translate <0.75, 0, 0.75>}    
	object {Series (Wheel,<-3, 0 ,0>,3) scale <0.25,1,0.25> translate <0.75, 0, -0.75>} 
}      

#declare Steam_Tank = union {
	object {Gun_Turret translate <0.2, 0, 0.6> }    
	object {Tank_Body }         
	object {Boiler translate <-0.7, 0, 0.5> }    
	
	object {Track scale 0.3 translate <0, -0.6,0.3>} 
	object {Track scale 0.3 translate <0,  0.6,0.3>} 
	
	scale 0.2	     
}        

//////////////////////////////////////////////////////////////////////////
// Battle Tank

#declare Battle_Tank = union {
	object {Single_Gun_Turret translate <0, 0, 0.5> }    
	object {Tank_Body}         
	
	object {Boiler scale 0.5 translate <-0.7, -0, 0.5> }    
	object {Chimney scale 0.1 translate <-0.7, -0.3, 0.5> }    
	object {Chimney scale 0.3 translate <-0.7, 0.3, 0.5> }    

	
	object {Track scale 0.3 translate <0, -0.6,0.3>} 
	object {Track scale 0.3 translate <0,  0.6,0.3>} 
	
	scale 0.22	     
}      

//////////////////////////////////////////////////////////////////////////
// Heavy Tank

#declare Heavy_Tank = union {
//	object {Single_Gun_Turret translate <0.1, -0.2, 0.5> }    
//	object {Single_Gun_Turret translate <0.1, 0.2, 0.5> }    
	object {Double_Gun_Turret translate <0.2, 0, 0.5> }
	
	object {Boiler scale 0.5 translate <-0.6, -0.3, 0.5> }    
	object {Chimney scale 0.8 translate <-0.8, 0, 0.5> }    
	object {Boiler scale 0.5 translate <-0.6, 0.3, 0.5> }    

	union {
		object {Tank_Body }         
		object {Track scale 0.3 translate <0, -0.6,0.3>} 
		object {Track scale 0.3 translate <0,  0.6,0.3>}
		scale <1,1.1,1> 
	}
	
	scale 0.24	     
} 


////////////////////////////////////////////////////////////////////////
// Artillery tank 


#declare Artillery_Tank = union {
	object {Tank_Body }         
	
	object {Track scale 0.3 translate <0, -0.6,0.3>} 
	object {Track scale 0.3 translate <0,  0.6,0.3>} 
	
	object {Artillery_Piece rotate z*-15 scale 4.5 translate <-0.3,0,0.5>}

	object {Boiler scale 0.6 rotate y*90 translate <0.4, 0, 0.6> }    

	
	scale 0.2	     
} 

////////////////////////////////////////////////////////////////////////
// Artillery

#declare Artillery = union {

	object {Artillery_Piece  translate <0,0,0.02>}
    
    union {
	    Revolve(
			object {box {<-0.05,-0.02,0>, <0.3,0.02,0.02>} texture {T_Unit_Metal} rotate z*-30},
			z*60,	     
	        2
	    )  
		scale 1.2
		translate <-0.1,0,0>
	}   
	
	// overall translation
	translate <-0.05,0,0>
	
}  
      


///////////////////////////////////////////////////////////////////////////////
// Construction Crawler

#declare Claw=difference {  
	cylinder {<-0.1,0,0>,<0.1,0,0>,1  } 
	cylinder {<-1,0,0>,<1,0,-0.7>,0.9}
	plane {<0,0,1>,-0.8}
        texture {T_Dark} 
		
}

#declare Construction_Claw=union {  
	cylinder {<0,-0.1,0>,<0,0.1,0>,0.5 texture {T_Unit_Metal} } 
	cylinder {<0,0,0>,<0,0,-0.7>,0.1 texture {T_Unit_Metal} }
	cylinder {<0,0,-0.5>,<0,0,-1>,0.4 texture {T_Unit_Metal} }
	object {Rivet_Ring scale 0.45 translate <0,0,-0.7>}  
	object {Claw rotate z*45 translate <0,0,-2>}   
	object {Claw rotate z*-45 translate <0,0,-2>}		
}

// construction crane base, radius 1, on <0,0,0>          
#declare Construction_Crane_Base=object {
	union {
		difference {
			cylinder {<0,0,0>,<0,0,0.5>,1}
		        cylinder {<0,0,0.3>,<0,0,0.6>,0.8}
			texture {T_Unit_Metal} 
		}                 
		
		cylinder {<0,-0.2,1.5>,<0,0.2,1.5>,0.5
			texture {T_Unit_Metal}    
		}    
		box {
			<-0.5,-0.2,0>,<0.5,0.2,1.5>
			texture {T_Unit_Metal}    
		}    
		cylinder {<0,-0.5,1.5>,<0,0.5,1.5>,0.2
			texture {T_Unit_Brass}    
		}  
		
		object {Rivet_Ring scale 1.05 translate <0,0,0.2>}
		
	}
}           

#declare Construction_Crane_Arm1=union {
	union {
		cylinder {<0,-0.4,0>,<0,0.4,0>,0.4}  
		union {
			box {<-0.4,-0.4,0>,<0.4,-0.3,2>}     
			box {<-0.4,0.4,0>,<0.4,0.3,2>}       
		}
		cylinder {<0,-0.4,2>,<0,0.4,2>,0.4}       
		texture {T_Unit_Metal}    
	}
	cylinder {<0,-0.5,2>,<0,0.5,2>,0.2
		texture {T_Unit_Brass}    
	}       
}          

#declare Construction_Crane_Arm2=union {
	union {
		cylinder {<0,-0.4,0>,<0,0.4,0>,0.4}  
		union {
			box {<-0.4,-0.25,0>,<0.4,-0.15,3>}     
			box {<-0.4,0.25,0>,<0.4,0.15,3>}       
		}
		cylinder {<0,-0.25,3>,<0,0.25,3>,0.3}       
		texture {T_Unit_Metal}    
	}
	cylinder {<0,-0.3,3>,<0,0.3,3>,0.15
		texture {T_Unit_Brass}    
	}    
	object {
		Construction_Claw
		rotate y*-80
		translate <0,0,3>
	}
}


#declare Construction_Crane=union {
	object {Construction_Crane_Base}
	union {
		object {Construction_Crane_Arm2 rotate y*115 translate <0,0,2>}  
		object {Construction_Crane_Arm1 }
		  
		rotate y*-45 translate <0,0,1.5> 
	}
}

            
#declare Construction_Crawler = union {
	object {Tank_Body }         
	object {Boiler scale 0.7 rotate y*-10 translate <-0.6, 0.2, 0.6> }  
	
	object {Chimney  scale 1.2  translate <-0.7, -0.2, 0.6>}      

	object {Construction_Crane  scale 0.35  translate <0.3, 0, 0.5>}      

	
	object {Track scale 0.3 translate <0, -0.6,0.3>} 
	object {Track scale 0.3 translate <0,  0.6,0.3>} 
	
	translate <-0.2,0,0>
	
	scale 0.25	     
}                


//////////////////////////////////////////////////////////////////////////////////////
// Mine

// spike pointing up one unit in z-direction
#declare Spike=union {
	cone {<0,0,0>,0.25,<0,0,1>,0
		texture {T_Unit_Metal}
	}
	object {Rivet_Ring scale 0.25 }

}

#declare Mine=union {
	sphere {<0,0,0>,1 texture {T_Unit_Metal}}
	Revolve (
		Revolve (
			object {Spike scale 1 translate z*1},
			y*60,
			6
		),  
		z*60,
		6
	) 
	translate <0,0,1>
	scale 0.12
}               

#declare Target_Globe=object {
	Rivet_Globe
    
    rotate z*30
    
	translate <0,0,1>
	scale 0.15
}      

#declare Target_Boiler=union {
	object {Boiler }

	scale 0.35
}

////////////////////////////////////////////////////////////////////////////////////////
// Minelayer

#declare Minelayer_Tank=union {
	object {Tank_Body }         
	object {Boiler scale 0.6 translate <0, 0, 0.5> }    

	object {Rivet_Globe scale 0.3 translate <0.7, 0, 0.5> }    

	
	object {Track scale 0.2 translate <0, -0.6,0.3>} 
	object {Track scale 0.2 translate <0,  0.6,0.3>} 
	
	object {Cow_Catcher scale <4,12,6> translate <0.85,0,0.1>}
	
	object {Construction_Crane_Base scale 0.4 translate <-0.6, 0, 0.5> }    

	union {	
		union {
			box {<-1,-1.1,-0.1>,<1,-0.9,0.1>}
			box {<-1,0.9,-0.1>,<1,1.1,0.1>}   
			texture {T_Unit_Metal}
			scale <0.9,0.15,1.3>
		}	   
		object {Mine scale 2 translate <-0.5,0,0.1>}
		object {Mine scale 2 translate <0.2,0,0.1>}
		rotate y*-30
		translate <-1.3,0,0.7>
	} 
	
	translate <0.3,0,0>
	
	scale 0.2	    
}

//////////////////////////////////////////////////////////////////////////////////////
// Fortress Turret


#declare Base_Block = union {
	#local BASE_BLOCK_EDGE=0.25;
	Revolve (box {<0.5,0.5,0>,<0.5-BASE_BLOCK_EDGE,0.5-BASE_BLOCK_EDGE,1>},90*z,4)            
	box {<-0.4,-0.4,0>,<0.4,0.4,0.8>}     
	texture {
		T_Concrete
	}
}

#declare Fortress_Turret_Gun = union {
	object {Gun_Turret scale 0.7 translate <0, 0, 0.75> }    
	
	scale 0.4	     
}                    

#declare Fortress_Turret_Base = union {
	object {Base_Block rotate 30*z scale <1,1,0.6>}
	
	scale 0.4	     
}   

//////////////////////////////////////////////////////////////////////////////////////
// Pillbox


#declare PillBox_Block = difference {  
	object {Octagonal_Block}
	Revolve (box {<-0.4,-2,0.6>,<0.4,2,0.8>},z*45,8)
	texture {T_Concrete}         
	scale 0.1
}                                    

#declare PillBox_Gun = union {
	object {Gun_Turret}   
	scale 0.2
	translate <0,0,0.15> 
}    

//////////////////////////////////////////////////////////////////////////////////////
// Bunker

#declare Bunker = union {
	difference {
		object {PillBox_Block scale 2}
		box {<-0.05,2,0.01>,<0.05,0,0.1> texture {T_Dark}} // door
	}   
	object {Chimney scale 0.2 translate <0.1,0.1,0.2>}
	object {Boiler scale 0.15 translate <-0.1,-0.1,0.2>}
}                                    

 
//////////////////////////////////////////////////////////////////////////////////////
// Troopers

#declare Head =union {
	object {
		sphere {<0,0,12>, 2}
		texture {T_Unit_Metal}
	}    
	object {
		sphere {<1,0,11>, 2}
		texture {T_Skin}
	}          
	
}        

#declare Leg =difference {
	union {
		object {
			sphere {<0,0,0>, 1 scale <2,1,1> translate <0,-2,0>}
			texture {T_Unit_Metal}
		}    
		object {
			sphere {<0,0,0>, 1 scale <1,0,4> translate <0,-2,3> rotate -x*20}
			texture {T_Unit_Metal}
		}  
	}        
	plane {<0,0,1>,0}
}      



#declare Trooper = union {
	object {Head}   
	object {
		sphere {<0,0,4>,3 }      
		scale <0.5,1,2>
		texture {T_Unit_Metal}
	}     
	union {
		sphere {<0.0,  2.0, 10.0>,1.5}    
		sphere {<0.0,  3.0,  8.5>,1}  
		sphere {<1.5,  3.0,  8.0>,1}         
		sphere {<3.0,  3.0,  7.5>,1}  
		texture {T_Unit_Metal}
	}    
	object {Leg}       
	object {Leg scale <1,-1,1>}
	object {
		Gun_Barrel
		scale 8.0
		translate <-1.0,  2.0,  8.5>
	}
	
	scale 0.02  
}          


#declare Rifles = union {
//	object {Trooper translate <0.15, 0,0>  }     
//	object {Trooper translate <-0.05, 0.25, 0> }
//	object {Trooper translate <-0.05, -0.25, 0>}
	object {Trooper translate <0, 0,0>  }      
	object {Trooper translate <0, 0.2, 0> }
	object {Trooper translate <0, -0.2, 0>}
}        

#declare Militia = union {
//	object {Trooper translate <0.15, 0,0>  }     
//	object {Trooper translate <-0.05, 0.25, 0> }
//	object {Trooper translate <-0.05, -0.25, 0>}
	object {Trooper translate <0.1, 0,0>  }      
	object {Trooper translate <-0.1, 0.25, 0> }
	object {Trooper translate <-0.1, -0.25, 0>}
}  

//////////////////////////////////////////////////////////////////////////////////////
// Paddle Cruiser    

#declare Tower=union {
	box {<-1,-1,-1>,<1,1,0> scale <0.6,0.4,1>}  
	object {Gun_Turret}

	texture {T_Unit_Metal}   
	scale 0.1
}          

#declare Bridge=union {
	box {<-1,-1,-3>,<1,1,0> }  
	box {<-1,-0.8,0>,<0.8,0.8,1.5>}

	texture {T_Unit_Metal}   
	scale 0.05
}      

#declare Paddle_Blade =union {
	difference {
		cylinder {<0,-1,-0.9>,<0,1,-0.9>,1} 	
		cylinder {<0,-2,-0.9>,<0,2,-0.9>,0.95} 	
		plane {<0,0,1>,0} 
		translate <0.5,0,0>
		texture {T_Dark}  
	}	
}

#declare Paddle =union {
	difference {
		cylinder {<0,-1,0>,<0,1,0>,1} 	
		cylinder {<0,-2,0>,<0,2,0>,0.9} 	
		plane {<0,0,1>,0.2} 	

		texture {T_Unit_Metal}       
	}  
	
	object {
		Revolve (Paddle_Blade,y*30,12)
	}   
	
	object {
		cylinder {<0,-1.1,0>,<0,1.1,0>,0.1} 
		texture {T_Unit_Brass}  		
	}
	
	scale <0.1,0.07,0.1>
}    


#declare Funnel=union {
	box {
		<-1,-1,-1>,<1,1,0> scale <0.6,0.4,1>
		texture {T_Unit_Metal}  
	}  
	difference {
		cylinder {
			<0,0,-0.1>,<0,0,0.8>,0.2 	
			texture {T_Unit_Brass}  
		}   
		cylinder {
			<0,0,-0.2>,<0,0,1>,0.18 	
			texture {T_Dark}  
		}    
		scale <1.5,1,1>
		rotate y*-10
        
        }
        
 
	scale 0.15
}  

   
                   
#declare Ship_Hull_Shape = 	intersection {
	sphere {<0,1,0.4>,1.2}
	sphere {<0,-1,0.4>,1.2}      
	plane {<-0.1,0,1>,0.3}
}                 

#declare Barge_Rounded_Zone = merge {
	sphere {<-1,0,0>,1.5}
	sphere {<1,0,0>,1.5}
	cylinder {<-1,0,0>,<1,0,0>,1.5}
}

#declare Barge_Hull_Shape = intersection {       
	object {Barge_Rounded_Zone translate <0,-1,0.5>}
	object {Barge_Rounded_Zone translate <0,1,0.5>}
	plane {<-0.1,0,1>,0.5}  
	scale 0.25   
}          
                   
#declare Ship_Hull=difference {
	union {  
		difference {
			object {Ship_Hull_Shape }        
			object {Ship_Hull_Shape translate <0,0,0.15> }   
			texture {T_Unit_Metal scale 0.1}
		}       
		
		intersection {
			object {Ship_Hull_Shape scale 0.99}        
			object {Ship_Hull_Shape translate <0,0,-0.02> }   
		    texture {T_Wooden_Deck}
	 	}              
	}
	
	plane {<0,0,1>,0}
	
}   

#declare Barge_Hull=difference {
	union {  
		difference {
			object {Barge_Hull_Shape }        
			object {Barge_Hull_Shape translate <0,0,0.1> }   
			texture {T_Unit_Metal scale 0.1}
		}       
		
		intersection {
			object {Barge_Hull_Shape scale 0.99}        
			object {Barge_Hull_Shape translate <0,0,-0.05> }   
		    texture {T_Wooden_Deck}
	 	}              
	}
	
	plane {<0,0,1>,0}
	
}   

#declare Paddle_Cruiser=union {
	object {Ship_Hull scale <0.6,0.5,0.5>}     
	object {Tower translate <0.23,0,0.2>}   
	object {Funnel translate <-0.1,0,0.2>}      
	object {Bridge translate <0,0,0.3>}       
	
	object {Boiler scale 0.07 translate <-0.07, 0.07, 0.2> }    
	object {Boiler scale 0.07 translate <-0.07, -0.07, 0.2> }      
	
	object {Paddle translate <0, 0.08, 0.07> }    
	object {Paddle translate <0, -0.08, 0.07> }   

	
	object {Tower translate <0.11,0,0.25>}    
	object {Tower rotate z*180 translate <-0.22,0,0.15>}
}        

/////////////////////////////////////////////////////////////////////
// Patrol Boat

#declare Patrol_Boat=union {       
	difference {
		object {Ship_Hull scale <0.5,0.5,0.3>}        
		box {<-1,-1,-1>,<-0.2,1,1> texture {T_Unit_Metal}}	
	}
	object {Tower translate <0.13,0,0.19>}   
	object {Funnel scale 0.7 translate <-0.1,0,0.2>}      
	object {Bridge translate <0,0,0.2>}       
	
	object {Boiler scale 0.07 translate <-0.07, 0.05, 0.15> }    
	object {Boiler scale 0.07 translate <-0.07, -0.05, 0.15> }      
}      

/////////////////////////////////////////////////////////////////////
// Transport Barge

#declare Basic_Barge=union {
	difference {
		object {Barge_Hull scale <0.8,0.8,0.5>}        
		box {<-1,-1,-1>,<-0.3,1,1> texture {T_Unit_Metal}}	
	}
	object {Bridge scale <1.3,1.3,0.8> translate <-0.15,0,0.1>}       
	object {Boiler scale 0.05 translate <-0.27, 0.05, 0.05> }    
	object {Boiler scale 0.05 translate <-0.27, -0.05, 0.05> }      
	object {Funnel scale 0.7 translate <-0.25,0,0.1>}      
}

#declare Transport_Barge=union {       
    object {Basic_Barge}
	object {Goods_Boxes scale 0.2 translate <0.05,0,0.04>}      	
}     

#declare Artillery_Barge=union {       
    object {Basic_Barge}                                   
	object {Artillery_Piece scale 0.6 rotate z*25 translate <0.1,0,0.06>}	
	object {Goods_Boxes scale 0.1 translate <0.3,0,0.04>}      	
}     


///////////////////////////////////////////////////////////////////////
// Assault Zeppelin 

#declare Airship_Balloon=union {
	object {
		Revolve (object {sphere {<0,0,0>,1} scale <1,0.3,0.5>},x*30,12)	       
		texture {T_Airship_Balloon} 

	}
	
	difference {
		union {
			sphere {<-1,0,0>,0.3 scale <1,0.2,2>}
			sphere {<-1,0,0>,0.3 scale <1,2,0.2>}
		}   
		box {<-2,-1,-1>,<-1.15,1,1>}   
		rotate x*45
		texture {T_Unit_Colour}
	}
	
	scale 0.2
}     

#declare Airship_Gun=union {
	object {Gun_Turret scale 0.1}
}    

#declare Airship_Harness=union {
	torus {1,0.05} 
	rotate z*90  
	scale <0.2,0.1,0.1>    
	texture {T_Unit_Metal}
}       

#declare Airship_Hull=union {
	object {Ship_Hull scale <0.33,0.4,0.2>}
}


#declare Assault_Zeppelin=union {
	object {Airship_Balloon translate <0,0,0.45>}  
	object {Airship_Harness scale 1 translate <0,0,0.45>}    
	object {Airship_Harness scale 0.9 translate <0.1,0,0.45>}    
	object {Airship_Harness scale 0.9  translate <-0.1,0,0.45>}  
	
	object {Airship_Gun rotate x*90  translate <0,-0.1,0.4>}  
	object {Airship_Gun rotate x*-90  translate <0,0.1,0.4>}  
	    
	object {Airship_Hull translate <0,0,0.3>}   
	
	translate <0,0,-0.05>

}     

///////////////////////////////////////////////////////////////////////
// Death ray balloon

#declare DRB_Balloon=union {
	object {
		Revolve (
			union {
				sphere {<0,0,0>,1 scale <1,0.7,1> texture {T_Airship_Balloon}} 
				sphere {<0,0,0>,1 scale <1,0.7,1> rotate z*30 texture {T_Unit_Colour}} 
			},z*60,6)	       
		 

	}      
	
	object {Airship_Harness rotate y*90 scale 9.5 translate <0,0,0.2>}    
	
	scale 0.2
}       

#declare DRB_Basket=union {
	object {
		cylinder {<0,0,-1>,<0,0,1>,1 texture {T_Unit_Colour}} 

	}      
	
	Revolve (
		object {
			cylinder {<-1.5,0,-0.8>,<1.5,0,-0.8>,0.1}    
			texture {T_Dark}
			rotate z*45
		},z*90,2)
	
	
	scale 0.07
}   

#declare Death_Ray_Balloon=union {
	object {DRB_Balloon   translate <0,0,0.3>}   
	object {DRB_Basket   translate <0,0,0.0>}        
	
	translate <0,0,0.2>
}    

///////////////////////////////////////////////////////////////////////
// Observation balloon

#declare Observation_Balloon=union {
	object {
		Revolve (
			union {
				sphere {<0,0,0>,1 scale <1,0.8,1> texture {T_Airship_Balloon}} 
				sphere {<0,0,0>,1 scale <0.93,0.6,1> rotate z*45 texture {T_Unit_Colour}}  
				rotate z*30
			},z*90,4)	       
		 

	}      
	
	object {
		cylinder {<0,0,-2>,<0,0,-1>,0.2 texture {T_Unit_Colour}} 

	}      
	
	scale 0.18    
	
	translate <0,0,0.55>

}   

/////////////////////////////////////////////////////////////////////////
//  Tripods and walkers

#declare Tripod = union {
	object {Tripod_Leg}
	object {Tripod_Leg rotate z*110}
	object {Tripod_Leg rotate z*-110}      
	union {
		object {Tripod_Hood scale 0.5 }
		object {Gun_Turret rotate x*180 scale 0.6 translate <0.1,0,-0.1>}       
		scale 0.9
		translate <0,0,1.2>
	}
    scale 0.25
}        

#declare Heavy_Walker = union {
	object {Tripod_Leg translate <0.2,0,0> rotate z*45}
	object {Tripod_Leg translate <0.2,0,0> rotate z*-45}      
	object {Tripod_Leg translate <0.2,0,0> rotate z*120}
	object {Tripod_Leg translate <0.2,0,0> rotate z*-120}      
	union {
		object {Tripod_Hood scale 0.5 }  
		
		object {Artillery_Piece rotate z*25 scale 2 translate <-0.25,0,0.15>}
		
		object {Gun_Turret rotate x*180 scale 0.7 translate <0.1,0,-0.1>}       
		scale 0.9
		translate <0,0,1.2>
	}
    scale 0.33
}  

///////////////////////////////////////////////////////////////////////
// Armoured Train


#declare Armoured_Train_Centre = union {
	object {Rail_Car scale <1.2,1.2,1>} 
	object {Chimney scale 0.2 translate <-0.02,0,0.25>}
	object {Boiler scale 0.1 rotate y*90 translate <0.0,0,0.28>}

}

#declare Armoured_Train=union {
	object {Rail_Base}	
	object {Rail_Small_Wheel_Unit scale 1/UNIT_SCALE translate <0.25,0,0>}	
	object {Rail_Small_Wheel_Unit scale 1/UNIT_SCALE translate <-0.25,0,0>}	
		
	object {Rail_Gun_Block translate <0.26,0,0>}	
	object {Rail_Gun_Block translate <0.26,0,0> rotate z*180}	
	object {Armoured_Train_Centre}
}    

////////////////////////////////////////////////////////////////////////
// Transport Train

#declare Transport_Train=union {
	object {Rail_Base}	
	object {Rail_Small_Wheel_Unit scale 1/UNIT_SCALE translate <0.25,0,0>}	
	object {Rail_Small_Wheel_Unit scale 1/UNIT_SCALE translate <-0.25,0,0>}	
		
	object {Rail_Car translate <-0.26,0,0>}	
	//object {Goods_Boxes scale 0.25 translate <-0.26,0,0.23>}
	object {Rail_Car }	  
	object {Goods_Boxes scale 0.29 translate <0,0,0.22>}
	object {Rail_Engine translate <0.26,0,0>}	
}     




///////////////////////////////////////////////////////////////////////////
// Artillery Train



#declare Artillery_Train=union {    
	union {
		object {Artillery_Shell rotate z*90 translate <-0.00,0,0.02>}
		object {Artillery_Shell rotate z*90 translate <-0.04,0,0.02>}
		object {Artillery_Shell rotate z*90 translate <-0.08,0,0.02>}
		object {Artillery_Shell rotate z*90 translate <-0.02,0,0.04>}
		object {Artillery_Shell rotate z*90 translate <-0.06,0,0.04>}
		object {Artillery_Shell rotate z*90 translate <-0.04,0,0.06>}  
		translate <-0.26,-0.04,RAIL_BASE_HEIGHT>
    }
	
	object {Rail_Base}	
	object {Rail_Small_Wheel_Unit scale 1/UNIT_SCALE translate <0.25,0,0>}	
	object {Rail_Small_Wheel_Unit scale 1/UNIT_SCALE translate <-0.25,0,0>}	

	object {Artillery_Piece rotate z*15 translate <-0.1,0,RAIL_BASE_HEIGHT>}	

		
	object {Rail_Engine translate <0.26,0,0>}	
}    