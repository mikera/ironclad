
////////////////////////////////////////////////////////////////////////
// Mike's handy macros

#macro Revolve (Thing, Angle, Times)
    union {
		#local i=0;    
		#while (i < Times) 
		    object {Thing rotate Angle*i}
			#local i=i+1;
		#end
    }
#end     

#macro Series (Thing, Offset, Times)
    union {
		#local i=0;    
		#while (i < Times) 
		    object {Thing translate Offset*i}
			#local i=i+1;
		#end
    }
#end   

#macro Shift_Part (Source, Mask, Offset)
    union {
		difference {
			object {Source}
			object {Mask}
		}    
		intersection {
			object {Source}
			object {Mask}    
			translate Offset
		}
    }
#end