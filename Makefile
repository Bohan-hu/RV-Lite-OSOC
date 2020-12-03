verilog:
	mill chiselModule.runMain sim.ysyx_hbh --output-file ysyx_hbh.v	
sim:
	mill chiselModule.runMain sim.SimTop --output-file SimTop.v	
copy: sim 
	cat SimTop.v > ../Sim_Workspace/verilog/top.v
	cat AXIRAM.v > ../Sim_Workspace/src/vsrc/axi_ram.v
