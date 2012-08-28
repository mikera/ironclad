



//////////////////////////////////////////////
// Transform for tiling


#declare Tiled_Transform = transform {
	rotate z*45   
	scale <RATIO/sqrt(2), 0.5/sqrt(2),1>
}           

/////////////////////////////////////////////
// Functions

#declare Zero_Function =function {
	0
}  


#declare Hill_Height=function {
	#local NS=2.5;
	(f_noise3d(x*NS+3,y*NS,0)-0.4)*1 * max(0,4*(0.25-(x*x+y*y)))
} 

#declare Mountain_Height=function {
	#local NS=1.8;
	(f_noise3d(x*NS+11,y*NS+2,0) + 0.6*f_noise3d(5*x*NS+100,5*y*NS,0)-0.5) * max(0,4*(0.25-(x*x+y*y)))
}

//////////////////////////////////////////////
// Textures    

#declare P_Grass=pigment {color <0.3,0.35,0.1>}

#declare P_Sand=pigment {color <1.1,1,0.4>}


#declare T_White= texture {   
	pigment {
	    color <1.0,1.0,1.0>
	}    	
	finish {
		phong 1
	}
}	          

#declare T_Grass= texture {   
	pigment {
	   P_Grass
	}    	
	finish {
		phong 0.1
	}   
	normal {
		bumps 0.3  
		scale 0.04
	}
}	    

#declare T_Sand= texture {   
	pigment {
	   P_Sand
	}    	
	finish {
		phong 0.1
	}   
	normal {
		bumps 0.3  
		scale 0.02
	}
}	   

#declare T_Hills= texture {   
	pigment {
	    function {max(0,z)}
	    pigment_map {
	    	[0 P_Grass]
	    	[0.3 color <0.5,0.3,0.1>]
	    	[0.5 color <0.6,0.4,0.3>]
	    	[0.8 color <0.7,0.6,0.6>]
	    	[1 P_Grass]
	    }
	}    	
	finish {
	}   
	normal {
		bumps 0.5  
		scale 0.04
	}
}	    

#declare T_Mountains= texture {   
	pigment {
	    function {max(0,z)}
	    pigment_map {
	    	[0 P_Grass]
	    	[0.1 color <0.5,0.4,0.3>]
	    	[0.3 color <0.6,0.7,0.8>]
	    	[0.43 color <0.6,0.7,0.8>]   
	    	[0.45 color <1.3,1.3,1.3>]   
	    	[0.7 color <1.6,1.6,1.6>]
	    }
	}    	
	finish {
	}   
	normal {
		bumps 0.5  
		scale 0.04
	}
}	

#declare T_Woods= texture {   
	pigment {
	    color <0.1,0.2,0.05>
	}    	
	finish {
		phong 0.1
	}   
	normal {
		bumps 0.5  
		scale 0.04
	}
}	  





        
// grid pattern scaled to tile on hexes.        
#declare T_Grid = texture {
	pigment { checker <0,0,0>, <1,1,1> }
	transform {Tiled_Transform}
}


#declare T_Sea= texture {   
	pigment {
	    color <0.0,0.3,0.4>
	}    	
	finish {
		phong 0.05  
		//reflection 0.5
	}   
	normal {
		bumps 0.4  
		scale 0.15  
		transform {Tiled_Transform}
	}

}	            

#declare T_Deep_Sea= texture {   
	pigment {
	    color <0.0,0.2,0.3>
	}    	
	finish {
		//reflection 0.5
	}   
	normal {
		bumps 0.4  
		scale 0.15  
		transform {Tiled_Transform}
	}

}	   

#declare T_Rock= texture {   
	pigment {
	    color <0.5,0.6,0.7>
	}    	
	finish {
	}   
	normal {
		bumps 0.6  
		scale 0.05  
		transform {Tiled_Transform}
	}
}	   


//////////////////////////////////////////////
// Objects    


#declare Grey_Rock=isosurface {    
	#local NS=5.0;
//	function {x*x+y*y+z*z-0.5 + 0.2*f_ridged_mf(x*3,y*3,z*3, 0.5, 1, 5, 1, 0, 1) }   
	function { f_sphere(x, y, z, 0.5) - f_noise3d(x*NS,y*NS,z*NS)*0.2 }
	max_gradient 2
	contained_by {sphere {<0,0,0>,1}}    
	texture {T_Rock}
}         

