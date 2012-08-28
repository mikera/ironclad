#include "all-includes.inc"

camera {
	perspective        
	
	// camera position and direction  
	location <-1,4,1>    
	up <0,0,0.75>
	right <1,0,0>     
	direction <0,-1,0>

	
    rotate x*15
	rotate z*30
		
}         

//////////////////////////////////////////////////////////////////////////////
// Texture for title

// ordinary finish:

#macro _shinyness ( _g1 )
finish {
	ambient 0
	diffuse min (1, (1.65 - _g1))
	specular _g1
	roughness 1 / pow (500, _g1)
	reflection { 0.5 * max ( 0, _g1 - 0.65 ), 2 * max ( 0, _g1 - 0.65 ) }
}
#end

// metal finish:

#macro _metal ( _g1 )
finish {
	ambient 0
	diffuse 1 - _g1
	specular _g1 * 2
	roughness 1 / pow (250, _g1)
	metallic
	reflection { _g1 / 2, _g1 metallic }
}
#end

#local Brass_color = color rgb <0.9, 0.712, 0.45>;




#local Bump0 = normal {
	granite 
	scale 5
	bump_size -0.2
    slope_map {
		[ 0 <0, 1> ]
		[ 0.3 <1, 0> ]
		[ 1 <1, 0> ]
    }
}
#local Bump1 = normal { bumps scale 0.25 bump_size 0.2 }
#local Bump2 = normal { bumps scale 5 bump_size 0.2 }

#declare CastMetal_normal = normal {
 average normal_map {
  [ 1 Bump0 ]
  [ 1 Bump1 ]
  [ 1 Bump2 ]
 }
}

#declare CastMetal_texture = texture {
 pigment { Brass_color }
 normal { CastMetal_normal }
 _metal (0.4) // the macro from original post
}    



// sea
  

plane{<0,0,1>, 0 
	texture{
		pigment{ rgb <0.2, 0.2, 0.2> } 
        normal {bumps 0.1 scale <1,0.25,0.25>*1 turbulence 0.6 }
        finish { 
        	ambient 0.05 diffuse 0.55 
			brilliance 6.0 phong 0.8 phong_size 120
			reflection 0.2 
        }  
        scale 0.05
	}
}    


// title text     

union {
	text {
		ttf "cowboys.ttf" "Ironclad" 0.5, 0
		rotate x*90 
		translate <-2,0,-0.5>         
		scale 1.1
		texture {
			CastMetal_texture scale 0.15
		}
//		finish { reflection .1 specular 1 }
		
	}    
	
	text {
		ttf "timrom.ttf" "  Generals of Steam" 0.1, 0
		translate <-0.5,0,-0.9> 
		rotate x*90 
		scale 0.4
		translate <0,0,-0.9>         
		
		texture {
			pigment { rgb <1.3,1.3,0.3> }
		}
//		finish { reflection .1 specular 1 }
		
	} 
	
	rotate z*30      
	translate <0,-5,0.9>

	
	no_shadow   
	no_reflection

}

union {
    object {Paddle_Cruiser rotate z*45 translate <-1,-1.0,0>}

    object {Patrol_Boat scale 0.75 rotate z*45 translate <1,-1.0,0>}

    object {Patrol_Boat scale 0.75 rotate z*45 translate <1,-2.5,0>}

    object {Assault_Zeppelin rotate z*45 translate <2,-10.0,1.3> no_shadow }

    union {
	    object {Assault_Zeppelin rotate z*45 translate <0,0,0> no_shadow }
	    object {Death_Ray_Balloon rotate z*45 translate <3,-10,0.8> no_shadow }   
	    translate <0,-15,2>
	}
} 

object {Mountains scale <6,6,4> translate <20,-20,0>}               

object {Hills scale <30,30,2> translate <20,-30,-0.001>}               


fog{
	fog_type   2   
	up <0,0,1>
	distance 10  
	color rgb<1,0.99,0.9>
    
    fog_offset 0.0 
    fog_alt  0.3 
    turbulence 0.2
}
