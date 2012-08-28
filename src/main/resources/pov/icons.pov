
#include "all-includes.inc"        

#declare ICON_HEIGHT = 0.001;       
#declare ISIZE=0.02;

#declare Icon_Zone = intersection {
	plane {<0,0,1>,ICON_HEIGHT}
	plane {<0,0,-1>,-(ICON_HEIGHT-0.001)}
}  

#declare Hex_Edge = difference {
	object {Hex scale 0.9}
	object {Hex scale 0.9-4*ISIZE}
}             

#declare Hex_Brightened_Area = intersection {
    object {Hex}
    object {Icon_Zone}     
    texture {
    	pigment {color rgbf <1, 1, 1,0.9>}
    }    
    translate <0,0,-0.0001>
}

#declare Wrench_Shape=union {
	box {<-1,-0.2,-0.2>,<1,0.2,0.2> }    
	Revolve (
		difference {
			cylinder {<-1.1,0,-0.2>,<-1,0,0.2>,2*0.2}
			box {<-2,-0.2,-0.2*2>,<-1,0.2,0.2*2>}
		},
		z*180,
		2
	)
}         

#declare Down_Arrow_Shape=union {
	box {<-0.2,-0.6,-0.2>,<0.2,0.2,0.2> } 
	
	prism {
	  linear_spline
	  -0.2,  // low height
	  0.2,   // high height
	  3,     // number of points               
	  <-0.5,0.2>, <0.5,0.2>, <0,0.7>   
	  rotate x*-90
	}
   
	
}


#declare ICON_TRANSPARENCY=function(T) {0.8-0.2*(1+cos(2*pi*T/16))}


  


#macro Cursor(T)
	union {
		intersection {
			object {Hex_Edge}
			object {Icon_Zone}
			texture {
				pigment {color rgbt <1, 1, 0,ICON_TRANSPARENCY(T)>}
			}      
		}	
		
//		object {
//			Hex_Brightened_Area
//			texture {
//				pigment {color rgbt <1, 1, 0,0.9>}
//			}    			
//		}
		
	    	no_shadow
	}	
#end           

#macro Attack(T)
	union {
		intersection {
			union {
				object {Hex_Edge}      
				union {
					box {<-0.3,-ISIZE,-1> <0.3,ISIZE,1>}     
					box {<-ISIZE,-0.3,-1> <ISIZE,0.3,1>} 
					difference {
					        cylinder {<0,0,-1> <0,0,1> 0.2}     
					        cylinder {<0,0,-1> <0,0,1> 0.2-2*ISIZE}
					} 
					// rotate z*45
				}   
			}
			object {Icon_Zone}    
			texture {
				pigment {color rgbt <1, 0, 0, ICON_TRANSPARENCY(T)>}
		    	}       
		}	   
		
		object {Hex_Brightened_Area}
   
	    	no_shadow
	}		
#end            

#macro Move(T)
	union {
		intersection {
			union {
	    			cylinder {<0,0,-1> <0,0,1> ISIZE} 
	    			object {Hex_Edge}      
	    		}
	    		object {Icon_Zone}    
			texture {
		    		pigment {color rgbt <0, 1, 1, ICON_TRANSPARENCY(T)>}
		    	} 
		}	
		
		object {Hex_Brightened_Area}
	        
		no_shadow
	}		
#end           

#macro Build(T)
	union {
		intersection {
			union {
	    			object {Wrench_Shape rotate z*-45 scale 0.2}
	    			object {Hex_Edge}      
	    		}
	    		object {Icon_Zone}    
				texture {
		    		pigment {color rgbt <1, 0, 1, ICON_TRANSPARENCY(T)>}
		    	} 
		}	
		
		object {Hex_Brightened_Area}
	        
		no_shadow
	}		
#end               

#macro Deploy (T)
	union {
		intersection {
			union {
	    			object {Down_Arrow_Shape scale 0.4}
	    			object {Hex_Edge}      
	    		}
	    		object {Icon_Zone}    
				texture {
		    		pigment {color rgbt <0, 1, 0, ICON_TRANSPARENCY(T)>}
		    	} 
		}	
		
		object {Hex_Brightened_Area}
	        
		no_shadow
	}		
