module SyncReadWriteMem(
   input clk,
   input reset,
   input rreq,
   input [31:0] raddr,
    output reg[31:0] rdata,
    output reg data_valid,
    input [31:0] waddr,
    input [3:0] wmask,
    input [31:0] wdata,
    input wreq,
    output reg write_done
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
   reg [2:0] current_state, next_state;
   localparam IDLE = 0;
   localparam READ_DELAY = 1;
   localparam WRITE_DELAY = 2;
   localparam OK = 3;
   localparam W_OK = 4;
   reg [5:0] delay_reg;
   reg [5:0] cnt_reg;
   always @(posedge clk)    begin
       if(reset)    begin
        current_state <= IDLE;
       end else begin
        current_state <= next_state;
       end
   end
   always @(posedge clk)    begin
       if(reset)    delay_reg <= 6'h3f;
       if(current_state == IDLE)    delay_reg <= {$random(seed)}%(32);
   end
   always @(posedge clk)    begin
       if(current_state == READ_DELAY || current_state == WRITE_DELAY)   cnt_reg <= cnt_reg + 1;
       else cnt_reg <= 0;
   end
   always @(*)  begin
       next_state = IDLE;
       case(current_state)
        IDLE:   begin
            if(rreq && delay_reg !=0 ) next_state = READ_DELAY;
            else if(rreq) next_state = OK;
            else next_state = IDLE;
        end
        READ_DELAY:  begin
            if(cnt_reg == delay_reg)    next_state = OK;
            else next_state = READ_DELAY;
        end
        WRITE_DELAY:  begin
            if(cnt_reg == delay_reg)    next_state = W_OK;
            else next_state = WRITE_DELAY;
        end
        OK:     begin
            next_state = IDLE;
        end
        W_OK:     begin
            next_state = IDLE;
        end
        endcase
   end
   wire [7:0] byte3, byte2, byte1, byte0;
   assign byte3 = wmask[3] ? wdata[31:24] : mem[waddr][31:24];
   assign byte2 = wmask[2] ? wdata[23:16] : mem[waddr][23:16];
   assign byte1 = wmask[1] ? wdata[15:8] : mem[waddr][15:8];
   assign byte0 = wmask[0] ? wdata[7:0] : mem[waddr][7:0];
   always @(posedge clk)    begin
       if(reset)    begin
           // Initialize the memory
       end else if(current_state == WRITE_DELAY && next_state == W_OK)  begin
           mem[waddr] <= {byte3, byte2, byte1, byte0};
       end
   end

   always @(*)  begin
       rdata = 0;
       data_valid = 0;
       case(current_state)
        IDLE,READ_DELAY,WRITE_DELAY:   begin
            rdata = 0;
            data_valid = 0;
            data_valid = 0;
            write_done = 0;
        end
        OK: begin
            rdata = mem[raddr];
            data_valid=1;
        end
        W_OK: begin
            write_done=1;
        end
        endcase
   end


endmodule