#macro Scatter_Rock_Field(n,heightfunc) 
	union {
		#local R1 = seed(12322+n);
		#local i=n;
		#while (i>0)   
			#local a=rand(R1);
			#local b=rand(R1);      
			#local sscale=(0.5+rand(R1))*0.08;
			object {
				Grey_Rock   
				
				scale <0.5+rand(R1),0.5+rand(R1),2+rand(R1)>  
				rotate (z*rand(R1)*360)
	
				scale sscale  
				translate <-a*0.5/RATIO+b*0.25/RATIO,b*0.5,0> 
				rotate floor(rand(R1)*3)*120*z	
				
				//translate <0,0,heightfunc(1,1,1)>		 
			}  
			
			#declare i=i-1;
		#end
	}   
#end

#declare Hex = intersection {
	cylinder {
		<0,0,-1>,
		<0,0,1>,
		1
	} 
	box { <-1, -0.5, -1.1>, <1, 0.5, 1.1> } 
	box { <-1, -0.5, -1.1>, <1, 0.5, 1.1> rotate z*60}                     
	box { <-1, -0.5, -1.1>, <1, 0.5, 1.1> rotate z*-60}   
	plane {<0,0,-1>, -HEX_BOTTOM} 
	scale HEX_SCALE
} 


#declare White_Hex = object {
	Hex
	texture {T_White}   
	texture {T_Grid}
}   



#declare Tree=object {
	sphere {<0,0,1>,1}
	scale <0.1,0.1,0.15>    
	texture {T_Woods}
}        
  
#macro Scatter_Trees(n)
	union {
		#local R1 = seed(12345);
		#local i=n;
		#while (i>0)   
			#local a=rand(R1);
			#local b=rand(R1);      
			#local treeScale=(0.5+rand(R1))*0.35;
			object {
				Tree
	
				scale treeScale  
				translate <-a*0.5/RATIO+b*0.25/RATIO,b*0.5,0> 
				rotate floor(rand(R1)*3)*120*z
	
				texture {
					pigment {
					    color <0.05+0.1*rand(R1),0.2+0.1*rand(R1),0.1*rand(R1)>
					}    	  
					normal {
						bumps 2  
						scale 0.005
					}		
				}
				 
			}  		
			#declare i=i-1;
		#end		
	}
#end   


///////////////////////////////////////////////////////////////
// Hills and mountains


#declare Hill_Area=intersection {   
	object {Hex}
	isosurface {    
		function {z-Hill_Height(x,y,z)}
		max_gradient 2
		contained_by {sphere {<0,0,0>,1}}         
	}    
}     

#declare Hills = union {
	intersection {
		plane {<0,0,1> 0}
		Hex
	}                    
	
	object {Hill_Area}
	
	texture {T_Hills}     
} 


#declare Rocky_Hills =union {
	object {Hills}
	Scatter_Rock_Field(17,Hill_Height)
}          

#declare Wooded_Hills =union {
	object {Hills}
	object {
		Scatter_Rock_Field(12,Hill_Height)
		scale <1,1,0.8>
	}
	Scatter_Trees(19)
}
  

#declare Mountain_Area=intersection {   
	object {Hex}
	isosurface {    
		function {z-Mountain_Height(x,y,z)}
		max_gradient 4
		contained_by {sphere {<0,0,0>,1}}         
	}    
}           

#declare Mountains = union {
	union {
		intersection {
			plane {<0,0,1> 0}
			Hex
		}       
	
		object {Mountain_Area scale <1,1,0.7>}
		
		texture {T_Mountains}     
	}
		
	union {
		Scatter_Rock_Field(11,Mountain_Height)
		scale <0.9,0.9,2>
	}
}   

#declare Impassable_Mountains = union {
	union {
		intersection {
			plane {<0,0,1> 0}
			Hex
		}       
	
		object {Mountain_Area scale <1,1,1.5>}
		
		texture {T_Mountains}     
	}
		
	union {
		Scatter_Rock_Field(60,Mountain_Height)
		scale <0.9,0.9,1.5>
	}
}          


///////////////////////////////////////////////////////////////
// Open terrain
             