#end  


#macro Smoke(T)
	#local SIZE= 0.05+(0.05*T);
	sphere { < 0, 0, 0>, 1
		pigment { rgbt 1 } // transparent sphere
		interior {
			media {
				absorption 1      
				emission 1
				intervals 1
				samples 25
				method 3
				
				// spherical fade-out
		        density {
		           spherical   
		           density_map {
		              [0.0 rgb <0.0, 0.0, 0.0>]
		              [1.0 rgb <1, 1, 1> * 0.1* (16-T) * (16-T)]
		           }
		        }            
		        
		
				// main colour
				density {     
					agate
//			        warp { 
//			        	turbulence 0.5  
//			        	lambda 1.5  
			        	omega 0.5
//
//			        }
			        translate -(20+z*(T))   
					density_map {
		                [0.0 rgb 2]
		                [0.1 rgb 1]
		                [0.5 rgb 0.5]
		                [0.7 rgb 0.1]
		                [0.9 rgb 1]
					}  
					
//					warp {
//		                black_hole <0.0, 0.0, 0.0>, 2.0
//		                strength .95
//		                falloff 2.5
//		            }         
		        }
		        
		        // fine detail    
	            density {
		            spherical
		            density_map {
		                [0.0 rgb 0]
		                [0.1 rgb 1]
		                [0.2 rgb 2]
		                [0.8 rgb 3]
		            }           
		
		            warp {
		                turbulence 1.5
		                lambda 2.5
		                omega 0.55
		                octaves 3
		            }
		            scale .75
		
//		            warp {
//		                black_hole <0.0, 0.0, 0.0>, 2.0
//		                strength .8
//		                falloff 2.0

//					}  
				}	
			}  
		}
		hollow   
		scale SIZE        
		rotate z*5*T
	}
#end  

    

#macro Explosion(T)
	#local SIZE= 0.5+(0.02*T);
	sphere { < 0, 0, 0>, 1
		pigment { rgbt 1 } // transparent sphere
		interior {
			media {
				absorption 2      
				emission 2
				intervals 1
				samples 25
				method 3
				
				// spherical fade-out
		        density {
		           spherical   
		           density_map {
		              [0.0 rgb <0.0, 0.0, 0.0>]
		              [1.0 rgb <1.0, 0.5, 0.0>]
		           }
		        }            
		        
		
				// main colour
				density {     
					agate
//			        translate 10+z*(T)  
//			        warp { 
//			        	turbulence 0.5  
//			        	lambda 1.5  
			        	omega 0.5
//
//			        }
			        translate -z*(T)   
					density_map {
		                [0.0 rgb <0.5, 0.0, 0.0>*5]
		                [0.1 rgb <0.8, 0.0, 0.0>*2]
		                [0.5 rgb <0.9, 0.2, 0.0>*3]
		                [0.7 rgb <1.0, 0.4, 0.0>*4]
		                [0.9 rgb <1.0, 0.75, 0.5>*5]
					}  
					
//					warp {
//		                black_hole <0.0, 0.0, 0.0>, 2.0
//		                strength .95
//		                falloff 2.5
//		            }         
		        }
		        
		        // fine detail    
	            density {
		            spherical
		            density_map {
		                [0.0 rgb <0.0, 0.0, 0.0>]
		                [0.1 rgb <1.0, 0.0, 0.0> * .75]
		                [0.2 rgb <1.0, 0.5, 0.0> * .75]
		                [0.8 rgb <1.0, 0.75, 0.5> * 2.5]
		            }           
		
		            warp {
		                turbulence 1.5
		                lambda 2.5
		                omega 0.55
		                octaves 3
		            }
		            scale .75
		
//		            warp {
//		                black_hole <0.0, 0.0, 0.0>, 2.0
//		                strength .8
//		                falloff 2.0

//					}  
				}	
			}  
		}
		hollow   
		scale SIZE        
		rotate z*5*T
	}
#end     

