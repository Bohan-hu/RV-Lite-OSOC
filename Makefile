verilog:
	mill chiselModule.runMain sim.ysyx_hbh --output-file ysyx_hbh.v	
sim:
	mill chiselModule.runMain sim.SimTop --output-file SimTop.v	
	cat SimTop.v > ../Sim_Workspace/verilog/top.v