#declare Grassland = object {
	intersection {
		plane {<0,0,1> 0}
		Hex
	}   
	
	texture {T_Grass}     
}      

#declare Rocky_Grassland = union {
	intersection {
		plane {<0,0,1> 0}
		Hex
		texture {T_Grass}     
	}   
    
    object {
		Scatter_Rock_Field(6,Zero_Function)    
		scale <0.9,0.9,0.6>
	}
}    


#declare Wooded_Grassland = union{
    Scatter_Trees(20)
	
	object {
		Grassland     
	}     
}      

#declare Ground=object {Grassland}


///////////////////////////////////////////////////////////////
// Trenches and fortifications


// razor wire at north of Hex
#declare Razor_Wire=union {
	#local RINGS=10; 
	#local R1 = seed(1222);
	#local i=0;
	#while (i<RINGS)  
		#local CYLS=20;
		#local ii=0;
		union {
			#while (ii<CYLS)  
				#local ii=ii+1;
				cylinder {<-0.2,0,0>,<0.2,0,0>,0.01 rotate <rand(R1)*1000,rand(R1)*1000,rand(R1)*1000> translate <0,0,0.5> rotate y*(360*ii/CYLS)}
			#end    
			rotate z*(45+90*rand(R1))  
			translate <0,0,0.5>
			scale 0.1
			translate <0,-0.8,0>
			rotate (z*(-15+30*(i/(RINGS-1))))
			translate <0,0.5,0>  
		}	
		#local i=i+1;	
	#end
	
	texture {T_Silver_Metal}
	
	bounded_by {sphere {<0,0,0>,0.8}}
	
}

#declare Earth_Area=object {
    disc {<0,0,0>,<0,0,1>,0.5}
	texture {
		function {max(0,min(1,1.5-4*sqrt(x*x+y*y+z*z)+0.3*(f_bumps(x,y,z)-0.5)))} 
		texture_map {
			[0.0 pigment {rgbt <0,0,0,1>}]
			[0.5 T_Earth]    
			[0.7 T_Dark_Earth]    
		}
	}     
    
	translate <0,0,0.0001>	
}

#declare Trench = object {
	difference {
		intersection {
			plane {<0,0,1> 0}
			Hex
		}   
		cone {<0,0,-0.15>,0.25,<0,0,0.1>,0.4}
	} 
	
	texture {
		function {max(0,min(1,2-4*sqrt(x*x+y*y+z*z)+0.3*(f_bumps(x,y,z)-0.5)))} 
		texture_map {
			[0.0 T_Grass]
			[0.5 T_Earth]    
			[0.7 T_Dark_Earth]    
		}
	}     
}    

#declare Trench_Connection = union {  
	intersection {object {Earth_Area} box {<-1,-0.2,-1>,<1,0.2,1> pigment {Clear}} translate <0,-0.5,0>}
	object {Razor_Wire translate <0,0.15,0> rotate z*90 translate <0,-0.5,0>}
	object {Razor_Wire translate <0,0.15,0>rotate z*-90 translate <0,-0.5,0>}
}



///////////////////////////////////////////////////////////////
// Woods and forests



#declare Woods = union{
    Scatter_Trees(150)
	
	object {
		Grassland    
	}     
}     

////////////////////////////////////////////////////////////////////
// Water and seas   



#declare Sea = object {
	intersection {
		plane {<0,0,1> 0}
		Hex
	}   
	
	texture {T_Sea} 
}           

#declare Sea_Rocks = union {
	object {Sea}
	
	object {
		Scatter_Rock_Field(16,Zero_Function)    
		scale <1,1,2>
	}
}    

#declare Deep_Sea = object {
	intersection {
		plane {<0,0,1> 0}
		Hex
	}   
	
	texture {T_Deep_Sea} 
}   


///////////////////////////////////////////////////////////////////////////
// Rail and roads
         
// basic shape is <0,0,0> to <0,1,0> (straight south)

#local Road_Earth_Width = 0.15;



#declare Distance =function (x,y,z) {
	sqrt(x*x+y*y+z*z)
}  


         
#declare Earth_Function = function (x,y,z) {
	#local BS=40;
	
	max (0,
		select (y,
		 	Road_Earth_Width-Distance(x,y,0),
			select (y-1,
				Road_Earth_Width-abs(x), 
				Road_Earth_Width-Distance(x,y-1,0)
			
		 	)
		) + (f_bumps(x*BS,y*BS,z*BS)-0.5)*0.3
	)
}      