#macro Blast(T)
	#local SIZE= 0.3+(0.03*T);
	sphere { < 0, 0, 0>, 1
		pigment { rgbt 1 } // transparent sphere
		interior {
			media {
				absorption 2      
				emission 2
				intervals 1
				samples 15
				method 3
				
				// spherical fade-out
		        density {
		           spherical   
		           density_map {
		              [0.0 rgb <0.0, 0.0, 0.0>]
		              [1.0 rgb <1.0, 1.0, 1.0>]
		           }
		        }            
		        
		        // bright centre
				density {
					spherical
					density_map {
						[0.5 rgb <1.0, 1.0, 1.0>]
						[1.0 rgb <1.0, 1.0, 1.0>*10]
					}
				}   
				
				// main colour
				density {     
					agate
			        translate z*(T)  
			        warp { 
			        	turbulence 0.5  
			        	lambda 1.5  
			        	omega 0.5

			        }
			        translate -z*(T)   
					density_map {
		                [0.0 rgb <0.0, 0.0, 0.0>]
		                [0.1 rgb <0.5, 0.0, 0.0>]
		                [0.3 rgb <0.8, 0.4, 0.0>]
		                [0.5 rgb <1.0, 1.0, 0.0>]
		                [0.9 rgb <1.0, 1.0, 1.0>]
					}  
					
					warp {
		                black_hole <0.0, 0.0, 0.0>, 2.0
		                strength .95
		                falloff 2.5
		            }         
		        }
		        
		        // fine detail    
	            density {
		            spherical
		            density_map {
		                [0.0 rgb <0.0, 0.0, 0.0>]
		                [0.1 rgb <1.0, 0.0, 0.0> * .75]
		                [0.2 rgb <1.0, 0.5, 0.0> * .75]
		                [0.8 rgb <1.0, 1.0, 1.0> * 2.5]
		            }           
		
		            warp {
		                turbulence 1.5
		                lambda 2.5
		                omega 0.55
		                octaves 3
		            }
		            scale .75
		
		            warp {
		                black_hole <0.0, 0.0, 0.0>, 2.0
		                strength .8
		                falloff 2.0

					}  
				}	
			}  
		}
		hollow   
		scale SIZE        
		translate <0,0,T*0.02>
	}
#end

#macro Icons() 
	union {
		#declare T=0;
		#while (T<16)
			 object {Cursor(T) translate <2*T,0*YS,0>}
			 object {Attack(T) translate <2*T,2*YS,0>}        
			 object {Move(T) translate <2*T,4*YS,0>}    
			 object {Explosion(T) translate <2*T,6*YS,0>}
			 object {Build(T) translate <2*T,8*YS,0>}    
			 object {Deploy(T) translate <2*T,10*YS,0>}    
			 object {Blast(T) translate <2*T,12*YS,0>}
			 object {Smoke(T) translate <2*T,14*YS,0>}
		     #declare T=T+1;
	 	#end     
	}
#end


object {Icons()}   

// mini-scene
union {
    union {           
    	union {
			object {Ground translate <0,0,0>}    
			union {
				object {Ground translate <0,1,0>} 
				object {Explosion(3) translate <0,0,0> }  
				object {Buildings translate <0,1,0>}            
				object {Blast(3) translate <0,2,0> }  
				object {Smoke(3) translate <0,3,0> }  
				translate STEP_HEIGHT*z
			}
			object {Ground translate <0,2,0>}      
		}
		object {Sea translate <0,3,0>}  
		translate <RATIO*0,0,0>
	}      
	
	union {
    	union {
			object {Ground translate <0,0,0>}  
			object {Attack(0) translate <0,0,0> }  
			object {Ground translate <0,1,0>}   
			object {Steam_Tank rotate 30*z translate <0,1,0> }      
			object {Cursor(0) translate <0,1,0> }
			translate STEP_HEIGHT*z      
		}
		object {Sea translate <0,2,0>}  
		translate <RATIO*1,0.5,0>
	}     
	
	intersection {         
	    plane {<0,0,1>,-1}
	    box {<-1,-2,-2> <6,6,0>}
	 
		texture {pigment {color <0,0,0>}}
	}
	
	
	translate <0, 18*YS ,0>	
}