//	Persistence of Vision Raytracer Version 3.5 Scene Description File
//*******************************************

global_settings {
  max_trace_level 5
}

#include "colors.inc"

camera {
	location <-1.5, 30, -150>
	look_at <0, 25, 35>
	angle 35
}

background { color rgb 0 }

// light_source { <100, 100, -200> color White }


sphere { < 0, 0, 0>, 2
	pigment { rgbt 1 } // surface of sphere is transparent
	interior {
		media {
			emission 0.02
			intervals 1
			samples 25
			method 3
			density {
				spherical
				ramp_wave
        translate 1.0*y  // replace 1.0 = t   by time for animation
        warp { turbulence 1.5 }
        translate -1.0*y // replace -1.0 = -t  by time for animation
				color_map {
					[0.0 color rgb <0, 0, 0>]
					[0.1 color rgb <1, 0, 0>]
					[0.5 color rgb <1, 1, 0>]
					[1.0 color rgb <1, 1, 0>]
				}
			}
		}
	}
	scale 25
	translate 25*y
	hollow
}
