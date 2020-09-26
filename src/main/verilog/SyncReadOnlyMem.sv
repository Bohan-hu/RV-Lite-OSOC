/* verilator lint_off WIDTH */
module SyncReadOnlyMem(
   input clk,
   input reset,
   input rreq,
   input [31:0] raddr,
    output reg[31:0] rdata,
    output reg data_valid,
    output reg[5:0]delay_reg,
    output reg [1:0] current_state
);
    // reg clk;
    // reg reset;
    // reg rreq;
    // reg [31:0] raddr;
    // initial begin
    //     #0 clk = 0; reset=1; rreq = 0;
    //     #20 reset = 0;
    //     #60 rreq = 1; raddr = 4;
    //     wait(data_valid == 1) ;
    // end
    // reg [31:0] seed = 0;
    // always begin
    //     #10 clk = ~clk;
    // end

   reg [31:0] 		mem [0:65536];
   reg [1:0] next_state;
   localparam IDLE = 0;
   localparam DELAY = 1;
   localparam OK = 2;
   reg [5:0] cnt_reg;
    always @(posedge clk)    begin
       if(reset)    begin
           $readmemh("init_mem",mem);
       end
    end
   always @(posedge clk)    begin
       if(reset)    begin
        current_state <= IDLE;
       end else begin
        current_state <= next_state;
       end
   end
   wire [31:0] del = {$random}%(32);
   always @(posedge clk)    begin
       if(reset)    delay_reg <= 6'h3f;
       if(current_state == IDLE)    delay_reg <= {$random}%(32);
   end
   always @(posedge clk)    begin
       if(current_state == DELAY)   cnt_reg <= cnt_reg + 1;
       else cnt_reg <= 0;
   end
   always @(*)  begin
       next_state = IDLE;
       case(current_state)
        IDLE:   begin
            if(rreq && delay_reg !=0 ) next_state = DELAY;
            else if(rreq) next_state = OK;
            else next_state = IDLE;
        end
        DELAY:  begin
            if(cnt_reg == delay_reg)    next_state = OK;
            else next_state = DELAY;
        end
        OK:     begin
            next_state = IDLE;
        end
        endcase
   end
   reg [31:0] read_data;
   always @(posedge clk)    begin
       if(current_state == IDLE && next_state == DELAY) begin
           read_data <= mem[raddr];
       end
   end
   always @(*)  begin
       rdata = 0;
       data_valid = 0;
       case(current_state)
        IDLE,DELAY:   begin
            rdata = 0;
            data_valid = 0;
        end
        OK: begin
            rdata = read_data;
            data_valid = 1;
        end
        endcase
   end

    initial begin
            $dumpfile("vcd");
            $dumpvars(0, SyncReadOnlyMem);        
    end

endmodule

