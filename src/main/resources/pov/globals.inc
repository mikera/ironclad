
//////////////////////////////////////////////
// Global settings    
  

// step for hex elevation levels
#declare STEP_HEIGHT=0.07;    

// bottom of hex block
#declare HEX_BOTTOM=-0.2;    

// scale for hexes, drives between hex gap if < 1
#declare HEX_SCALE=1;         

// scale for all units
#declare UNIT_SCALE=1.1;  

//#declare HEX_SCALE=0.985;  

#declare WIDTH = 32; // width of rendering area in units      
#declare RATIO = sqrt(3)*0.5; // ratio of hex radius (= half x-diameter) to height ~ 0.866


#declare P_Unit_Colour= pigment { color <0.4,0.5,0.4> }     

#declare VIEW_ANGLE=30;           

#declare YS=1/(cos(30*pi/180));           



#declare P_Clear = pigment {color rgbf <1,1,1,1>}

#declare T_Clear = texture  {pigment {P_Clear}}


// road and rail

#declare Road_Width = 0.095;

#declare Rails_Width = 0.06;

#declare Rail_Size = 0.01;           

#declare Rail_Height = 2*Rail_Size;    

#declare Rail_Car_Width=Rails_Width+Rail_Size+0.01;                  





#declare Spherical_Distance = function {
	sqrt (x*x+y*y+z*z)
}

#declare Hex_Distance = function {
	max (abs (y),abs (x*RATIO+y*0.5),abs (x*RATIO-y*0.5))	
}

                                                          