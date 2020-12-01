verilog:
	mill chiselModule.runMain sim.SimTop --output-file SimTop.v
copy: verilog
	cat SimTop.v > ../Sim_Workspace/verilog/top.v