#declare Earth_Road_Area=object {
	disc {<0,0.5,0>,<0,0,1>,1 translate <0,0,0.001>
	
	
		texture {   
			pigment {
			    function {Earth_Function(x,y,z)}
			    pigment_map {
			    	[0 Clear]
			    	[0.05 P_Earth]
			    }
			}    	
			finish {
			}   
			normal {N_Earth}
		}	 
	}
}        

#declare Road_Area = merge  {
	cylinder {<0,0,0>,<0,0,1>,Road_Width}  
	box {<-Road_Width,0,0>,<Road_Width,1,1>}
	cylinder {<0,1,0>,<0,1,1>,Road_Width}  
	scale <1,1,0.01>
	
	texture {T_Road}	
}    

#declare Rail=object {
	box {<-Rail_Size,-(0+Rails_Width),0>,<Rail_Size,(1+Rails_Width),Rail_Height>}
	texture {T_Rail_Metal}
}  

#declare Sleeper=object {
	box {<-Rails_Width*1.4,-Rail_Size,0>,<Rails_Width*1.4,Rail_Size,Rail_Size>}
}

// rails and sleepers, from <0,0,0> to <0,1,0>
#declare Rail_Structure = union  {
	object {Rail translate <-Rails_Width,0,0>}
	object {Rail translate <Rails_Width,0,0>}    
	
	union {    
	    #local sleepers=20;
	    #local i=0;
	    #while (i<sleepers) 
	        object {Sleeper translate <0,i/sleepers,0>}
	    	#local i=i+1;
	    #end
	    texture {T_Sleeper_Wood}
	}
}

#macro Road(dir)
	intersection {
		object {Road_Area translate <0,select(dir-2.5,-1,0),0> rotate (-60+60*mod(dir,3))*z}
		object {Hex scale 1.1 pigment {Clear}}
    }
#end        

#macro Rails(dir) 
	union {
		intersection {
			object {Rail_Structure translate <0,select(dir-2.5,-1,0),0> rotate (-60+60*mod(dir,3))*z}
			object {Hex scale 1.08 pigment {Clear}}
	    } 
	}
#end   

#macro Earth(dir)
	intersection {
		object {Earth_Road_Area translate <0,select(dir-2.5,-1,0),0> rotate (-60+60*mod(dir,3))*z}
    	object {Hex scale 1.08  pigment {Clear}}
    }
#end   


// Coastline       

#local COASTLINE_SLOPE=1.2;

// coastline around a hex (height above sea level)
#declare Hex_Coastline_Height = function {
	#local NS=7.8;
	max(0,min (STEP_HEIGHT,
		COASTLINE_SLOPE*0.5/RATIO+STEP_HEIGHT - COASTLINE_SLOPE*(Hex_Distance(x,y,0)) + 0.1*f_noise3d(x*NS,y*NS,100) -0.1   
	))

}


#declare T_Coastline= texture {   
    function {max(0,z)}
    texture_map {
    	[0 T_Clear]
    	[0.001 T_Sand]
    	[STEP_HEIGHT/2 T_Earth]
    	[STEP_HEIGHT T_Grass]
    	[1 T_Grass]
    }
}	

#declare Coast_Isosurface = object {
	isosurface {    
		function {z-Hex_Coastline_Height(x,y,z)}
		max_gradient 3
		contained_by {sphere {<0,0,0>,1.5}}         
	
		texture {T_Coastline}
	} 
}

#declare Coast_Area=intersection {
	object {Hex scale 1.5  pigment {Clear}}      
	plane {<0,0,-1>,0 pigment {Clear}}
	object {Coast_Isosurface}

}

#macro Coastline(dir) 
	intersection {
   		object {Hex scale 1  pigment {Clear}}
		//object {Coast_Area translate <0,select(dir-2.5,-1,0),0>}
		object {Coast_Area translate -vrotate(<0,1,0>,z*(dir*60-60))}
        translate <0,0,-STEP_HEIGHT+0.001>
    }
#end





