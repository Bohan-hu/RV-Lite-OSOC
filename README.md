RV-Lite 
=======================

RV Lite is a RISC-V core implements RV64G instruction set capable of booting Linux, xv6 and RT-Thread.

RV Lite aims at implementing a fully functioning pipeline processor core supporting OS within minimized die area.

Specification:

- AXI4 full & lite interface for memory access or IO access

- 3 stage pipeline with stall-on-hazard policy to prevent RAW hazards

- Hardware PTW and 8-Entry TLB to support Sv39 paging