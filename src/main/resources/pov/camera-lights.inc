//////////////////////////////////////////////
// Camera and lights
//
// Remember to use "+UA" option for alpha output   


camera {
	orthographic        
	
	// camera position and direction  
	location <WIDTH/2-1,WIDTH/2-1,1000>    
	direction <0,0, -1>             
	
	// length of up and right determines view size
	right <WIDTH,0,0>
	up <0,-WIDTH,0>
    
    rotate x*-VIEW_ANGLE
}      

light_source {
	<-1000,500,2000>          
	color rgb <1,1,1>
	parallel
	point_at <0,0,0>
}     
     

sky_sphere { pigment { gradient <0,0,1>
                       color_map { [0.00 color Clear]  
                                   [0.499 color Clear]
                                   [0.5 rgb  <0.0,0.1,0.8>]
                                   [0.51 rgb <0.0,0.1,0.8>]
                                   [0.75 rgb <0.6,0.7,1.0>] 
                                 } 
                       scale 2 
                       translate -1        
                     } // end of pigment
           }
